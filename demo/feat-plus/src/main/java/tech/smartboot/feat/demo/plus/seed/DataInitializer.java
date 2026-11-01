package tech.smartboot.feat.demo.plus.seed;

import java.util.Calendar;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.demo.plus.dashboard.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.dashboard.mapper.LoginStatMapper;
import tech.smartboot.feat.demo.plus.dashboard.mapper.NotificationMapper;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;
import tech.smartboot.feat.demo.plus.dashboard.model.NotificationRecord;
import tech.smartboot.feat.demo.plus.user.mapper.UserMapper;
import tech.smartboot.feat.demo.plus.user.model.UserAccount;

/**
 * 演示数据初始化器。
 * <p>
 * 首次启动时写入种子数据；reset-demo 复用同一个 reset() 方法，两条路径的数据完全一致。
 * 种子数据由系统写入，creator / editor 一律留 NULL，表示不是任何登录用户操作的。
 */
@Bean
public class DataInitializer {

    @Autowired
    private UserMapper userMapper;

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
        notificationMapper.truncate();
        loginStatMapper.truncate();
        userMapper.truncate();

        for (SeedData.SeedUser seed : SeedData.users()) {
            UserAccount user = new UserAccount();
            user.setUsername(seed.username);
            user.setPasswordHash(SeedData.PASSWORD_HASH);
            user.setDisplayName(seed.displayName);
            user.setRoleCode(seed.roleCode);
            user.setDepartment(seed.department);
            user.setEmail(seed.email);
            user.setStatus(seed.status);
            user.setIsDeleted(0);
            user.setCreatedTime(SeedData.parseTime(seed.createdTime));
            user.setEditTime(SeedData.parseTime(seed.editTime));
            userMapper.insert(user);
        }

        for (SeedData.SeedActivity seed : SeedData.activities()) {
            ActivityRecord record = new ActivityRecord();
            record.setKind(seed.kind);
            record.setTitle(seed.title);
            record.setDescription(seed.description);
            record.setUserId(resolveUserId(seed.username));
            record.setIsDeleted(0);
            record.setCreatedTime(SeedData.parseTime(seed.createdTime));
            record.setEditTime(SeedData.parseTime(seed.createdTime));
            activityMapper.insert(record);
        }

        for (SeedData.SeedNotification seed : SeedData.notifications()) {
            NotificationRecord record = new NotificationRecord();
            record.setTitle(seed.title);
            record.setDescription(seed.description);
            record.setUserId(null);
            record.setReadFlag(seed.readFlag);
            record.setIsDeleted(0);
            record.setCreatedTime(SeedData.parseTime(seed.createdTime));
            record.setEditTime(SeedData.parseTime(seed.createdTime));
            notificationMapper.insert(record);
        }

        seedLoginStats();
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
