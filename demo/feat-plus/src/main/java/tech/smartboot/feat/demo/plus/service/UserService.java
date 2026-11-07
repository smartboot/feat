package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.PageResult;
import tech.smartboot.feat.demo.plus.dto.UserInputRequest;
import tech.smartboot.feat.demo.plus.dto.UserItemResponse;
import tech.smartboot.feat.demo.plus.mapper.UserMapper;
import tech.smartboot.feat.demo.plus.model.DepartmentRecord;
import tech.smartboot.feat.demo.plus.model.RoleRecord;
import tech.smartboot.feat.demo.plus.model.UserAccount;
import tech.smartboot.feat.demo.plus.model.UserRoleRow;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.PasswordHasher;
import tech.smartboot.feat.demo.plus.tx.TxTemplate;

/**
 * 用户管理业务规则：
 * 字段必填、用户名/邮箱唯一、不能停用或删除当前登录账号、必须保留至少一个已启用管理员。
 * <p>
 * 角色是<b>多选</b>：一个人可以同时是部门主管和审计员，权限点取并集、数据范围取最宽。
 * 部门仍是一对一，通过 DepartmentService 校验存在性。
 * <p>
 * 数据权限在这里落地，分两层：
 * <ul>
 *   <li>列表查询把 AccessContext 的可见部门翻译成 SQL 条件，挡住「翻找」；</li>
 *   <li>每个按 id 的读写都调用 {@link AccessContext#canReach} 校验目标，挡住「直接猜 id」。
 *       只做前者等于没做——改一下 URL 里的数字就能越权。</li>
 * </ul>
 * <p>
 * 审计字段：operatorId 是当前登录人，创建时写入 creator + editor，之后每次变更只更新 editTime + editor。
 * 删除是软删除（is_deleted = 1），因此唯一性校验只在未删除的范围内比较。
 * <p>
 * <b>写路径全部走 {@link TxTemplate}。</b>建号要写三张表（sys_user、sys_user_role、sys_activity），
 * 以前三个语句三次自动提交，中间任何一步失败就留下「有账号没角色」或者「有账号没日志」的半截数据，
 * 而且不会有任何报错。事务体里一律用 {@code session.getMapper(...)}：
 * 注入的 Mapper 每个方法自己开事务提交，混用等于把那一笔写落在事务外。
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

    @Autowired
    private TxTemplate txTemplate;

    public PageResult<UserItemResponse> list(String keyword, Integer status, String roleCode,
                                             Long departmentId, int page, int pageSize,
                                             AccessContext access) {
        String like = toLike(keyword);
        String role = emptyToNull(roleCode);
        // 按部门筛选时默认包含全部下级：勾「技术中心」应该能看到平台研发部里的人。
        List<Long> departmentIds = departmentId == null || departmentId <= 0
                ? null : departmentService.subtreeIds(departmentId);
        int safePage = page < 1 ? 1 : page;
        int safeSize = pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        List<Long> scope = access.queryDepartmentIds();
        Long selfOnly = access.selfOnlyUserId();

        long total = userMapper.countPage(like, status, role, departmentIds, scope, selfOnly);
        List<UserAccount> rows = userMapper.selectPage(like, status, role, departmentIds, scope, selfOnly,
                safeSize, (safePage - 1) * safeSize);
        return new PageResult<>(toItems(rows), total, safePage, safeSize);
    }

    public List<UserItemResponse> listAll(AccessContext access) {
        return toItems(userMapper.selectAll(access.queryDepartmentIds(), access.selfOnlyUserId()));
    }

    public UserItemResponse get(long id, AccessContext access) {
        return toItem(requireReachable(userMapper, id, access));
    }

    public UserItemResponse create(UserInputRequest input, AccessContext access) {
        long operatorId = access.getUserId();
        validate(input);
        // 跨 Service 的校验放在事务外：它们只读既有数据，拿不到本事务未提交的行也无所谓。
        List<RoleRecord> roles = roleService.requireAssignables(input.effectiveRoleCodes());
        DepartmentRecord department = departmentService.requireAssignable(input.getDepartmentId());
        if (!access.canPlaceIn(department.getId())) {
            throw DomainException.forbidden("不能在当前数据范围之外的部门创建账号");
        }

        UserAccount saved = txTemplate.call(session -> {
            UserMapper users = session.getMapper(UserMapper.class);
            assertUnique(users, input.getUsername(), input.getEmail(), 0L);

            Date now = new Date();
            UserAccount user = new UserAccount();
            user.setUsername(input.getUsername().trim());
            user.setPasswordHash(PasswordHasher.hash("Feat@" + System.currentTimeMillis()));
            user.setDisplayName(input.getDisplayName().trim());
            user.setDepartmentId(department.getId());
            user.setEmail(input.getEmail().trim());
            user.setStatus(input.getStatus() == null ? 1 : input.getStatus());
            user.setIsDeleted(0);
            user.setCreatedTime(now);
            user.setEditTime(now);
            user.setCreator(operatorId);
            user.setEditor(operatorId);
            users.insert(user);

            UserAccount row = users.selectByUsername(user.getUsername());
            replaceRoles(users, row.getId(), input.effectiveRoleCodes(), operatorId);
            // 审计与被审计的变更同一个事务：回滚时日志一起消失，不会出现「记录说建了、账号却不在」。
            activityRecorder.record("create", "创建了用户 " + row.getDisplayName(),
                    row.getUsername() + " · " + roleNames(roles), row.getId(), operatorId);
            return row;
        });
        return get(saved.getId(), access);
    }

    public UserItemResponse update(long id, UserInputRequest input, AccessContext access) {
        long operatorId = access.getUserId();
        validate(input);
        UserAccount current = requireReachable(userMapper, id, access);
        assertUnique(userMapper, input.getUsername(), input.getEmail(), id);
        List<RoleRecord> roles = roleService.requireAssignables(input.effectiveRoleCodes());
        DepartmentRecord department = departmentService.requireAssignable(input.getDepartmentId());
        if (!access.canPlaceIn(department.getId())) {
            throw DomainException.forbidden("不能把账号调整到当前数据范围之外的部门");
        }

        boolean hadAdmin = userMapper.countUserRole(id, "ADMIN") > 0;
        boolean willHaveAdmin = input.effectiveRoleCodes().contains("ADMIN");
        if (hadAdmin && !willHaveAdmin) {
            assertNotLastEnabledAdmin(current);
        }
        boolean disabling = current.getStatus() == 1 && input.getStatus() != null && input.getStatus() == 0;
        if (disabling) {
            assertCanDisable(current, operatorId, hadAdmin);
        }

        current.setUsername(input.getUsername().trim());
        current.setDisplayName(input.getDisplayName().trim());
        current.setDepartmentId(department.getId());
        current.setEmail(input.getEmail().trim());
        if (input.getStatus() != null) {
            current.setStatus(input.getStatus());
        }
        current.setEditTime(new Date());
        current.setEditor(operatorId);

        txTemplate.run(session -> {
            UserMapper users = session.getMapper(UserMapper.class);
            users.updateProfile(current);
            // 角色与资料必须一起生效：只改了资料没改角色，等于「这个人是审计员」这句话是假的。
            replaceRoles(users, id, input.effectiveRoleCodes(), operatorId);
            activityRecorder.record("update", "更新了用户 " + current.getDisplayName(),
                    current.getUsername() + " 的账号资料已变更 · " + roleNames(roles), current.getId(), operatorId);
        });
        return get(id, access);
    }

    public UserItemResponse setStatus(long id, int status, AccessContext access) {
        long operatorId = access.getUserId();
        UserAccount user = requireReachable(userMapper, id, access);
        if (status == 0) {
            assertCanDisable(user, operatorId, userMapper.countUserRole(id, "ADMIN") > 0);
        }
        txTemplate.run(session -> {
            session.getMapper(UserMapper.class).updateStatus(id, status, operatorId);
            activityRecorder.record(status == 1 ? "enable" : "disable",
                    (status == 1 ? "启用了" : "停用了") + "用户 " + user.getDisplayName(),
                    user.getUsername() + " 的账号状态已更新", id, operatorId);
        });
        return get(id, access);
    }

    public String resetPassword(long id, AccessContext access) {
        long operatorId = access.getUserId();
        UserAccount user = requireReachable(userMapper, id, access);
        String temporaryPassword = "Feat@" + String.format("%03d", id) + "8X";
        txTemplate.run(session -> {
            session.getMapper(UserMapper.class)
                    .updatePassword(id, PasswordHasher.hash(temporaryPassword), operatorId);
            activityRecorder.record("password", "重置了用户 " + user.getDisplayName() + " 的密码",
                    "临时密码已生成并等待安全交付", id, operatorId);
        });
        return temporaryPassword;
    }

    public void delete(long id, AccessContext access) {
        long operatorId = access.getUserId();
        UserAccount user = requireReachable(userMapper, id, access);
        if (id == operatorId) {
            throw new DomainException("不能删除当前登录账号");
        }
        if (userMapper.countUserRole(id, "ADMIN") > 0 && user.getStatus() == 1) {
            assertNotLastEnabledAdmin(user);
        }
        txTemplate.run(session -> {
            UserMapper users = session.getMapper(UserMapper.class);
            users.softDelete(id, operatorId);
            // 关联表一并失效，否则「该角色下有多少人」会把已删除账号算进去。
            users.softDeleteUserRoles(id, operatorId);
            activityRecorder.record("delete", "删除了用户 " + user.getDisplayName(),
                    user.getUsername() + " 已从演示工作空间移除", null, operatorId);
        });
    }

    /**
     * 整体替换某个用户的角色：先全部失效，再逐条恢复或插入。
     * <p>
     * 不能直接 INSERT：sys_user_role 主键是 (user_id, role_code)，软删除的行还在表里，
     * 把角色改回原值时会撞主键。
     * <p>
     * 入参是事务会话上的 Mapper，不是注入的那个——两套 Mapper 混用，后两次写就落在事务外。
     */
    private void replaceRoles(UserMapper users, long userId, List<String> roleCodes, long operatorId) {
        users.softDeleteUserRoles(userId, operatorId);
        for (String code : roleCodes) {
            if (users.restoreUserRole(userId, code, operatorId) == 0) {
                users.insertUserRole(userId, code, operatorId);
            }
        }
    }

    /**
     * 数据权限的「按 id 访问」闸门。
     * 返回 404 而不是 403：不告诉调用方「这个 id 存在但你看不到」，避免被用来枚举用户。
     */
    private UserAccount requireReachable(UserMapper users, long id, AccessContext access) {
        UserAccount user = users.selectById(id);
        if (user == null) {
            throw DomainException.notFound("用户不存在或已被删除");
        }
        if (!access.canReach(user.getId(), user.getDepartmentId())) {
            throw DomainException.notFound("用户不存在或已被删除");
        }
        return user;
    }

    private List<UserItemResponse> toItems(List<UserAccount> rows) {
        List<UserItemResponse> items = new ArrayList<>(rows.size());
        if (rows.isEmpty()) {
            return items;
        }
        // 一次批量查询补齐当前页的角色，避免每行查一次（N+1）。
        List<Long> userIds = new ArrayList<>(rows.size());
        for (UserAccount row : rows) {
            userIds.add(row.getId());
        }
        Map<Long, List<String>> codes = new HashMap<>();
        Map<Long, List<String>> names = new HashMap<>();
        for (UserRoleRow role : userMapper.selectRolesByUserIds(userIds)) {
            putInto(codes, role.getUserId(), role.getRoleCode());
            putInto(names, role.getUserId(), role.getRoleName());
        }
        for (UserAccount row : rows) {
            items.add(UserItemResponse.from(row, codes.get(row.getId()), names.get(row.getId())));
        }
        return items;
    }

    private UserItemResponse toItem(UserAccount account) {
        Map<Long, List<String>> codes = new HashMap<>();
        Map<Long, List<String>> names = new HashMap<>();
        for (UserRoleRow role : userMapper.selectRolesByUserIds(Collections.singletonList(account.getId()))) {
            putInto(codes, role.getUserId(), role.getRoleCode());
            putInto(names, role.getUserId(), role.getRoleName());
        }
        return UserItemResponse.from(account, codes.get(account.getId()), names.get(account.getId()));
    }

    private static void putInto(Map<Long, List<String>> map, long userId, String value) {
        List<String> bucket = map.get(userId);
        if (bucket == null) {
            bucket = new ArrayList<>(2);
            map.put(userId, bucket);
        }
        bucket.add(value);
    }

    private static String roleNames(List<RoleRecord> roles) {
        StringBuilder builder = new StringBuilder();
        for (RoleRecord role : roles) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(role.getName());
        }
        return builder.toString();
    }

    private void assertCanDisable(UserAccount user, long operatorId, boolean admin) {
        if (user.getId() == operatorId) {
            throw new DomainException("不能停用当前登录账号");
        }
        if (admin) {
            assertNotLastEnabledAdmin(user);
        }
    }

    private void assertNotLastEnabledAdmin(UserAccount user) {
        if (user.getStatus() == 1 && userMapper.countEnabledAdmins() <= 1) {
            throw new DomainException("必须至少保留一个已启用的管理员账号");
        }
    }

    private void assertUnique(UserMapper users, String username, String email, long excludeId) {
        if (users.countByUsername(username.trim(), excludeId) > 0) {
            throw new DomainException("用户名已存在");
        }
        if (users.countByEmail(email.trim(), excludeId) > 0) {
            throw new DomainException("邮箱已存在");
        }
    }

    private void validate(UserInputRequest input) {
        if (input == null
                || isBlank(input.getUsername())
                || isBlank(input.getDisplayName())
                || isBlank(input.getEmail())) {
            throw new DomainException("请完整填写用户信息");
        }
        if (input.effectiveRoleCodes().isEmpty()) {
            throw new DomainException("请至少选择一个角色");
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
