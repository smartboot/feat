package tech.smartboot.feat.demo.plus.dw.service;

import java.util.List;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.cache.CacheStore;
import tech.smartboot.feat.demo.plus.dw.mapper.PageMapper;
import tech.smartboot.feat.demo.plus.dw.model.PageRecord;

/**
 * 页面配置全表的读缓存，模式与 MenuSnapshot 一致：主动失效为主、TTL 只兜底，
 * 失效必须在事务提交之后调。
 * <p>
 * 读这条缓存的有两拨人：动态页面的渲染（/api/dw/pages/{pageKey}，每打开一次页面拉一次配置）
 * 和菜单校验（MenuService 校验 /p/{pageKey} 类路径时查配置存在性）。缓存原始行（含 JSON 原文），
 * canWrite 这类「每人不同」的字段绝不进缓存，由 PageService 逐请求计算。
 */
@Bean("pageSnapshot")
public class PageSnapshot {

    private static final String CACHE_KEY = "page:all";

    /** 与菜单缓存同量级：正常路径有主动失效，TTL 只为兜底，宁长不短。 */
    private static final int TTL_SECONDS = 600;

    @Autowired
    private CacheStore cacheStore;

    @Autowired
    private PageMapper pageMapper;

    /**
     * 全部页面配置（未软删），按 page_key 字典序。
     * <p>
     * 返回的是反序列化出来的新对象，调用方可以随意修改，改不到缓存里的副本。
     */
    public List<PageRecord> all() {
        String cached = cacheStore.get(CACHE_KEY);
        if (cached != null) {
            return JSON.parseArray(cached, PageRecord.class);
        }
        List<PageRecord> rows = pageMapper.selectAll();
        cacheStore.put(CACHE_KEY, JSON.toJSONString(rows), TTL_SECONDS);
        return rows;
    }

    /** 页面配置表发生任何写操作后调用。 */
    public void invalidate() {
        cacheStore.evict(CACHE_KEY);
    }
}
