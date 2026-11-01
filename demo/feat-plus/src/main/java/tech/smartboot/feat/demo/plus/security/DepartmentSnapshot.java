package tech.smartboot.feat.demo.plus.security;

import java.util.List;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.cache.CacheStore;
import tech.smartboot.feat.demo.plus.mapper.DepartmentMapper;
import tech.smartboot.feat.demo.plus.model.DepartmentRecord;

/**
 * 部门全表的读缓存。
 * <p>
 * 存在的理由是调用频次：数据范围解析每请求都要把部门表整表读出来算子树（见 {@link DataScopeResolver}），
 * 而部门数据一个月改不了几次。缓存后把这条固定开销从「每请求一次全表」压成「十分钟一次全表」。
 * <p>
 * 为什么放在 security 而不是 service：{@link DataScopeResolver} 要用它，而机制层不能依赖业务层
 * （{@code service} 反过来依赖 {@code security}，反向引用会成环）。
 * <p>
 * <b>TTL 和主动失效同时存在，不是二选一。</b>主动失效保证改完立刻生效，TTL 是漏失效时的兜底——
 * 少了 TTL，一次漏调就会永久读到旧部门树；少了主动失效，改完要等十分钟才生效，期间的数据范围是错的。
 * <p>
 * 只给读路径用。写路径（{@code DepartmentService.create / requireByName / assertNoCycle}）必须直读
 * mapper：写方法内部的读如果命中旧缓存，会看不到自己刚插入的行，或者拿已删除的部门当有效数据，
 * 后者的后果是权限范围被悄悄放宽。
 */
@Bean("departmentSnapshot")
public class DepartmentSnapshot {

    /**
     * 键名不带用户维度，因为缓存的是「全表」这一个整体。
     */
    private static final String CACHE_KEY = "dept:all";

    /**
     * 十分钟。取值依据是「改完到生效的最坏延迟」：正常路径有主动失效，TTL 只在漏失效时起作用，
     * 所以宁可长一点，减少整表重扫的次数。
     */
    private static final int TTL_SECONDS = 600;

    @Autowired
    private CacheStore cacheStore;

    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 全部部门（未软删）。返回的是反序列化出来的新对象，调用方可以随意修改，
     * 改不到缓存里的副本——这点和「缓存一个 List 直接返回」不同，省掉了一整类串改问题。
     */
    public List<DepartmentRecord> all() {
        String cached = cacheStore.get(CACHE_KEY);
        if (cached != null) {
            return JSON.parseArray(cached, DepartmentRecord.class);
        }
        List<DepartmentRecord> rows = departmentMapper.selectAll();
        cacheStore.put(CACHE_KEY, JSON.toJSONString(rows), TTL_SECONDS);
        return rows;
    }

    /**
     * 部门表发生任何写操作后调用。
     */
    public void invalidate() {
        cacheStore.evict(CACHE_KEY);
    }
}
