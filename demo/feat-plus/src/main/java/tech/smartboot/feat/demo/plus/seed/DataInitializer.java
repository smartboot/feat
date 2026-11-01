package tech.smartboot.feat.demo.plus.seed;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.demo.plus.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.mapper.DepartmentMapper;
import tech.smartboot.feat.demo.plus.mapper.LoginStatMapper;
import tech.smartboot.feat.demo.plus.mapper.NotificationMapper;
import tech.smartboot.feat.demo.plus.mapper.RoleMapper;
import tech.smartboot.feat.demo.plus.mapper.UserMapper;
import tech.smartboot.feat.demo.plus.model.ActivityRecord;
import tech.smartboot.feat.demo.plus.model.DepartmentRecord;
import tech.smartboot.feat.demo.plus.model.NotificationRecord;
import tech.smartboot.feat.demo.plus.model.RoleRecord;
import tech.smartboot.feat.demo.plus.model.UserAccount;

/**
 * 演示数据初始化器。
 * <p>
 * 首次启动时写入种子数据；reset-demo 复用同一个 reset() 方法，两条路径的数据完全一致。
 * 种子数据由系统写入，creator / editor 一律留 NULL，表示不是任何登录用户操作的。
 * <p>
 * 写入顺序由外键关系决定：角色 → 部门（先父后子）→ 用户 → 用户角色关联 → 动态 / 通知 / 登录统计。
 */
@Bean
public class DataInitializer {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private LoginStatMapper loginStatMapper;

    @PostConstruct
    public void init() {
        if (userMapper.countAll() == 0) {
            reset();
        }
    }

    public void reset() {
        activityMapper.truncate();
        // 已读状态是通知的从属数据，必须先清，否则重置后旧 id 的已读记录会串到新通知上。
        notificationMapper.truncateReads();
        notificationMapper.truncate();
        loginStatMapper.truncate();
        // 关联表先清：sys_user_role / sys_role_dept 的主键是业务组合键，不自增，
        // 但顺序上仍应先清关联再清主表，避免以后加外键时返工。
        userMapper.truncateUserRoles();
        roleMapper.truncateRoleDepts();
        userMapper.truncate();
        roleMapper.truncate();
        departmentMapper.truncate();

        seedRoles();
        Map<String, Long> departmentIds = seedDepartments();
        seedUsers(departmentIds);
        seedActivities();
        seedNotifications();
        seedLoginStats();
    }

    private void seedRoles() {
        Date now = new Date();
        for (SeedData.SeedRole seed : SeedData.roles()) {
            RoleRecord role = new RoleRecord();
            role.setCode(seed.code);
            role.setName(seed.name);
            role.setDescription(seed.description);
            role.setPermissions(seed.permissions);
            role.setDepartmentScope(seed.departmentScope);
            role.setBuiltin(seed.builtin);
            role.setStatus(seed.status);
            role.setIsDeleted(0);
            role.setCreatedTime(now);
            role.setEditTime(now);
            roleMapper.insert(role);
        }
    }

    /**
     * 先按先父后子的顺序插入，再从库里回读 id 回填 parent_id。
     * 不依赖自增序列的具体取值，避免以后调整种子顺序时层级错乱。
     */
    private Map<String, Long> seedDepartments() {
        Date now = new Date();
        for (SeedData.SeedDepartment seed : SeedData.departments()) {
            DepartmentRecord record = new DepartmentRecord();
            record.setParentId(0L);
            record.setCode(seed.code);
            record.setName(seed.name);
            record.setSortOrder(seed.sortOrder);
            record.setDescription(seed.description);
            record.setStatus(1);
            record.setIsDeleted(0);
            record.setCreatedTime(now);
            record.setEditTime(now);
            departmentMapper.insert(record);
        }

        Map<String, Long> ids = new HashMap<>();
        for (DepartmentRecord record : departmentMapper.selectAll()) {
            ids.put(record.getCode(), record.getId());
        }
        for (SeedData.SeedDepartment seed : SeedData.departments()) {
            if (seed.parentCode.isEmpty()) {
                continue;
            }
            Long parentId = ids.get(seed.parentCode);
            if (parentId != null) {
                departmentMapper.updateParent(ids.get(seed.code), parentId);
            }
        }
        return ids;
    }

    private void seedUsers(Map<String, Long> departmentIds) {
        for (SeedData.SeedUser seed : SeedData.users()) {
            UserAccount user = new UserAccount();
            user.setUsername(seed.username);
            user.setPasswordHash(SeedData.PASSWORD_HASH);
            user.setDisplayName(seed.displayName);
            user.setDepartmentId(departmentIds.get(seed.departmentCode));
            user.setEmail(seed.email);
            user.setStatus(seed.status);
            user.setIsDeleted(0);
            user.setCreatedTime(SeedData.parseTime(seed.createdTime));
            user.setEditTime(SeedData.parseTime(seed.editTime));
            userMapper.insert(user);

            UserAccount saved = userMapper.selectByUsername(seed.username);
            for (String roleCode : seed.roleCodes) {
                userMapper.insertUserRole(saved.getId(), roleCode, 0L);
            }
        }
    }

    private void seedActivities() {
        for (SeedData.SeedActivity seed : SeedData.activities()) {
            ActivityRecord record = new ActivityRecord();
            record.setKind(seed.kind);
            record.setTitle(seed.title);
            record.setDescription(seed.description);
            record.setUserId(resolveUserId(seed.username));
            Long operator = resolveUserId(seed.operator);
            record.setCreator(operator);
            record.setEditor(operator);
            record.setIsDeleted(0);
            record.setCreatedTime(SeedData.parseTime(seed.createdTime));
            record.setEditTime(SeedData.parseTime(seed.createdTime));
            activityMapper.insert(record);
        }
    }

    private void seedNotifications() {
        for (SeedData.SeedNotification seed : SeedData.notifications()) {
            NotificationRecord record = new NotificationRecord();
            record.setTitle(seed.title);
            record.setDescription(seed.description);
            // 0 表示广播，落库时统一存 NULL，查询里 NULL 与 0 都按广播处理。
            record.setUserId(seed.recipientUserId == 0 ? null : Long.valueOf(seed.recipientUserId));
            record.setIsDeleted(0);
            record.setCreatedTime(SeedData.parseTime(seed.createdTime));
            record.setEditTime(SeedData.parseTime(seed.createdTime));
            notificationMapper.insert(record);
        }
    }

    /**
     * 生成最近 30 天的登录统计种子，让仪表盘趋势图在首次打开时就有历史曲线。
     * 之后每次真实登录都会在当前日期上累加。
     */
    private void seedLoginStats() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -29);
        for (int i = 0; i < 30; i++) {
            int count = 36 + ((i * 11) % 31) + (i > 20 ? 8 : 0);
            loginStatMapper.insert(calendar.getTime(), count);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private Long resolveUserId(String username) {
        if (username == null) {
            return null;
        }
        UserAccount user = userMapper.selectByUsername(username);
        return user == null ? null : user.getId();
    }
}
