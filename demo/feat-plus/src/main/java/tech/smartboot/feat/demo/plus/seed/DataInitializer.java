package tech.smartboot.feat.demo.plus.seed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.io.Resources;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.demo.plus.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.mapper.DepartmentMapper;
import tech.smartboot.feat.demo.plus.mapper.LoginStatMapper;
import tech.smartboot.feat.demo.plus.mapper.MenuMapper;
import tech.smartboot.feat.demo.plus.mapper.NotificationMapper;
import tech.smartboot.feat.demo.plus.mapper.PageMapper;
import tech.smartboot.feat.demo.plus.mapper.RoleMapper;
import tech.smartboot.feat.demo.plus.mapper.UserMapper;
import tech.smartboot.feat.demo.plus.model.ActivityRecord;
import tech.smartboot.feat.demo.plus.model.DepartmentRecord;
import tech.smartboot.feat.demo.plus.model.MenuRecord;
import tech.smartboot.feat.demo.plus.model.NotificationRecord;
import tech.smartboot.feat.demo.plus.model.PageRecord;
import tech.smartboot.feat.demo.plus.model.RoleRecord;
import tech.smartboot.feat.demo.plus.model.UserAccount;
import tech.smartboot.feat.demo.plus.security.MenuSnapshot;
import tech.smartboot.feat.demo.plus.service.PageSnapshot;

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

    /**
     * sys_page 初始配置。表为空时导入（首次启动、reset-demo、存量库升级到这张表）。
     * <p>
     * 建表仍由 feat.yaml 的 {@code initial-sql} 负责，这里只负责灌配置——两件事分开之后，
     * 表结构变更不需要动配置，改一条列宽也不需要动 SQL。
     */
    private static final String PAGES = "pages/pages.json";

    /** 与 PageService.KEY_PATTERN 同源：pageKey 同时是菜单 path 段和前端拉配置的键。 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_-]{1,32}$");

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

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private MenuSnapshot menuSnapshot;

    @Autowired
    private PageSnapshot pageSnapshot;

    /**
     * 首次启动装种子数据；存量库补菜单。
     * <p>
     * 两条分支不能合并：{@link #reset()} 会清空全部业务数据，而「菜单表是新加的」这件事只发生在
     * 已经有数据的库上——那种库升级上来时，用户表非空、菜单表为空，需要的只是补一份菜单，
     * 不是把客户的数据清掉重来。这个分支就是「给存量库加表」的最小迁移路径。
     * <p>
     * 首次启动走 {@link #seedAll()} 而不是 {@link #reset()}：Feat 的 @PostConstruct 按注册顺序执行，
     * 不感知 bean 依赖，DataInitializer 可能排在 CacheStoreProvider 之前——那时调 menuSnapshot.invalidate()
     * 会撞上「缓存尚未初始化」。首次启动本来也无须失效：进程内缓存是空的；Redis 后端下上一个进程
     * 留下的旧条目由 TTL 兜底，与「TTL 兜住漏失效路径」是同一条原则。
     */
    @PostConstruct
    public void init() {
        if (userMapper.countAll() == 0) {
            seedAll();
        } else if (menuMapper.countAll() == 0) {
            seedMenus();
        } else if (pageMapper.countAll() == 0) {
            // 动态页面配置是后加的表：存量库升级时补一份种子，与菜单分支是同一个迁移套路。
            seedPages();
        }
    }

    public void reset() {
        truncateAll();
        seedAll();
        // 重置换了菜单行的 id，缓存里还是上一批。不清的话「恢复演示数据」之后侧栏还是改过的样子，
        // 要等十分钟 TTL 到期才恢复——这正是「缓存忘了失效」最典型的现场。
        // 页面配置的缓存同理：重置换了行的 id，缓存里还是上一批。
        menuSnapshot.invalidate();
        pageSnapshot.invalidate();
    }

    private void truncateAll() {
        // 已读状态是通知的从属数据，必须先清，否则重置后旧 id 的已读记录会串到新通知上。
        notificationMapper.truncateReads();
        notificationMapper.truncate();
        loginStatMapper.truncate();
        // 活动日志也必须清：不清的话每次 reset 都把种子活动追加一遍，
        // 「日志条数 / 操作人下拉」的回归断言会随 reset 次数线性漂移。
        activityMapper.truncate();
        // 关联表先清：sys_user_role / sys_role_dept 的主键是业务组合键，不自增，
        // 但顺序上仍应先清关联再清主表，避免以后加外键时返工。
        userMapper.truncateUserRoles();
        roleMapper.truncateRoleDepts();
        userMapper.truncate();
        roleMapper.truncate();
        departmentMapper.truncate();
        menuMapper.truncate();
        pageMapper.truncate();
    }

    private void seedAll() {
        seedRoles();
        Map<String, Long> departmentIds = seedDepartments();
        seedUsers(departmentIds);
        seedActivities();
        seedNotifications();
        seedLoginStats();
        seedMenus();
        seedPages();
    }

    /**
     * 菜单表初始化。菜单是配置数据，落库之后由「菜单管理」页维护，
     * 这里只负责给出第一份可用的侧边栏——包括「菜单管理」自己那一条。
     */
    private void seedMenus() {
        Date now = new Date();
        for (SeedData.SeedMenu seed : SeedData.menus()) {
            MenuRecord record = new MenuRecord();
            record.setGroupKey(seed.groupKey);
            record.setGroupLabel(seed.groupLabel);
            record.setGroupSort(seed.groupSort);
            record.setPath(seed.path);
            record.setName(seed.name);
            record.setIcon(seed.icon);
            record.setPermission(seed.permission);
            record.setSortOrder(seed.sort);
            record.setVisible(seed.visible ? 1 : 0);
            record.setIsDeleted(0);
            record.setCreatedTime(now);
            record.setEditTime(now);
            menuMapper.insert(record);
        }
    }

    /**
     * 页面配置初始化：把 {@code pages/pages.json} 逐条导入 sys_page。
     * <p>
     * 配置的唯一真相在那份 JSON 里，Java 只负责搬运。放在 JSON 而不是 SQL 字符串里的原因很实际：
     * 它是嵌套结构，塞进 SQL 后不能出现 ASCII 分号（ScriptRunner 按分号切语句）、不能用单引号、
     * 整行无法 diff——三条纪律都只是为了迁就脚本执行器。
     * <p>
     * 导入只做结构校验，不做「列 / 筛选键是否存在于数据源」的交叉校验：那需要
     * {@code PageDataSourceRegistry}，而 @PostConstruct 顺序上本类排在它之前（AOT 按注册顺序执行）。
     * 交叉校验放在两处——保存配置时由 {@code PageService} 拒，提交前由 {@code check_menu.py} 查。
     */
    private void seedPages() {
        if (pageMapper.countAll() > 0) {
            return;
        }
        JSONArray seeds = readPageSeeds();
        // 种子配置是代码的一部分：写错就在启动时炸，而不是让某个页面白屏到被人发现。
        Date now = new Date();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < seeds.size(); i++) {
            PageRecord record = toPageRecord(seeds.getJSONObject(i), i + 1, seen);
            record.setIsDeleted(0);
            record.setCreatedTime(now);
            record.setEditTime(now);
            pageMapper.insert(record);
        }
    }

    private JSONArray readPageSeeds() {
        String text;
        try (InputStream in = Resources.getResourceAsStream(PAGES)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            text = new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取页面配置 " + PAGES + " 失败：" + e);
        }
        JSONArray seeds;
        try {
            seeds = JSON.parseArray(text);
        } catch (RuntimeException e) {
            throw new IllegalStateException(PAGES + " 不是合法的 JSON 数组：" + e);
        }
        if (seeds == null || seeds.isEmpty()) {
            throw new IllegalStateException(PAGES + " 里没有任何页面配置，首次启动会得到空侧栏");
        }
        return seeds;
    }

    /**
     * 一份配置 = 一行 sys_page。list / form 在 JSON 里是嵌套对象，落库时序列化成字符串，
     * 与「配置管理页保存」走完全相同的存储形态——运行期读路径不区分种子还是人工配置。
     */
    private PageRecord toPageRecord(JSONObject seed, int ordinal, Set<String> seen) {
        String where = PAGES + " 第 " + ordinal + " 项";
        String pageKey = trim(seed.getString("pageKey"));
        if (pageKey == null || !KEY_PATTERN.matcher(pageKey).matches()) {
            throw new IllegalStateException(where + "的 pageKey 不合法：" + pageKey
                    + "（需匹配 " + KEY_PATTERN.pattern() + "）");
        }
        if (!seen.add(pageKey)) {
            throw new IllegalStateException(where + "的 pageKey 重复：" + pageKey);
        }
        String title = trim(seed.getString("title"));
        if (title == null) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 title");
        }
        String datasourceKey = trim(seed.getString("datasourceKey"));
        if (datasourceKey == null) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 datasourceKey");
        }
        JSONObject list = seed.getJSONObject("list");
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 list 配置");
        }
        JSONObject form = seed.getJSONObject("form");
        if (form == null) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 form 配置（只读页面写空 fields）");
        }

        PageRecord record = new PageRecord();
        record.setPageKey(pageKey);
        record.setTitle(title);
        record.setDescription(trim(seed.getString("description")));
        record.setDatasourceKey(datasourceKey);
        record.setListJson(list.toJSONString());
        record.setFormJson(form.toJSONString());
        return record;
    }

    private static String trim(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
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
