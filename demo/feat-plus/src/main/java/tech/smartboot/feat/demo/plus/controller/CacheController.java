package tech.smartboot.feat.demo.plus.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.cache.CacheStats;
import tech.smartboot.feat.demo.plus.cache.CacheStore;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.router.Context;

/**
 * 缓存运行状态。
 * <p>
 * 只读。存在的意义是回答一个运维问题：<b>「现在到底在用哪个后端」</b>。
 * 这个信息无法从配置推断——配置是编译期固化的，部署包里可能还留着上一套配置，
 * 而「配了 Redis 却一直是内存在跑」只会表现为「偶发读到旧数据」，没有任何报错。
 * <p>
 * 用 {@code system:write} 而不是新开一个只读权限点：新增权限点要同步改 Java 常量、
 * 前端权限字典、种子角色三处，漏一处就会出现「角色页看不到这个权限」。
 * 为一条运维查询付这个代价不划算。
 */
@Controller("api/system/cache")
public class CacheController extends ApiController {

    @Autowired
    private CacheStore cacheStore;

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 当前后端与本次启动以来的累计计数。多实例部署时每个实例各算各的。
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<CacheStats> stats(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.SYSTEM_WRITE);
            return cacheStore.stats();
        });
    }
}
