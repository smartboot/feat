package tech.smartboot.feat.demo.plus.cache;

/**
 * 缓存后端的最小契约。
 * <p>
 * 值统一是 {@code String}，不是 {@code Object}：
 * <ul>
 *   <li>Redis 只能存字节，内存实现若直接放对象，两边行为就不一致——同一段业务代码
 *       在开发环境（内存）正常、换到生产（Redis）就开始报序列化错，是最难查的一类问题；</li>
 *   <li>把序列化交给调用方（项目里统一用 fastjson2），缓存层就不必引入额外的序列化契约，
 *       也不必关心字段增减；</li>
 *   <li>键值都是字符串，{@code redis-cli} 能直接看懂现场，排障不用先反序列化。</li>
 * </ul>
 * <p>
 * 只提供单键失效（{@link #evict(String)}），不提供按前缀批量清除：Redis 侧没有 SCAN/KEYS，
 * 想支持就得在框架里维护一份键索引，而索引本身会失同步。需要成组失效时，用「键里带版本号」
 * 的做法——版本加一，旧键自然过期，不需要真的去删。
 */
public interface CacheStore {

    /**
     * 读缓存。未命中或已过期返回 {@code null}。
     */
    String get(String key);

    /**
     * 写缓存。{@code ttlSeconds <= 0} 时使用配置的默认有效期。
     */
    void put(String key, String value, int ttlSeconds);

    /**
     * 清掉一个键，返回是否真的存在并删除了。
     */
    boolean evict(String key);

    /**
     * 当前实例的运行计数。多实例部署时每个实例各算各的，不是全局值。
     */
    CacheStats stats();

    /**
     * 实际生效的后端名，用于启动日志与运维页展示——「以为在用 Redis，其实一直是内存」这类问题
     * 只能靠它发现。
     */
    String providerName();
}
