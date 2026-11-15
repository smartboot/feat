package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.PageResult;
import tech.smartboot.feat.demo.plus.dto.DwFieldMeta;
import tech.smartboot.feat.demo.plus.dto.DwSaveRequest;
import tech.smartboot.feat.demo.plus.dto.PageDataQuery;
import tech.smartboot.feat.demo.plus.dto.RoleInputRequest;
import tech.smartboot.feat.demo.plus.dto.RoleItemResponse;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.Permission;

/**
 * 角色数据源：第一个配置驱动页面（/p/roles）的数据来源，包装 {@link RoleService}。
 * <p>
 * 角色量级小（一屏以内），list 走内存过滤 + 分页，不动 RoleMapper。
 * 角色是全局数据，没有部门维度，{@code access} 只用来取操作人。
 * <p>
 * <b>权限点走多选控件。</b> {@code permissions} 是可写字段，选项来自 {@code /api/permissions}
 * （按模块分组），不把权限点字典复制进页面配置——{@code Permission.CATALOG} 才是它的真相。
 * 值类型是数组，用 {@code DwSaveRequest.list()} 取值。
 * <p>
 * <b>仍然留在手写页的两件事：</b>数据范围（需要「全部 / 本部门 / 本部门及以下 / 自定义」四态
 * 加部门树联动，不是一个字段能表达的），以及内置角色的逐行差异化禁用（超管权限恒为全集、
 * 内置角色不允许停用）——这些规则由 {@link RoleService} 兜底，配置层只负责不把用户送进死胡同。
 */
@Bean
public class RolePageDataSource implements PageDataSource {

    @Autowired
    private RoleService roleService;

    @Override
    public String key() {
        return "role";
    }

    @Override
    public String label() {
        return "角色";
    }

    @Override
    public List<DwFieldMeta> fields() {
        List<DwFieldMeta> fields = new ArrayList<>();
        fields.add(new DwFieldMeta("id", "ID", "number", false));
        fields.add(new DwFieldMeta("code", "角色编码", "string", true));
        fields.add(new DwFieldMeta("name", "角色名称", "string", true));
        fields.add(new DwFieldMeta("description", "描述", "string", true));
        // 值统一按字符串处理（"1" / "0"），switch 与 badge 都按字符串比较。
        fields.add(new DwFieldMeta("status", "状态", "string", true));
        // 权限点可写：选项由 /api/permissions 现取，值以数组提交。
        fields.add(new DwFieldMeta("permissions", "权限点", "list", true));
        fields.add(new DwFieldMeta("scopeLabel", "数据范围", "string", false));
        fields.add(new DwFieldMeta("userCount", "成员数", "number", false));
        fields.add(new DwFieldMeta("createdTime", "创建时间", "string", false));
        // 虚拟筛选字段：不对应行数据的列，list() 里解释成「名称 / 编码 / 描述」三列模糊匹配。
        // 不声明它的话，种子配置里的 keyword 筛选会在页面配置保存时被「字段漂移」校验拒掉。
        fields.add(new DwFieldMeta("keyword", "搜索（名称 / 编码 / 描述）", "string", false));
        return fields;
    }

    @Override
    public String readPermission() {
        return Permission.ROLE_READ;
    }

    @Override
    public String writePermission() {
        return Permission.ROLE_WRITE;
    }

    @Override
    public boolean writable() {
        return true;
    }

    @Override
    public PageResult<Map<String, Object>> list(PageDataQuery query, AccessContext access) {
        List<RoleItemResponse> rows = roleService.list();

        String keyword = value(query.getFilters(), "keyword");
        String status = value(query.getFilters(), "status");
        List<RoleItemResponse> matched = new ArrayList<>();
        for (RoleItemResponse row : rows) {
            if (!keyword.isEmpty()
                    && !contains(row.getName(), keyword) && !contains(row.getCode(), keyword)
                    && !contains(row.getDescription(), keyword)) {
                continue;
            }
            if (!status.isEmpty() && !status.equals(String.valueOf(row.getStatus()))) {
                continue;
            }
            matched.add(row);
        }

        int page = Math.max(query.getPage(), 1);
        int pageSize = query.getPageSize() <= 0 ? 20 : query.getPageSize();
        int from = Math.min((page - 1) * pageSize, matched.size());
        int to = Math.min(from + pageSize, matched.size());

        List<Map<String, Object>> items = new ArrayList<>(to - from);
        for (RoleItemResponse row : matched.subList(from, to)) {
            items.add(toRow(row));
        }
        return new PageResult<>(items, matched.size(), page, pageSize);
    }

    @Override
    public Map<String, Object> get(long id, AccessContext access) {
        return toRow(roleService.get(id));
    }

    @Override
    public Map<String, Object> save(DwSaveRequest request, AccessContext access) {
        RoleInputRequest input = new RoleInputRequest();
        input.setName(request.text("name"));
        input.setDescription(request.text("description"));
        input.setStatus(parseStatus(request.text("status")));

        if (request.getId() == null) {
            // 新建：没配权限点字段就是空权限，由管理员后续补——配置里没这一项不该让保存失败。
            input.setCode(request.text("code"));
            if (request.has("permissions")) {
                input.setPermissions(knownPermissions(request.list("permissions")));
            }
            return toRow(roleService.create(input, access.getUserId()));
        }

        RoleItemResponse current = roleService.get(request.getId());
        // 数据范围不在动态表单里：不传就等于「保持不变」，绝不能因为表单没这一项被清空。
        input.setDepartmentScope(current.getDepartmentScope());
        input.setDepartmentIds(current.getDepartmentIds());
        if ("ADMIN".equals(current.getCode())) {
            // 超管的权限恒为全集，RoleService 会拒绝任何变更。这里先按原值回填，
            // 让「改个名称」不会被一条「权限不可修改」挡下来——勾选框对超管本就不该有效果。
            input.setPermissions(current.getPermissions());
        } else if (request.has("permissions")) {
            input.setPermissions(knownPermissions(request.list("permissions")));
        } else {
            input.setPermissions(current.getPermissions());
        }
        return toRow(roleService.update(request.getId(), input, access.getUserId()));
    }

    @Override
    public void delete(long id, AccessContext access) {
        roleService.delete(id, access.getUserId());
    }

    private static Map<String, Object> toRow(RoleItemResponse role) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", role.getId());
        row.put("code", role.getCode());
        row.put("name", role.getName());
        row.put("description", role.getDescription());
        row.put("permissions", role.getPermissions());
        row.put("scopeLabel", role.getScopeLabel());
        row.put("userCount", role.getUserCount());
        row.put("status", String.valueOf(role.getStatus()));
        row.put("createdTime", role.getCreatedTime());
        return row;
    }

    private static String value(Map<String, String> filters, String key) {
        String raw = filters.get(key);
        return raw == null ? "" : raw.trim();
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }

    /** "1" / "0" → 1 / 0；空值当启用（新建的默认态），解析失败按脏数据拒绝。 */
    private static Integer parseStatus(String raw) {
        if (raw.isEmpty()) {
            return 1;
        }
        if ("1".equals(raw) || "0".equals(raw)) {
            return Integer.parseInt(raw);
        }
        throw new DomainException("状态值只能是 1（启用）或 0（停用）");
    }

    /**
     * 只接受 {@code Permission.CATALOG} 里登记过的权限点。
     * <p>
     * 不做静默过滤：勾选框来自那个接口，传回一个不存在的 key 意味着配置或接口已经不一致，
     * 这时候悄悄丢掉比报错更难查——管理员会以为「保存成功但没生效」。
     */
    private static List<String> knownPermissions(List<String> raw) {
        for (String key : raw) {
            if (!Permission.ALL.contains(key)) {
                throw new DomainException("未知的权限点：" + key);
            }
        }
        return raw;
    }
}
