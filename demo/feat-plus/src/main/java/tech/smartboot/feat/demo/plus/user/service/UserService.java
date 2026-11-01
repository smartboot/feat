package tech.smartboot.feat.demo.plus.user.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.auth.service.PasswordHasher;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.page.PageResult;
import tech.smartboot.feat.demo.plus.dashboard.service.ActivityRecorder;
import tech.smartboot.feat.demo.plus.department.model.DepartmentRecord;
import tech.smartboot.feat.demo.plus.department.service.DepartmentService;
import tech.smartboot.feat.demo.plus.role.model.RoleRecord;
import tech.smartboot.feat.demo.plus.role.service.RoleService;
import tech.smartboot.feat.demo.plus.user.dto.UserInputRequest;
import tech.smartboot.feat.demo.plus.user.dto.UserItemResponse;
import tech.smartboot.feat.demo.plus.user.mapper.UserMapper;
import tech.smartboot.feat.demo.plus.user.model.UserAccount;

/**
 * 用户管理业务规则：
 * 字段必填、用户名/邮箱唯一、不能停用或删除当前登录账号、必须保留至少一个已启用管理员。
 * <p>
 * 角色与部门都必须是「已存在且启用」的记录：角色决定权限点，部门决定组织归属，
 * 两者都通过各自的 Service 校验，避免用户表里出现指向不存在记录的引用。
 * <p>
 * 审计字段：operatorId 是当前登录人，创建时写入 creator + editor，之后每次变更只更新 editTime + editor。
 * 删除是软删除（is_deleted = 1），因此唯一性校验只在未删除的范围内比较。
 */
@Bean
public class UserService {

    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ActivityRecorder activityRecorder;

    public PageResult<UserItemResponse> list(String keyword, Integer status, String roleCode,
                                             Long departmentId, int page, int pageSize) {
        String like = toLike(keyword);
        String role = emptyToNull(roleCode);
        // 按部门筛选时默认包含全部下级：勾「技术中心」应该能看到平台研发部里的人。
        List<Long> departmentIds = departmentId == null || departmentId <= 0
                ? null : departmentService.subtreeIds(departmentId);
        int safePage = page < 1 ? 1 : page;
        int safeSize = pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);

        long total = userMapper.countPage(like, status, role, departmentIds);
        List<UserAccount> rows = userMapper.selectPage(like, status, role, departmentIds,
                safeSize, (safePage - 1) * safeSize);
        List<UserItemResponse> items = new ArrayList<>(rows.size());
        for (UserAccount row : rows) {
            items.add(UserItemResponse.from(row));
        }
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public List<UserItemResponse> listAll() {
        List<UserAccount> rows = userMapper.selectAll();
        List<UserItemResponse> items = new ArrayList<>(rows.size());
        for (UserAccount row : rows) {
            items.add(UserItemResponse.from(row));
        }
        return items;
    }

    public UserItemResponse get(long id) {
        return UserItemResponse.from(require(id));
    }

    public UserItemResponse create(UserInputRequest input, long operatorId) {
        validate(input);
        assertUnique(input.getUsername(), input.getEmail(), 0L);
        RoleRecord role = roleService.requireAssignable(input.getRoleCode().trim());
        DepartmentRecord department = departmentService.requireAssignable(input.getDepartmentId());

        Date now = new Date();
        UserAccount user = new UserAccount();
        user.setUsername(input.getUsername().trim());
        user.setPasswordHash(PasswordHasher.hash("Feat@" + System.currentTimeMillis()));
        user.setDisplayName(input.getDisplayName().trim());
        user.setRoleCode(role.getCode());
        user.setDepartmentId(department.getId());
        user.setEmail(input.getEmail().trim());
        user.setStatus(input.getStatus() == null ? 1 : input.getStatus());
        user.setIsDeleted(0);
        user.setCreatedTime(now);
        user.setEditTime(now);
        user.setCreator(operatorId);
        user.setEditor(operatorId);
        userMapper.insert(user);

        UserAccount saved = userMapper.selectByUsername(user.getUsername());
        activityRecorder.record("create", "创建了用户 " + saved.getDisplayName(),
                saved.getUsername() + " · " + role.getName(), saved.getId(), operatorId);
        return UserItemResponse.from(saved);
    }

    public UserItemResponse update(long id, UserInputRequest input, long operatorId) {
        UserAccount current = require(id);
        validate(input);
        assertUnique(input.getUsername(), input.getEmail(), id);
        RoleRecord role = roleService.requireAssignable(input.getRoleCode().trim());
        DepartmentRecord department = departmentService.requireAssignable(input.getDepartmentId());

        boolean losingAdmin = "ADMIN".equals(current.getRoleCode()) && !"ADMIN".equals(role.getCode());
        if (losingAdmin) {
            assertNotLastEnabledAdmin(current);
        }
        boolean disabling = current.getStatus() == 1 && input.getStatus() != null && input.getStatus() == 0;
        if (disabling) {
            assertCanDisable(current, operatorId);
        }

        current.setUsername(input.getUsername().trim());
        current.setDisplayName(input.getDisplayName().trim());
        current.setRoleCode(role.getCode());
        current.setDepartmentId(department.getId());
        current.setEmail(input.getEmail().trim());
        if (input.getStatus() != null) {
            current.setStatus(input.getStatus());
        }
        current.setEditTime(new Date());
        current.setEditor(operatorId);
        userMapper.updateProfile(current);

        activityRecorder.record("update", "更新了用户 " + current.getDisplayName(),
                current.getUsername() + " 的账号资料已变更", current.getId(), operatorId);
        return UserItemResponse.from(require(id));
    }

    public UserItemResponse setStatus(long id, int status, long operatorId) {
        UserAccount user = require(id);
        if (status == 0) {
            assertCanDisable(user, operatorId);
        }
        userMapper.updateStatus(id, status, operatorId);
        activityRecorder.record(status == 1 ? "enable" : "disable",
                (status == 1 ? "启用了" : "停用了") + "用户 " + user.getDisplayName(),
                user.getUsername() + " 的账号状态已更新", id, operatorId);
        return UserItemResponse.from(require(id));
    }

    public String resetPassword(long id, long operatorId) {
        UserAccount user = require(id);
        String temporaryPassword = "Feat@" + String.format("%03d", id) + "8X";
        userMapper.updatePassword(id, PasswordHasher.hash(temporaryPassword), operatorId);
        activityRecorder.record("password", "重置了用户 " + user.getDisplayName() + " 的密码",
                "临时密码已生成并等待安全交付", id, operatorId);
        return temporaryPassword;
    }

    public void delete(long id, long operatorId) {
        UserAccount user = require(id);
        if (id == operatorId) {
            throw new DomainException("不能删除当前登录账号");
        }
        if ("ADMIN".equals(user.getRoleCode()) && user.getStatus() == 1) {
            assertNotLastEnabledAdmin(user);
        }
        userMapper.softDelete(id, operatorId);
        activityRecorder.record("delete", "删除了用户 " + user.getDisplayName(),
                user.getUsername() + " 已从演示工作空间移除", null, operatorId);
    }

    private UserAccount require(long id) {
        UserAccount user = userMapper.selectById(id);
        if (user == null) {
            throw DomainException.notFound("用户不存在或已被删除");
        }
        return user;
    }

    private void assertCanDisable(UserAccount user, long operatorId) {
        if (user.getId() == operatorId) {
            throw new DomainException("不能停用当前登录账号");
        }
        if ("ADMIN".equals(user.getRoleCode())) {
            assertNotLastEnabledAdmin(user);
        }
    }

    private void assertNotLastEnabledAdmin(UserAccount user) {
        if (user.getStatus() == 1 && userMapper.countEnabledAdmins() <= 1) {
            throw new DomainException("必须至少保留一个已启用的管理员账号");
        }
    }

    private void assertUnique(String username, String email, long excludeId) {
        if (userMapper.countByUsername(username.trim(), excludeId) > 0) {
            throw new DomainException("用户名已存在");
        }
        if (userMapper.countByEmail(email.trim(), excludeId) > 0) {
            throw new DomainException("邮箱已存在");
        }
    }

    private void validate(UserInputRequest input) {
        if (input == null
                || isBlank(input.getUsername())
                || isBlank(input.getDisplayName())
                || isBlank(input.getRoleCode())
                || isBlank(input.getEmail())) {
            throw new DomainException("请完整填写用户信息");
        }
        if (input.getDepartmentId() == null) {
            throw new DomainException("请选择所属部门");
        }
    }

    private static String toLike(String keyword) {
        if (isBlank(keyword)) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    private static String emptyToNull(String value) {
        return isBlank(value) || "all".equalsIgnoreCase(value.trim()) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
