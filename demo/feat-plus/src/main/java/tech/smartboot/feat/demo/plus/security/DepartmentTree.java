package tech.smartboot.feat.demo.plus.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tech.smartboot.feat.demo.plus.model.DepartmentRecord;

/**
 * 部门树的纯函数计算。
 * <p>
 * 放在 security 而不是 service，是为了打断依赖环：数据范围解析（{@link DataScopeResolver}）
 * 需要「本部门及以下」的 id 集合，而机制层一旦反向依赖业务层，security 与 service 就成环，
 * 之后任何一侧都无法单独抽走。这里只接受调用方传入的部门行，不自己查库，
 * 所以两边都能用同一份算法，不必各写一遍。
 */
public final class DepartmentTree {

    private DepartmentTree() {
    }

    /**
     * 某部门及其全部下级的 id 集合，含自身。父链成环时也能终止：已加入集合的节点不会重复入队。
     */
    public static List<Long> subtreeIds(List<DepartmentRecord> rows, long rootId) {
        Map<Long, List<Long>> children = new HashMap<>();
        for (DepartmentRecord row : rows) {
            List<Long> bucket = children.get(row.getParentId());
            if (bucket == null) {
                bucket = new ArrayList<>();
                children.put(row.getParentId(), bucket);
            }
            bucket.add(row.getId());
        }

        Set<Long> result = new LinkedHashSet<>();
        List<Long> queue = new ArrayList<>();
        queue.add(rootId);
        result.add(rootId);
        while (!queue.isEmpty()) {
            List<Long> next = new ArrayList<>();
            for (Long id : queue) {
                List<Long> bucket = children.get(id);
                if (bucket == null) {
                    continue;
                }
                for (Long child : bucket) {
                    if (result.add(child)) {
                        next.add(child);
                    }
                }
            }
            queue = next;
        }
        return new ArrayList<>(result);
    }
}
