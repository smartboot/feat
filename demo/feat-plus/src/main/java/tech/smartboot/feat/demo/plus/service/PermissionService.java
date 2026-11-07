package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.dto.PermissionItemResponse;
import tech.smartboot.feat.demo.plus.model.MenuRecord;
import tech.smartboot.feat.demo.plus.security.MenuSnapshot;
import tech.smartboot.feat.demo.plus.security.Permission;

/**
 * 权限点字典的对外形态。
 * <p>
 * 存在的理由是分层：{@code Permission} 在 security 层，不能依赖 dto；而控制器只能返回 dto。
 * 所以在 service 层把 entry 搬成 response，而不是让 security 反过来依赖 dto（那会成环）。
 * <p>
 * 每个权限点还带 menuCount——它挂在多少条菜单上。权限点既被接口的 require(...) 用，
 * 又被菜单的「所需权限点」用，是两条链路之间的那颗螺丝；勾选一个权限点会开关哪些侧栏入口，
 * 只有把菜单表一起读了才知道。走缓存（menuSnapshot），与菜单管理页、侧栏是同一份数据，
 * 不会出现「管理页改完这里还是旧数」。
 */
@Bean
public class PermissionService {

    @Autowired
    private MenuSnapshot menuSnapshot;

    public List<PermissionItemResponse> catalog() {
        Map<String, Integer> menuCounts = countMenusByPermission();
        List<Permission.Entry> entries = Permission.CATALOG;
        List<PermissionItemResponse> items = new ArrayList<>(entries.size());
        int sort = 0;
        for (Permission.Entry entry : entries) {
            sort += 10;
            items.add(new PermissionItemResponse(entry.getGroupKey(), entry.getGroupLabel(), entry.getKey(),
                    entry.getLabel(), entry.getDescription(), sort,
                    menuCounts.getOrDefault(entry.getKey(), 0)));
        }
        return items;
    }

    /**
     * 权限点 key → 挂在该权限点上的菜单条数。
     * <p>
     * 一个权限点可以挂在多条菜单上：path 唯一约束拦的是「同一页面两个入口」，
     * 不拦「两个页面共用同一个权限点」。隐藏菜单（visible=0）也计数——它虽不进侧栏，
     * 仍是配置面的一部分，删掉权限点影响的是整条配置。
     */
    private Map<String, Integer> countMenusByPermission() {
        List<MenuRecord> menus = menuSnapshot.all();
        Map<String, Integer> counts = new HashMap<>(menus.size());
        for (MenuRecord menu : menus) {
            String permission = menu.getPermission();
            if (permission != null && !permission.isEmpty()) {
                counts.merge(permission, 1, Integer::sum);
            }
        }
        return counts;
    }
}
