package tech.smartboot.feat.demo.plus.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.demo.plus.common.DomainException;

/**
 * 数据源注册表：页面配置可绑定的数据源白名单。
 * <p>
 * 无反射、无运行时注册——每个数据源是一个 {@code @Bean}，这里按字段名注入并逐一登记。
 * 新增数据源 = 写一个 @Bean 实现类 + 这里加一个 @Autowired 字段和一行 register，
 * 与 Permission / MenuRoutes 的「编译期白名单」是同一套哲学。datasourceKey 只能从这里选，
 * 配置里填错的后果是保存配置时 400，而不是渲染时静默空页。
 */
@Bean
public class PageDataSourceRegistry {

    @Autowired
    private RolePageDataSource rolePageDataSource;

    // 字段名必须等于 bean 名（@Autowired 按名注入，AOT 硬约束）。
    @Autowired
    private ActivityPageDataSource activityPageDataSource;

    private final Map<String, PageDataSource> byKey = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        register(rolePageDataSource);
        register(activityPageDataSource);
    }

    private void register(PageDataSource dataSource) {
        byKey.put(dataSource.key(), dataSource);
    }

    /** 按 key 取数据源，找不到即 404——key 来自已校验的配置行或配置管理页的保存请求。 */
    public PageDataSource require(String key) {
        PageDataSource dataSource = byKey.get(key);
        if (dataSource == null) {
            throw DomainException.notFound("数据源不存在：" + key);
        }
        return dataSource;
    }

    /** 全部数据源，配置管理页的下拉与字段提示用。 */
    public Collection<PageDataSource> all() {
        return byKey.values();
    }
}
