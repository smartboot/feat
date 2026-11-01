package tech.smartboot.feat.demo.plus.role.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.Permission;
import tech.smartboot.feat.demo.plus.dashboard.service.ActivityRecorder;
import tech.smartboot.feat.demo.plus.role.dto.RoleInputRequest;
import tech.smartboot.feat.demo.plus.role.dto.RoleItemResponse;
import tech.smartboot.feat.demo.plus.role.mapper.RoleMapper;
import tech.smartboot.feat.demo.plus.role.model.RoleRecord;

/**
 * 角色管理业务规则：
 * <ul>
 *   <li>编码全局唯一且创建后不可改——用户表用编码关联角色，改编码等于换身份；</li>
 *   <li>内置角色不可删除；ADMIN 额外不可停用、权限不可修改，避免把所有管理员锁在门外；</li>
 *   <li>仍被账号引用的角色不可删除，必须先改派；</li>
 *   <li>权限点必须是 Permission 里登记过的 key，未知 key 直接拒绝。</li>
 * </ul>
 */
@Bean
public class RoleService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,31}$");

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private ActivityRecorder activityRecorder;

    public List<RoleItemResponse> list() {
        List<RoleRecord> rows = roleMapper.selectAll();
        List<RoleItemResponse> items = new ArrayList<>(rows.size());
        for (RoleRecord row : rows) {
            items.add(RoleItemResponse.from(row, roleMapper.countUsers(row.getCode())));
        }
        return items;
    }

    public RoleItemResponse get(long id) {
        RoleRecord role = require(id);
        return RoleItemResponse.from(role, roleMapper.countUsers(role.getCode()));
    }

    public RoleItemResponse create(RoleInputRequest input, long operatorId) {
        String code = validateCode(input == null ? null : input.getCode());
        String name = requireName(input == null ? null : input.getName());
        assertUnique(code, name, 0L);

        Date now = new Date();
        RoleRecord role = new RoleRecord();
        role.setCode(code);
        role.setName(name);
        role.setDescription(trimToEmpty(input.getDescription()));
        role.setPermissions(Permission.join(input.getPermissions()));
        role.setBuiltin(0);
        role.setStatus(input.getStatus() == null ? 1 : input.getStatus());
        role.setIsDeleted(0);
        role.setCreatedTime(now);
        role.setEditTime(now);
        role.setCreator(operatorId);
        role.setEditor(operatorId);
        roleMapper.insert(role);

        RoleRecord saved = requireByCode(code);
        activityRecorder.record("create", "创建了角色 " + saved.getName(),
                saved.getCode() + " · " + describeCount(saved.getPermissions()), null, operatorId);
        return RoleItemResponse.from(saved, 0);
    }

    public RoleItemResponse update(long id, RoleInputRequest input, long operatorId) {
        RoleRecord current = require(id);
        String name = requireName(input == null ? null : input.getName());
        assertUnique(current.getCode(), name, id);

        boolean admin = "ADMIN".equals(current.getCode());
        String permissions = current.getPermissions();
        if (admin) {
            // 超管权限固定：允许改名称和描述，但不接受权限点变更，否则可能一次误操作让所有人失去管理权限。
            String requested = Permission.join(input.getPermissions());
            if (!requested.equals(current.getPermissions())) {
                throw new DomainException("内置管理员角色的权限不可修改");
            }
        } else {
            permissions = Permission.join(input.getPermissions());
        }

        current.setName(name);
        current.setDescription(trimToEmpty(input.getDescription()));
        current.setPermissions(permissions);
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

        activityRecorder.record("update", "更新了角色 " + current.getName(),
                current.getCode() + " · " + describeCount(current.getPermissions()), null, operatorId);
        return RoleItemResponse.from(require(id), roleMapper.countUsers(current.getCode()));
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
        return RoleItemResponse.from(require(id), roleMapper.countUsers(role.getCode()));
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
