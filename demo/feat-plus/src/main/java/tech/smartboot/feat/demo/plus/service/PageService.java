package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dto.DatasourceOptionResponse;
import tech.smartboot.feat.demo.plus.dto.DwColumn;
import tech.smartboot.feat.demo.plus.dto.DwDataApi;
import tech.smartboot.feat.demo.plus.dto.DwFieldMeta;
import tech.smartboot.feat.demo.plus.dto.DwFilter;
import tech.smartboot.feat.demo.plus.dto.DwFormConfig;
import tech.smartboot.feat.demo.plus.dto.DwFormField;
import tech.smartboot.feat.demo.plus.dto.DwListConfig;
import tech.smartboot.feat.demo.plus.dto.DwOption;
import tech.smartboot.feat.demo.plus.dto.PageAdminItemResponse;
import tech.smartboot.feat.demo.plus.dto.PageConfigResponse;
import tech.smartboot.feat.demo.plus.dto.PageInputRequest;
import tech.smartboot.feat.demo.plus.mapper.PageMapper;
import tech.smartboot.feat.demo.plus.model.PageRecord;
import tech.smartboot.feat.demo.plus.tx.TxTemplate;

/**
 * 动态页面配置的读写与校验规则，模式同 {@link MenuService}。
 * <p>
 * 「配置入库」比「菜单入库」风险面更大：菜单纯错了是导航跳转失败，页面配置错的是整页渲染。
 * 所以校验闸门全部设在保存时，<b>存进去的一定能渲染</b>：
 * <ol>
 *   <li>{@code pageKey} 唯一且创建后不可改——菜单 path（/p/{pageKey}）引用它，改 key 等于换身份；</li>
 *   <li>{@code datasourceKey} 必须命中 {@link PageDataSourceRegistry} 的编译期白名单；</li>
 *   <li>list_json / form_json 必须是合法 JSON 且逐项过校验：列 / 筛选 / 表单字段的 key
 *       必须命中数据源的字段声明，表单字段还必须是可写字段——数据源改字段名时，
 *       配置漂移在保存配置的瞬间报 400，而不是渲染时静默空列；</li>
 *   <li>create / edit / delete 动作只能配在可写数据源上，避免「按钮能点、保存被拒」的错配；</li>
 *   <li>任何配置改动记审计——它决定别人页面的长相。</li>
 * </ol>
 * <p>
 * <b>存储契约即下发契约。</b> list_json / form_json 原文入库，下发前反序列化成结构再发，
 * 两边走的是同一份校验，不会出现「库里存了一套、前端拿到另一套」。
 * <p>
 * 缓存失效必须在事务提交之后，同 MenuService。
 */
@Bean
public class PageService {

    /** 与 MenuService.KEY_PATTERN 同源语义：小写字母、数字、下划线、短横线。 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_-]{1,32}$");

    private static final Set<String> COLUMN_FORMATS = new HashSet<>(Arrays.asList(
            "text", "muted", "badge", "tags", "number"));
    private static final Set<String> FILTER_WIDGETS = new HashSet<>(Arrays.asList(
            "input", "select", "dateRange"));
    private static final Set<String> FIELD_WIDGETS = new HashSet<>(Arrays.asList(
            "input", "password", "textarea", "select", "switch", "span"));
    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            "create", "edit", "delete", "refresh"));
    private static final Set<String> VARIANTS = new HashSet<>(Arrays.asList(
            "success", "warning", "destructive", "secondary", "outline"));
    /** 表单字段的写入策略：两态布尔不够用了，见 {@link tech.smartboot.feat.demo.plus.dto.DwFormField}。 */
    private static final Set<String> WRITE_MODES = new HashSet<>(Arrays.asList(
            "always", "createOnly", "readonly"));
    /** 自定义数据接口的路径格式：站内 /api/ 相对路径，防外链与拼接注入面。 */
    private static final Pattern API_PATH_PATTERN = Pattern.compile("^/api/[A-Za-z0-9/_.-]{2,120}$");

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private PageSnapshot pageSnapshot;

    // 字段名必须等于 bean 名（@Autowired 按名注入，AOT 硬约束）。
    @Autowired
    private PageDataSourceRegistry pageDataSourceRegistry;

    @Autowired
    private ActivityRecorder activityRecorder;

    @Autowired
    private TxTemplate txTemplate;

    /** 管理页用的全量列表，走缓存。 */
    public List<PageAdminItemResponse> list() {
        List<PageRecord> rows = pageSnapshot.all();
        List<PageAdminItemResponse> items = new ArrayList<>(rows.size());
        for (PageRecord row : rows) {
            items.add(PageAdminItemResponse.from(row));
        }
        return items;
    }

    /**
     * 渲染面的配置下发：原文解析成结构 + 按当前请求人计算 canWrite。
     * canWrite 严禁进缓存——配置是共享的，写权限是个人的。
     */
    public PageConfigResponse config(String pageKey, boolean canWrite) {
        PageRecord record = requireByPageKey(pageKey);
        PageConfigResponse response = new PageConfigResponse();
        response.setPageKey(record.getPageKey());
        response.setTitle(record.getTitle());
        response.setDescription(record.getDescription());
        response.setDatasourceKey(record.getDatasourceKey());
        response.setList(parseListConfig(record.getListJson()));
        response.setForm(parseFormConfig(record.getFormJson()));
        response.setCanWrite(canWrite);
        return response;
    }

    /** 配置行存在性 + 数据源 key，供 DwController 定位数据源。 */
    public String requireDatasourceKey(String pageKey) {
        return requireByPageKey(pageKey).getDatasourceKey();
    }

    /**
     * 表单配置，供保存通道按写入策略清洗入参。
     * <p>
     * 不走 {@link #config}：那条路径要算 canWrite、还要带列表配置，写路径只需要字段级的写入策略。
     */
    public DwFormConfig requireForm(String pageKey) {
        return parseFormConfig(requireByPageKey(pageKey).getFormJson());
    }

    /** 菜单校验用：path 形如 /p/{pageKey} 且配置行真实存在。 */
    public boolean isValidDynamicPath(String path) {
        if (path == null || !path.startsWith("/p/") || path.length() <= 3) {
            return false;
        }
        String pageKey = path.substring(3);
        if (!KEY_PATTERN.matcher(pageKey).matches()) {
            return false;
        }
        for (PageRecord row : pageSnapshot.all()) {
            if (row.getPageKey().equals(pageKey)) {
                return true;
            }
        }
        return false;
    }

    /** 菜单管理页「路径」下拉的动态页选项：/p/{pageKey} 列表。 */
    public List<String> dynamicRouteOptions() {
        List<PageRecord> rows = pageSnapshot.all();
        List<String> paths = new ArrayList<>(rows.size());
        for (PageRecord row : rows) {
            paths.add("/p/" + row.getPageKey());
        }
        return paths;
    }

    /** 配置管理页的数据源下拉 + 字段提示（writable 供编辑器禁用只读源的写动作）。 */
    public List<DatasourceOptionResponse> datasourceOptions() {
        List<DatasourceOptionResponse> options = new ArrayList<>();
        for (PageDataSource dataSource : pageDataSourceRegistry.all()) {
            List<DwFieldMeta> fields = dataSource.fields();
            options.add(new DatasourceOptionResponse(dataSource.key(), dataSource.label(),
                    dataSource.writable(), fields));
        }
        return options;
    }

    public PageAdminItemResponse create(PageInputRequest input, long operatorId) {
        String pageKey = requireKey(input == null ? null : input.getPageKey());
        String title = requireTitle(input == null ? null : input.getTitle());
        String description = requireDescription(input == null ? null : input.getDescription());
        PageDataSource dataSource = requireDatasource(input == null ? null : input.getDatasourceKey());
        DwListConfig list = validateListConfig(input == null ? null : input.getListJson(), dataSource, pageKey);
        DwFormConfig form = validateFormConfig(input == null ? null : input.getFormJson(), dataSource, pageKey);

        assertPageKeyUnique(pageKey, 0L);

        PageRecord saved = txTemplate.call(session -> {
            PageMapper pages = session.getMapper(PageMapper.class);
            Date now = new Date();
            PageRecord record = new PageRecord();
            record.setPageKey(pageKey);
            record.setTitle(title);
            record.setDescription(description);
            record.setDatasourceKey(dataSource.key());
            record.setListJson(JSON.toJSONString(list));
            record.setFormJson(JSON.toJSONString(form));
            record.setIsDeleted(0);
            record.setCreatedTime(now);
            record.setEditTime(now);
            record.setCreator(operatorId);
            record.setEditor(operatorId);
            pages.insert(record);
            activityRecorder.record("create", "创建了页面配置 " + title,
                    "/p/" + pageKey + " · " + dataSource.key(), null, operatorId);
            return pages.selectByPageKey(pageKey);
        });
        pageSnapshot.invalidate();
        return PageAdminItemResponse.from(saved);
    }

    public PageAdminItemResponse update(long id, PageInputRequest input, long operatorId) {
        PageRecord current = requireById(id);
        String title = requireTitle(input == null ? null : input.getTitle());
        String description = requireDescription(input == null ? null : input.getDescription());
        PageDataSource dataSource = requireDatasource(input == null ? null : input.getDatasourceKey());
        DwListConfig list = validateListConfig(input == null ? null : input.getListJson(), dataSource,
                current.getPageKey());
        DwFormConfig form = validateFormConfig(input == null ? null : input.getFormJson(), dataSource,
                current.getPageKey());

        String oldTitle = current.getTitle();

        current.setTitle(title);
        current.setDescription(description);
        current.setDatasourceKey(dataSource.key());
        current.setListJson(JSON.toJSONString(list));
        current.setFormJson(JSON.toJSONString(form));
        current.setEditTime(new Date());
        current.setEditor(operatorId);

        txTemplate.run(session -> {
            session.getMapper(PageMapper.class).update(current);
            activityRecorder.record("update", "更新了页面配置 " + title,
                    oldTitle.equals(title) ? "/p/" + current.getPageKey() + " · " + dataSource.key()
                            : oldTitle + " → " + title, null, operatorId);
        });
        pageSnapshot.invalidate();

        return PageAdminItemResponse.from(requireById(id));
    }

    /**
     * 软删除页面配置。删掉后 /p/{pageKey} 与指向它的菜单会变成「配置不存在」，
     * 菜单行本身不动——入口管理是菜单管理页的事，这里只管页面。
     */
    public void delete(long id, long operatorId) {
        PageRecord record = requireById(id);
        txTemplate.run(session -> {
            session.getMapper(PageMapper.class).softDelete(id, operatorId);
            activityRecorder.record("delete", "删除了页面配置 " + record.getTitle(),
                    "/p/" + record.getPageKey(), null, operatorId);
        });
        pageSnapshot.invalidate();
    }

    private PageRecord requireById(long id) {
        // 写路径直读库：缓存里可能读到刚被别的请求删掉的行。
        PageRecord record = pageMapper.selectById(id);
        if (record == null) {
            throw DomainException.notFound("页面配置不存在或已被删除");
        }
        return record;
    }

    private PageRecord requireByPageKey(String pageKey) {
        String key = pageKey == null ? "" : pageKey.trim();
        for (PageRecord row : pageSnapshot.all()) {
            if (row.getPageKey().equals(key)) {
                return row;
            }
        }
        throw DomainException.notFound("页面配置不存在：" + key);
    }

    private void assertPageKeyUnique(String pageKey, long excludeId) {
        if (pageMapper.countPageKey(pageKey, excludeId) > 0) {
            throw new DomainException("该页面标识已存在：" + pageKey);
        }
    }

    // ---------- 校验 ----------

    private static String requireKey(String raw) {
        String value = trimToEmpty(raw);
        if (!KEY_PATTERN.matcher(value).matches()) {
            throw new DomainException("页面标识只能包含小写字母、数字、下划线和短横线，且不能为空");
        }
        return value;
    }

    private static String requireTitle(String raw) {
        String value = trimToEmpty(raw);
        if (value.isEmpty()) {
            throw new DomainException("请填写页面标题");
        }
        if (value.length() > 64) {
            throw new DomainException("页面标题不能超过 64 个字符");
        }
        return value;
    }

    private static String requireDescription(String raw) {
        String value = trimToEmpty(raw);
        if (value.length() > 256) {
            throw new DomainException("页面说明不能超过 256 个字符");
        }
        return value;
    }

    private PageDataSource requireDatasource(String raw) {
        String value = trimToEmpty(raw);
        if (value.isEmpty()) {
            throw new DomainException("请选择数据源");
        }
        // require 的报错是 404 语义，这里配错了 key 是客户端的参数错误，转成 400 语义的提示。
        PageDataSource dataSource = null;
        for (PageDataSource candidate : pageDataSourceRegistry.all()) {
            if (candidate.key().equals(value)) {
                dataSource = candidate;
                break;
            }
        }
        if (dataSource == null) {
            throw new DomainException("数据源不存在：" + value + "，请从可选数据源中选择");
        }
        return dataSource;
    }

    /** 解析并校验 list_json；返回规整后的结构（create/update 时以它重新序列化入库）。 */
    private DwListConfig validateListConfig(String raw, PageDataSource dataSource, String pageKey) {
        DwListConfig list = parseListConfig(raw);
        Set<String> fieldKeys = fieldKeys(dataSource, false);

        if (list.getPageSize() < 1 || list.getPageSize() > 200) {
            throw new DomainException("每页条数必须在 1 到 200 之间（页面 " + pageKey + "）");
        }
        if (fieldKeys.isEmpty() || !fieldKeys.contains(list.getRowKey())) {
            throw new DomainException("行主键字段必须是数据源声明过的字段：" + list.getRowKey());
        }

        Set<String> actionSet = new HashSet<>(list.getActions());
        if (!ACTIONS.containsAll(actionSet)) {
            throw new DomainException("不支持的按钮动作，可选值：create / edit / delete / refresh");
        }
        if (!dataSource.writable() && (actionSet.contains("create") || actionSet.contains("edit")
                || actionSet.contains("delete"))) {
            throw new DomainException("数据源「" + dataSource.label() + "」是只读的，不能配置 create / edit / delete 动作");
        }

        Set<String> seen = new HashSet<>();
        for (DwColumn column : list.getColumns()) {
            String where = "列 " + column.getKey();
            requireFieldKey(column.getKey(), seen, fieldKeys, where);
            if (!COLUMN_FORMATS.contains(column.getFormat())) {
                throw new DomainException(where + "的格式不支持：" + column.getFormat()
                        + "（可选 text / muted / badge / tags / number）");
            }
            validateOptions(column.getOptions(), where);
        }
        if (list.getColumns().isEmpty()) {
            throw new DomainException("列表至少要有一列");
        }

        validateDataApi(list.getDataApi(), dataSource);

        seen = new HashSet<>();
        for (DwFilter filter : list.getFilters()) {
            String where = "筛选 " + filter.getKey();
            requireFieldKey(filter.getKey(), seen, fieldKeys, where);
            if (!FILTER_WIDGETS.contains(filter.getWidget())) {
                throw new DomainException(where + "的控件不支持：" + filter.getWidget()
                        + "（可选 input / select / dateRange）");
            }
            // dateRange 占两个键：起止都要命中数据源字段，否则「结束日期」这一侧会静默写进
            // 数据源读不到的名字里——筛选框能填、结果不变，比报错更难查。
            if ("dateRange".equals(filter.getWidget())) {
                if (filter.getEndKey().isEmpty()) {
                    throw new DomainException(where + "是日期范围筛选，必须配置结束字段 endKey");
                }
                requireFieldKey(filter.getEndKey(), seen, fieldKeys, where + " 的结束字段");
            }
            // 下拉的选项有两个来源：静态 options 或远程 optionsApi，二选一即可。
            if ("select".equals(filter.getWidget())
                    && filter.getOptions().isEmpty() && filter.getOptionsApi().isEmpty()) {
                throw new DomainException(where + "是下拉筛选，必须配置选项或选项接口");
            }
            if (!filter.getOptionsApi().isEmpty() && !API_PATH_PATTERN.matcher(filter.getOptionsApi()).matches()) {
                throw new DomainException(where + "的选项接口必须是站内 /api/ 路径：" + filter.getOptionsApi());
            }
            validateOptions(filter.getOptions(), where);
        }
        return list;
    }

    /** 解析并校验 form_json。表单字段必须命中数据源的<b>可写</b>字段。 */
    private DwFormConfig validateFormConfig(String raw, PageDataSource dataSource, String pageKey) {
        DwFormConfig form = parseFormConfig(raw);
        Set<String> writableKeys = fieldKeys(dataSource, true);

        Set<String> seen = new HashSet<>();
        for (DwFormField field : form.getFields()) {
            String where = "表单字段 " + field.getKey();
            requireFieldKey(field.getKey(), seen, writableKeys, where);
            if (!FIELD_WIDGETS.contains(field.getWidget())) {
                throw new DomainException(where + "的控件不支持：" + field.getWidget()
                        + "（可选 input / password / textarea / select / switch / span）");
            }
            // 写入策略归一：getter 已把「空值 + immutable」推成 createOnly，这里只校验取值，
            // 再把 immutable 反推回派生值。存进去的永远是带 writeMode 的配置，不会两处各说一套。
            String writeMode = field.getWriteMode();
            if (!WRITE_MODES.contains(writeMode)) {
                throw new DomainException(where + "的写入策略不支持：" + writeMode
                        + "（可选 always / createOnly / readonly）");
            }
            field.setWriteMode(writeMode);
            field.setImmutable(!"always".equals(writeMode));
            if ("select".equals(field.getWidget()) && field.getOptions().isEmpty()) {
                throw new DomainException(where + "是下拉字段，必须配置选项");
            }
            if (field.isRequired() && "span".equals(field.getWidget())) {
                throw new DomainException(where + "是只读展示字段，不能同时是必填项");
            }
            if (field.getSpan() != 1 && field.getSpan() != 2) {
                throw new DomainException(where + "的宽度取值只能是 1（半宽）或 2（全宽）");
            }
            if (!field.getPattern().isEmpty()) {
                try {
                    Pattern.compile(field.getPattern());
                } catch (RuntimeException e) {
                    throw new DomainException(where + "的校验正则无法编译：" + field.getPattern());
                }
            }
            validateOptions(field.getOptions(), where);
        }
        return form;
    }

    private static void requireFieldKey(String key, Set<String> seen, Set<String> fieldKeys, String where) {
        if (key == null || key.trim().isEmpty()) {
            throw new DomainException(where + "缺少字段标识");
        }
        if (seen.contains(key)) {
            throw new DomainException(where + "重复出现");
        }
        seen.add(key);
        if (!fieldKeys.contains(key)) {
            throw new DomainException(where + "不是数据源声明过的字段（或不可写），请核对字段标识");
        }
    }

    private static void validateOptions(List<DwOption> options, String where) {
        Set<String> seen = new HashSet<>();
        for (DwOption option : options) {
            if (option.getValue() == null || option.getValue().trim().isEmpty()) {
                throw new DomainException(where + "的选项值不能为空");
            }
            if (seen.contains(option.getValue())) {
                throw new DomainException(where + "的选项值重复：" + option.getValue());
            }
            seen.add(option.getValue());
            if (!VARIANTS.contains(option.getVariant())) {
                throw new DomainException(where + "的选项样式不支持：" + option.getVariant());
            }
        }
    }

    private static Set<String> fieldKeys(PageDataSource dataSource, boolean writableOnly) {
        Set<String> keys = new HashSet<>();
        for (DwFieldMeta field : dataSource.fields()) {
            if (!writableOnly || field.isWritable()) {
                keys.add(field.getKey());
            }
        }
        return keys;
    }

    /**
     * 数据通道覆盖的校验。行结构契约不变（列 / 筛选仍锚定数据源字段），
     * 这里只管路径合法性与「只读数据源不得配删除通道」——后者与 actions 的约束同源：
     * 按钮能不能点是 canWrite 决定的，只读源配了自定义删除接口只会做出「按钮永远灰着」的死配置。
     */
    private static void validateDataApi(DwDataApi dataApi, PageDataSource dataSource) {
        if (dataApi == null) {
            return;
        }
        requireApiPath(dataApi.getQuery(), "查询接口");
        requireApiPath(dataApi.getDelete(), "删除接口");
        if (!dataSource.writable() && !trimToEmpty(dataApi.getDelete()).isEmpty()) {
            throw new DomainException("数据源「" + dataSource.label() + "」是只读的，"
                    + "不能配置删除接口（通道覆盖不改变可写性约束）");
        }
    }

    /** 站内 /api/ 相对路径；留空表示走内置通道。 */
    private static void requireApiPath(String raw, String what) {
        String path = trimToEmpty(raw);
        if (path.isEmpty()) {
            return;
        }
        if (!API_PATH_PATTERN.matcher(path).matches() || path.endsWith("/")) {
            throw new DomainException(what + "必须是 /api/ 开头的站内路径（不以 / 结尾，不含参数与特殊字符）："
                    + path);
        }
    }

    // ---------- JSON 解析 ----------

    private static DwListConfig parseListConfig(String raw) {
        DwListConfig list = parseJson(raw, DwListConfig.class, "列表配置");
        return list == null ? new DwListConfig() : list;
    }

    private static DwFormConfig parseFormConfig(String raw) {
        DwFormConfig form = parseJson(raw, DwFormConfig.class, "表单配置");
        return form == null ? new DwFormConfig() : form;
    }

    private static <T> T parseJson(String raw, Class<T> type, String what) {
        String text = trimToEmpty(raw);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(text, type);
        } catch (JSONException e) {
            throw new DomainException(what + "不是合法的 JSON：" + e.getMessage());
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
