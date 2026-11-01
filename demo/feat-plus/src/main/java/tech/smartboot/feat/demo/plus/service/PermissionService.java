package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.List;

import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.dto.PermissionItemResponse;
import tech.smartboot.feat.demo.plus.security.Permission;

/**
 * 权限点字典的对外形态。
 * <p>
 * 存在的唯一理由是分层：{@code Permission} 在 security 层，不能依赖 dto；而控制器只能返回 dto。
 * 所以在 service 层把 entry 搬成 response，而不是让 security 反过来依赖 dto（那会成环）。
 * <p>
 * 没有缓存：字典是常量列表，一次组装是 9 个对象的分配，比查一次缓存还便宜。
 */
@Bean
public class PermissionService {

    public List<PermissionItemResponse> catalog() {
        List<Permission.Entry> entries = Permission.CATALOG;
        List<PermissionItemResponse> items = new ArrayList<>(entries.size());
        int sort = 0;
        for (Permission.Entry entry : entries) {
            sort += 10;
            items.add(new PermissionItemResponse(entry.getGroupKey(), entry.getGroupLabel(), entry.getKey(),
                    entry.getLabel(), entry.getDescription(), sort));
        }
        return items;
    }
}
