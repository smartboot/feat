package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.audit.ActivityRecorder;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.PageResult;
import tech.smartboot.feat.demo.plus.dto.ActivityItemResponse;
import tech.smartboot.feat.demo.plus.dw.dto.DwFieldMeta;
import tech.smartboot.feat.demo.plus.dw.dto.PageDataQuery;
import tech.smartboot.feat.demo.plus.dw.service.PageDataSource;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.Permission;

/**
 * 审计日志数据源：第二个配置驱动页面（/p/activities）的数据来源，包装 {@link ActivityService}。
 * <p>
 * <b>只读是可配置化的前提，不是妥协。</b> 审计日志没有写接口——日志只能由 {@code ActivityRecorder}
 * 在业务操作时落库。所以这一页迁到配置层之后，整个「表单 / 保存 / 删除」的复杂度都不存在了：
 * 没有 {@code save}，{@code writable()} 返回 false，页面配置里写不进去 create / edit / delete
 * 动作（{@code PageService} 在保存配置时就拒掉）。这是配置驱动最干净的一类页面。
 * <p>
 * <b>权限与数据范围没有被配置层削弱。</b> {@code list} 直接把 {@code AccessContext} 交给
 * {@code ActivityService}，「能看哪些部门的人留下的日志」仍由那一处的 {@code queryDepartmentIds} /
 * {@code selfOnlyUserId} 决定。配置改的是列与筛选的呈现，改不了查询范围——
 * 这正是「数据源注册表」路线与「配置里存 WHERE 片段」路线的分野。
 * <p>
 * 筛选键 {@code from} / {@code to} 沿用 {@link ActivityService} 的自然日语义（含当天），
 * 日期格式校验也仍在 Service 里，坏值照样 400，不会因为走动态通道就静默忽略。
 */
@Bean
public class ActivityPageDataSource implements PageDataSource {

    @Autowired
    private ActivityService activityService;

    @Override
    public String key() {
        return "activity";
    }

    @Override
    public String label() {
        return "审计日志";
    }

    @Override
    public List<DwFieldMeta> fields() {
        List<DwFieldMeta> fields = new ArrayList<>();
        fields.add(new DwFieldMeta("id", "ID", "number", false));
        fields.add(new DwFieldMeta("createdTime", "时间", "string", false));
        // kind 给原始英文值（create / update / delete / password），中文文案与颜色
        // 由页面配置的 badge 选项映射——数据源不替展示做决定，同一个值换个页面可以换种说法。
        fields.add(new DwFieldMeta("kind", "类型", "string", false));
        fields.add(new DwFieldMeta("title", "动作", "string", false));
        fields.add(new DwFieldMeta("description", "详情", "string", false));
        fields.add(new DwFieldMeta("operatorName", "操作人", "string", false));
        fields.add(new DwFieldMeta("operatorId", "操作人 ID", "number", false));
        // 虚拟筛选字段：operatorId 会作为筛选条件出现，from / to 是日期范围筛选的两端。
        // 不声明它们，页面配置保存时会被「字段漂移」校验拒掉——见 RolePageDataSource 的 keyword。
        fields.add(new DwFieldMeta("from", "开始日期", "string", false));
        fields.add(new DwFieldMeta("to", "结束日期", "string", false));
        return fields;
    }

    @Override
    public String readPermission() {
        return Permission.AUDIT_READ;
    }

    /** 只读：写权限点与读权限点同值，{@code canWrite} 由 {@link #writable()} 算出 false。 */
    @Override
    public String writePermission() {
        return Permission.AUDIT_READ;
    }

    @Override
    public PageResult<Map<String, Object>> list(PageDataQuery query, AccessContext access) {
        int page = Math.max(query.getPage(), 1);
        int pageSize = query.getPageSize() <= 0 ? 20 : query.getPageSize();
        // 筛选值原样交给 Service：空串 / "all" 由它解释成「不限」，格式错误由它抛 400。
        PageResult<ActivityItemResponse> source = activityService.list(
                emptyToNull(value(query.getFilters(), "kind")),
                parseId(value(query.getFilters(), "operatorId")),
                value(query.getFilters(), "from"),
                value(query.getFilters(), "to"),
                page, pageSize, access);

        List<Map<String, Object>> items = new ArrayList<>(source.getItems().size());
        for (ActivityItemResponse row : source.getItems()) {
            items.add(toRow(row));
        }
        return new PageResult<>(items, source.getTotal(), page, pageSize);
    }

    private static Map<String, Object> toRow(ActivityItemResponse row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId());
        item.put("createdTime", row.getCreatedTime());
        item.put("kind", row.getKind());
        item.put("title", row.getTitle());
        item.put("description", row.getDescription());
        // 操作人可能已被软删除，此时 Service 的 join 取不到名字。动态层统一按字符串处理，
        // 给空串而不是 null：null 在 Map 序列化时会被省略，前端拿到 undefined 与空串是两种分支。
        item.put("operatorName", row.getOperatorName() == null ? "" : row.getOperatorName());
        item.put("operatorId", row.getCreator() == null ? 0L : row.getCreator());
        return item;
    }

    private static String value(Map<String, String> filters, String key) {
        String raw = filters.get(key);
        return raw == null ? "" : raw.trim();
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() || "all".equalsIgnoreCase(value) ? null : value;
    }

    /** 空串 / "all" 都表示不限；给了就必须是数字，否则说明筛选控件传了脏值。 */
    private static Long parseId(String raw) {
        if (raw.isEmpty() || "all".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("操作人参数不合法：" + raw);
        }
    }
}
