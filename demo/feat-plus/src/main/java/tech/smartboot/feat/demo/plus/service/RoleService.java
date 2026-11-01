package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dto.RoleInputRequest;
import tech.smartboot.feat.demo.plus.dto.RoleItemResponse;
import tech.smartboot.feat.demo.plus.mapper.RoleMapper;
import tech.smartboot.feat.demo.plus.model.RoleRecord;
import tech.smartboot.feat.demo.plus.security.DataScope;
import tech.smartboot.feat.demo.plus.security.Permission;

/**
 * 角色管理业务规则：
 * <ul>
 *   <li>编码全局唯一且创建后不可改——角色关联用的是编码，改编码等于换身份；</li>
 *   <li>内置角色不可删除；ADMIN 额外不可停用、权限与数据范围都不可修改，
 *       避免把所有管理员锁在门外；</li>
 *   <li>仍被账号引用的角色不可删除，必须先改派；</li>
 *   <li>权限点必须是 Permission 里登记过的 key，未知 key 直接拒绝；</li>
 *   <li>数据范围取「自定义」时，配置的部门必须真实存在。</li>
 * </ul>
 */
@Bean
public class RoleService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,31}$");

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ActivityRecorder activityRecorder;

    public List<RoleItemResponse> list() {
        List<RoleRecord> rows = roleMapper.selectAll();
        List<RoleItemResponse> items = new ArrayList<>(rows.size());
        for (RoleRecord row : rows) {
            items.add(toItem(row));
        }
        return items;
    }

    public RoleItemResponse get(long id) {
        return toItem(require(id));
    }

    public RoleItemResponse create(RoleInputRequest input, long operatorId) {
        String code = validateCode(input == null ? null : input.getCode());
        String name = requireName(input == null ? null : input.getName());
        assertUnique(code, name, 0L);

        int scope = resolveScope(input == null ? null : input.getDepartmentScope(), null);
        List<Long> departmentIds = scope == DataScope.CUSTOM
                ? normalizeDepartmentIds(input.getDepartmentIds()) : new ArrayList<Long>();
        departmentService.assertAllExist(departmentIds);

        Date now = new Date();
        RoleRecord role = new RoleRecord();
        role.setCode(code);
        role.setName(name);
        role.setDescription(trimToEmpty(input.getDescription()));
        role.setPermissions(Permission.join(input.getPermissions()));
        role.setDepartmentScope(scope);
        role.setBuiltin(0);
        role.setStatus(input.getStatus() == null ? 1 : input.getStatus());
        role.setIsDeleted(0);
        role.setCreatedTime(now);
        role.setEditTime(now);
        role.setCreator(operatorId);
        role.setEditor(operatorId);
        roleMapper.insert(role);
        saveScopeDepartments(code, scope, departmentIds, operatorId);

        RoleRecord saved = requireByCode(code);
        activityRecorder.record("create", "创建了角色 " + saved.getName(),
                saved.getCode() + " · " + describeCount(saved.getPermissions())
                        + " · " + DataScope.label(scope), null, operatorId);
        return RoleItemResponse.from(saved, 0, roleMapper.selectDepartmentIds(code));
    }

    public RoleItemResponse update(long id, RoleInputRequest input, long operatorId) {
        RoleRecord current = require(id);
        String name = requireName(input == null ? null : input.getName());
        assertUnique(current.getCode(), name, id);

        boolean admin = "ADMIN".equals(current.getCode());
        String permissions = current.getPermissions();
        int scope = current.getDepartmentScope();
        List<Long> departmentIds = roleMapper.selectDepartmentIds(current.getCode());

        if (admin) {
            // 超管固定为「全部数据 + 全部权限」：允许改名称和描述，但不接受权限点或范围变更，
            // 否则一次误操作就可能让所有管理员看不到任何数据。
            String requested = Permission.join(input.getPermissions());
            if (!requested.equals(current.getPermissions())) {
                throw new DomainException("内置管理员角色的权限不可修改");
            }
            if (input.getDepartmentScope() != null
                    && DataScope.normalize(input.getDepartmentScope()) != current.getDepartmentScope()) {
                throw new DomainException("内置管理员角色的数据范围不可修改");
            }
        } else {
            permissions = Permission.join(input.getPermissions());
            scope = resolveScope(input.getDepartmentScope(), current.getDepartmentScope());
            if (scope == DataScope.CUSTOM) {
                departmentIds = normalizeDepartmentIds(input.getDepartmentIds());
                departmentService.assertAllExist(departmentIds);
            } else {
                departmentIds = new ArrayList<>();
            }
        }

        current.setName(name);
        current.setDescription(trimToEmpty(input.getDescription()));
        current.setPermissions(permissions);
        current.setDepartmentScope(scope);
        if (input.getStatus() != null) {
            if (current.getBuiltin() == 1 && input.getStatus() != current.getStatus()) {
                throw new DomainException("内置角色不允许停用");
            }
            if (current.getBuiltin() == 0) {
                current.setStatus(input.getStatus());
            }
        }
        current.setEditTime(new Date());
        current.setEditor(operatorId);
        roleMapper.update(current);
        if (!admin) {
            saveScopeDepartments(current.getCode(), scope, departmentIds, operatorId);
        }

        activityRecorder.record("update", "更新了角色 " + current.getName(),
                current.getCode() + " · " + describeCount(current.getPermissions())
                        + " · " + DataScope.label(scope), null, operatorId);
        return toItem(require(id));
    }

    public RoleItemResponse setStatus(long id, int status, long operatorId) {
        RoleRecord role = require(id);
        if (role.getBuiltin() == 1) {
            throw new DomainException("内置角色不允许停用");
        }
        roleMapper.updateStatus(id, status, operatorId);
        activityRecorder.record(status == 1 ? "enable" : "disable",
                (status == 1 ? "启用了" : "停用了") + "角色 " + role.getName(),
                "该角色下的账号权限随之" + (status == 1 ? "恢复" : "失效"), null, operatorId);
        return toItem(require(id));
    }

    public void delete(long id, long operatorId) {
        RoleRecord role = require(id);
        if (role.getBuiltin() == 1) {
            throw new DomainException("内置角色不允许删除");
        }
        long users = roleMapper.countUsers(role.getCode());
        if (users > 0) {
            throw new DomainException("该角色下还有 " + users + " 个账号，请先改派角色");
        }
        roleMapper.softDelete(id, operatorId);
        roleMapper.softDeleteRoleDepts(role.getCode(), operatorId);
        activityRecorder.record("delete", "删除了角色 " + role.getName(),
                role.getCode() + " 已从角色列表移除", null, operatorId);
    }

    /**
     * 供用户模块校验：角色必须存在且处于启用状态，否则不允许分配给账号。
     */
    public RoleRecord requireAssignable(String roleCode) {
        RoleRecord role = roleMapper.selectByCode(roleCode);
        if (role == null) {
            throw new DomainException("角色不存在：" + roleCode);
        }
        if (role.getStatus() != 1) {
            throw new DomainException("角色已停用，无法分配：" + role.getName());
        }
        return role;
    }

    /**
     * 批量校验并返回角色，供多角色分配使用。顺序按入参，前端展示与角色页顺序一致。
     */
    public List<RoleRecord> requireAssignables(List<String> roleCodes) {
        List<RoleRecord> roles = new ArrayList<>(roleCodes.size());
        for (String code : roleCodes) {
            roles.add(requireAssignable(code));
        }
        return roles;
    }

    private RoleItemResponse toItem(RoleRecord role) {
        return RoleItemResponse.from(role, roleMapper.countUsers(role.getCode()),
                roleMapper.selectDepartmentIds(role.getCode()));
    }

    /**
     * 数据范围解析：
     * <ul>
     *   <li>不传时沿用当前值，新建时默认「全部」——权限点才是主约束；</li>
     *   <li>传了非法值一律收敛到「仅本人」，安全边界宁可收紧。</li>
     * </ul>
     */
    private static int resolveScope(Integer requested, Integer current) {
        if (requested == null) {
            return current == null ? DataScope.ALL : DataScope.normalize(current);
        }
        return DataScope.normalize(requested);
    }

    private static List<Long> normalizeDepartmentIds(List<Long> ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        for (Long id : ids) {
            if (id != null && !result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * 整组重写「角色 × 部门」配置：先全部失效，再逐条恢复或插入。
     * <p>
     * 不能直接 INSERT：sys_role_dept 的主键是 (role_code, department_id)，
     * 上一次编辑留下的软删除行还在表里，重复插入会撞主键。
     */
    private void saveScopeDepartments(String roleCode, int scope, List<Long> departmentIds, long operatorId) {
        roleMapper.softDeleteRoleDepts(roleCode, operatorId);
        if (scope != DataScope.CUSTOM || departmentIds == null) {
            return;
        }
        for (Long departmentId : departmentIds) {
            if (roleMapper.restoreRoleDept(roleCode, departmentId, operatorId) == 0) {
                roleMapper.insertRoleDept(roleCode, departmentId, operatorId);
            }
        }
    }

    private RoleRecord require(long id) {
        RoleRecord role = roleMapper.selectById(id);
        if (role == null) {
            throw DomainException.notFound("角色不存在或已被删除");
        }
        return role;
    }

    private RoleRecord requireByCode(String code) {
        RoleRecord role = roleMapper.selectByCode(code);
        if (role == null) {
            throw new DomainException("角色保存失败");
        }
        return role;
    }

    private void assertUnique(String code, String name, long excludeId) {
        if (roleMapper.countByCode(code, excludeId) > 0) {
            throw new DomainException("角色编码已存在");
        }
        if (roleMapper.countByName(name, excludeId) > 0) {
            throw new DomainException("角色名称已存在");
        }
    }

    private static String validateCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase();
        if (code.isEmpty()) {
            throw new DomainException("请填写角色编码");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new DomainException("角色编码需以大写字母开头，只能包含大写字母、数字和下划线");
        }
        return code;
    }

    private static String requireName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new DomainException("请填写角色名称");
        }
        if (name.length() > 64) {
            throw new DomainException("角色名称不能超过 64 个字符");
        }
        return name;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String describeCount(String permissions) {
        return "共 " + Permission.split(permissions).size() + " 项权限";
    }
}
