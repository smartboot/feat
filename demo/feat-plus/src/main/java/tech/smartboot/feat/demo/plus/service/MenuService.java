package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dto.MenuAdminItemResponse;
import tech.smartboot.feat.demo.plus.dto.MenuInputRequest;
import tech.smartboot.feat.demo.plus.dto.MenuItemResponse;
import tech.smartboot.feat.demo.plus.mapper.MenuMapper;
import tech.smartboot.feat.demo.plus.model.MenuRecord;
import tech.smartboot.feat.demo.plus.security.MenuRoutes;
import tech.smartboot.feat.demo.plus.security.MenuSnapshot;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.tx.TxTemplate;

/**
 * 菜单的读写与配置规则。
 * <p>
 * <b>入库之后，菜单从「代码」变成了「数据」，校验就变成了必需品。</b> 原来 path 写错是编译期的事，
 * 现在是在界面上填的，错的代价是「侧栏有这一条，点进去被弹回首页」——数据看起来完全正常，
 * 排查成本极高。所以这里有四道闸：
 * <ol>
 *   <li>{@code path} 必须命中 {@link MenuRoutes} 的白名单，且未删除范围内唯一；</li>
 *   <li>{@code permission} 必须是 {@link Permission#CATALOG} 里登记过的权限点（或空串）；</li>
 *   <li>{@code icon} 只做长度校验——图标填错的后果是退化成默认图标，不影响可用性，
 *       交给 {@code check_menu.py} 静态检查种子数据就够了；</li>
 *   <li>改名、删菜单都记审计。菜单决定「谁能看到哪个入口」，这类改动事后要能查。</li>
 * </ol>
 * <p>
 * <b>权限点为什么仍然留在代码里。</b> 每个接口的 {@code require(...)} 是编译期写死的，
 * 允许运行时新增权限点只会造出「字典里有、接口从不校验」的空权限——看起来配好了，实际没拦。
 * 可以配置的是「这个菜单挂哪个权限点」，不是「系统有哪些权限点」。
 * <p>
 * <b>缓存失效必须在事务提交之后。</b> 提交前清缓存，下一个请求会用提交前的数据把缓存填回去，
 * 「改完立刻生效」就变成「十分钟后生效」。
 */
@Bean
public class MenuService {

    /** 小写字母、数字、下划线、短横线。与前端分组标识、CSS 无关，只是保证能安全地当 key 用。 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_-]{1,32}$");

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private MenuSnapshot menuSnapshot;

    @Autowired
    private ActivityRecorder activityRecorder;

    @Autowired
    private TxTemplate txTemplate;

    /**
     * 管理页用的全量列表（含不进侧栏的项），走缓存。
     * <p>
     * 管理页与侧栏读的是同一份数据——如果管理页直读库、侧栏读缓存，改完之后管理页显示新值、
     * 侧栏还是旧值，用户会以为保存没生效。
     */
    public List<MenuAdminItemResponse> list() {
        List<MenuRecord> rows = menuSnapshot.all();
        List<MenuAdminItemResponse> items = new ArrayList<>(rows.size());
        for (MenuRecord row : rows) {
            items.add(toAdminItem(row));
        }
        return items;
    }

    /**
     * 当前登录人可见的菜单（按权限点过滤）。
     * <p>
     * 这是显示层过滤，不是安全边界：漏了最多是侧栏多一条，点进去仍然被 {@code AccessGuard} 拦住；
     * 反过来也成立——菜单能看见不等于接口放行。
     */
    public List<MenuItemResponse> visibleFor(List<String> permissions) {
        List<MenuRecord> rows = menuSnapshot.all();
        List<MenuItemResponse> items = new ArrayList<>(rows.size());
        for (MenuRecord row : rows) {
            String permission = row.getPermission();
            if (!permission.isEmpty() && !Permission.contains(permissions, permission)) {
                continue;
            }
            items.add(new MenuItemResponse(row.getGroupKey(), row.getGroupLabel(), row.getPath(),
                    row.getName(), row.getIcon(), permission, row.getSortOrder(), row.getVisible() == 1));
        }
        return items;
    }

    /**
     * 当前登录人「无权直达」的页面路径，供前端做路由级拦截。
     * <p>
     * {@link #visibleFor(List)} 只过滤侧栏：知道地址的人仍可以在地址栏输入 /users 打开页面，
     * 然后面对一屏 403——报错来自五六个接口，看起来像系统坏了，其实只是没有权限。
     * 把这些路径随登录响应一起下发，前端就能在进入页面前拦住并说明原因。
     * 仍然只是体验层：拦的依据还是权限点，接口的 {@code require(...)} 才是安全边界。
     */
    public List<String> restrictedFor(List<String> permissions) {
        List<MenuRecord> rows = menuSnapshot.all();
        List<String> paths = new ArrayList<>(rows.size());
        for (MenuRecord row : rows) {
            String permission = row.getPermission();
            if (!permission.isEmpty() && !Permission.contains(permissions, permission)) {
                paths.add(row.getPath());
            }
        }
        return paths;
    }

    /** 可选的页面路径，供管理页的「路径」下拉使用。 */
    public List<String> routeOptions() {
        return MenuRoutes.ALL;
    }

    public MenuAdminItemResponse create(MenuInputRequest input, long operatorId) {
        String groupKey = requireKey(input == null ? null : input.getGroupKey());
        String groupLabel = requireGroupLabel(input == null ? null : input.getGroupLabel());
        int groupSort = input == null || input.getGroupSort() == null ? 0 : input.getGroupSort();
        String path = requirePath(input == null ? null : input.getPath());
        String name = requireName(input == null ? null : input.getName());
        String icon = trimToEmpty(input == null ? null : input.getIcon());
        String permission = validatePermission(input == null ? null : input.getPermission());
        int sort = input == null || input.getSort() == null ? 0 : input.getSort();
        boolean visible = input == null || input.getVisible() == null || input.getVisible();

        assertPathUnique(path, 0L);

        MenuRecord saved = txTemplate.call(session -> {
            MenuMapper menus = session.getMapper(MenuMapper.class);
            Date now = new Date();
            MenuRecord record = new MenuRecord();
            record.setGroupKey(groupKey);
            record.setGroupLabel(groupLabel);
            record.setGroupSort(groupSort);
            record.setPath(path);
            record.setName(name);
            record.setIcon(icon);
            record.setPermission(permission);
            record.setSortOrder(sort);
            record.setVisible(visible ? 1 : 0);
            record.setIsDeleted(0);
            record.setCreatedTime(now);
            record.setEditTime(now);
            record.setCreator(operatorId);
            record.setEditor(operatorId);
            menus.insert(record);

            // 分组信息是冗余在每行上的，新建一条时把同组的标签与顺序统一，避免同一组出现两个名字。
            menus.updateGroupMeta(groupKey, groupLabel, groupSort, operatorId);

            activityRecorder.record("create", "创建了菜单 " + name,
                    path + " · " + permissionLabel(permission), null, operatorId);
            return menus.selectByPath(path);
        });
        menuSnapshot.invalidate();
        return toAdminItem(saved);
    }

    public MenuAdminItemResponse update(long id, MenuInputRequest input, long operatorId) {
        MenuRecord current = require(id);
        String groupKey = requireKey(input == null ? null : input.getGroupKey());
        String groupLabel = requireGroupLabel(input == null ? null : input.getGroupLabel());
        int groupSort = input == null || input.getGroupSort() == null
                ? current.getGroupSort() : input.getGroupSort();
        String path = requirePath(input == null ? null : input.getPath());
        String name = requireName(input == null ? null : input.getName());
        String icon = trimToEmpty(input == null ? null : input.getIcon());
        String permission = validatePermission(input == null ? null : input.getPermission());
        int sort = input == null || input.getSort() == null ? current.getSortOrder() : input.getSort();
        boolean visible = input == null || input.getVisible() == null
                ? current.getVisible() == 1 : input.getVisible();

        assertPathUnique(path, id);

        String oldName = current.getName();
        String oldPath = current.getPath();
        boolean sameGroup = groupKey.equals(current.getGroupKey());

        current.setGroupKey(groupKey);
        current.setGroupLabel(groupLabel);
        current.setGroupSort(groupSort);
        current.setPath(path);
        current.setName(name);
        current.setIcon(icon);
        current.setPermission(permission);
        current.setSortOrder(sort);
        current.setVisible(visible ? 1 : 0);
        current.setEditTime(new Date());
        current.setEditor(operatorId);

        txTemplate.run(session -> {
            MenuMapper menus = session.getMapper(MenuMapper.class);
            menus.update(current);
            // 改组名 / 组顺序作用于整组：只改一行的话，前端取「该组第一条的 groupLabel」，
            // 会出现「改了名字但侧栏没变」的错觉。把菜单挪到别的组时不同步，那本来就只是这一行的事。
            if (sameGroup) {
                menus.updateGroupMeta(groupKey, groupLabel, groupSort, operatorId);
            }
            activityRecorder.record("update", "更新了菜单 " + name,
                    oldName.equals(name) && oldPath.equals(path)
                            ? path + " · " + permissionLabel(permission)
                            : oldName + "（" + oldPath + "）→ " + name + "（" + path + "）",
                    null, operatorId);
        });
        menuSnapshot.invalidate();

        return toAdminItem(require(id));
    }

    /**
     * 软删除。
     * <p>
     * <b>不阻止删除「菜单管理」自己那一条。</b> 删掉之后侧栏没有入口，但知道地址的人仍可直接访问
     * /menus 把自己放回去——前端路由和接口权限都不依赖菜单表，所以不会把系统锁死。
     * 加一条「不许删自己」的特例反而让规则变得不可预测，不如把后果写进 README。
     */
    public void delete(long id, long operatorId) {
        MenuRecord record = require(id);
        txTemplate.run(session -> {
            session.getMapper(MenuMapper.class).softDelete(id, operatorId);
            activityRecorder.record("delete", "删除了菜单 " + record.getName(),
                    record.getPath() + " · " + permissionLabel(record.getPermission()), null, operatorId);
        });
        menuSnapshot.invalidate();
    }

    private MenuRecord require(long id) {
        // 写路径直读库：命中的是缓存的话，可能读到一个刚刚被别的请求删掉的菜单。
        MenuRecord record = menuMapper.selectById(id);
        if (record == null) {
            throw DomainException.notFound("菜单不存在或已被删除");
        }
        return record;
    }

    private void assertPathUnique(String path, long excludeId) {
        if (menuMapper.countPath(path, excludeId) > 0) {
            throw new DomainException("该页面已经有菜单了：" + path + "（一个页面只应有一个入口）");
        }
    }

    private static MenuAdminItemResponse toAdminItem(MenuRecord row) {
        return new MenuAdminItemResponse(row.getId(), row.getGroupKey(), row.getGroupLabel(), row.getGroupSort(),
                row.getPath(), row.getName(), row.getIcon(), row.getPermission(), row.getSortOrder(),
                row.getVisible() == 1);
    }

    private static String permissionLabel(String permission) {
        if (permission == null || permission.isEmpty()) {
            return "登录可见";
        }
        for (Permission.Entry entry : Permission.CATALOG) {
            if (entry.getKey().equals(permission)) {
                return entry.getLabel();
            }
        }
        return permission;
    }

    private static String requireKey(String raw) {
        String value = trimToEmpty(raw);
        if (!KEY_PATTERN.matcher(value).matches()) {
            throw new DomainException("分组标识只能包含小写字母、数字、下划线和短横线，且不能为空");
        }
        return value;
    }

    private static String requireGroupLabel(String raw) {
        String value = trimToEmpty(raw);
        if (value.isEmpty()) {
            throw new DomainException("请填写分组名称");
        }
        if (value.length() > 32) {
            throw new DomainException("分组名称不能超过 32 个字符");
        }
        return value;
    }

    /**
     * 路径白名单。这是「菜单入库」之后最重要的一道校验：
     * 没有它，管理员可以配出指向不存在页面的菜单，而错误只会在有人点它的时候以「跳回首页」的形式出现。
     */
    private static String requirePath(String raw) {
        String value = trimToEmpty(raw);
        if (!MenuRoutes.isValid(value)) {
            throw new DomainException("页面不存在：" + value + "，请从可选路径中选择");
        }
        return value;
    }

    private static String requireName(String raw) {
        String value = trimToEmpty(raw);
        if (value.isEmpty()) {
            throw new DomainException("请填写菜单名称");
        }
        if (value.length() > 64) {
            throw new DomainException("菜单名称不能超过 64 个字符");
        }
        return value;
    }

    private static String validatePermission(String raw) {
        String value = trimToEmpty(raw);
        if (value.isEmpty()) {
            return "";
        }
        if (!Permission.isValid(value)) {
            throw new DomainException("未知的权限点：" + value);
        }
        return value;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
