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
 * <b>简化表单的功能边界。</b> 动态表单只覆盖 code / name / description / status 四个字段；
 * 权限点勾选、数据范围配置仍是手写页 /roles 的专属能力（控件集后续补齐后再迁移）。
 * {@code RoleService.update} 对非 ADMIN 角色会用入参整体覆盖权限与数据范围——
 * 所以这里的更新路径必须先把当前值回填进 RoleInputRequest，否则简化表单一保存，
 * 角色的权限点就被静默清空了。
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
        fields.add(new DwFieldMeta("permissions", "权限点", "list", false));
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
        input.setName(value(request.getValues(), "name"));
        input.setDescription(value(request.getValues(), "description"));
        input.setStatus(parseStatus(value(request.getValues(), "status")));

        if (request.getId() == null) {
            // 新建：简化表单不配权限点与数据范围，Service 的默认逻辑是「无权限 + 全部数据」，
            // 权限由管理员后续在手写角色页补齐——formJson 的说明文案里写了这条边界。
            input.setCode(value(request.getValues(), "code"));
            RoleItemResponse created = roleService.create(input, access.getUserId());
            return toRow(created);
        }

        // 更新：回填当前权限与数据范围。RoleService.update 会整体覆盖这两个字段，
        // 不回填的话，简化表单每次保存都会把角色权限清空。
        RoleItemResponse current = roleService.get(request.getId());
        input.setPermissions(current.getPermissions());
        input.setDepartmentScope(current.getDepartmentScope());
        input.setDepartmentIds(current.getDepartmentIds());
        RoleItemResponse updated = roleService.update(request.getId(), input, access.getUserId());
        return toRow(updated);
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
}
