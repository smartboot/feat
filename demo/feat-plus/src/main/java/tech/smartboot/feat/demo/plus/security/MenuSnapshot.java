package tech.smartboot.feat.demo.plus.security;

import java.util.List;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.cache.CacheStore;
import tech.smartboot.feat.demo.plus.mapper.MenuMapper;
import tech.smartboot.feat.demo.plus.model.MenuRecord;

/**
 * 菜单全表的读缓存。
 * <p>
 * 菜单在 {@code /api/auth/me} 与登录响应里都会读一次，而它一个月也改不了几次——和部门全表同理。
 * 入库之后这条读路径从「常量数组遍历」变成了「一条 SELECT」，缓存把它压回原来的开销。
 * <p>
 * <b>TTL 与主动失效同时存在。</b> 菜单改完必须立刻生效（改完标题刷新页面还是旧名字，用户会以为没保存），
 * 所以写操作后主动 {@link #invalidate()}；TTL 是漏调失效时的兜底。这和 {@link DepartmentSnapshot}
 * 是同一套取舍，不重复解释。
 * <p>
 * <b>失效必须在事务提交之后调。</b> 提交前清缓存，下一个请求会用提交前的数据把缓存重新填满，
 * 「立刻生效」就变成「十分钟后生效」，且只在有并发时出现，本地单线程测不出来。
 */
@Bean("menuSnapshot")
public class MenuSnapshot {

    private static final String CACHE_KEY = "menu:all";

    /** 与部门缓存同量级：正常路径有主动失效，TTL 只为兜底，宁长不短。 */
    private static final int TTL_SECONDS = 600;

    @Autowired
    private CacheStore cacheStore;

    @Autowired
    private MenuMapper menuMapper;

    /**
     * 全部菜单（未软删），已按 groupSort、sortOrder、id 排好序。
     * <p>
     * 返回的是反序列化出来的新对象，调用方可以随意修改，改不到缓存里的副本。
     */
    public List<MenuRecord> all() {
        String cached = cacheStore.get(CACHE_KEY);
        if (cached != null) {
            return JSON.parseArray(cached, MenuRecord.class);
        }
        List<MenuRecord> rows = menuMapper.selectAll();
        cacheStore.put(CACHE_KEY, JSON.toJSONString(rows), TTL_SECONDS);
        return rows;
    }

    /** 菜单表发生任何写操作后调用。 */
    public void invalidate() {
        cacheStore.evict(CACHE_KEY);
    }
}
