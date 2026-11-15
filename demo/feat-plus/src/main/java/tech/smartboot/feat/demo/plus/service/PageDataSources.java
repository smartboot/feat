package tech.smartboot.feat.demo.plus.service;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.demo.plus.dw.service.PageDataSourceRegistry;

/**
 * 动态页面的数据源装配点（组合根）。
 * <p>
 * 白名单放在业务侧而不是 {@code PageDataSourceRegistry} 里，是为了让 {@code dw} 包只依赖契约、
 * 不依赖任何业务实现——注册表读起来是一份「有哪些数据源」的清单，不该出现 role / activity 这些名字。
 * 新增一个配置驱动页面时，改动就落在这里：写一个实现 + 在下面加一个字段和一行 register。
 * <p>
 * 为什么分成两个类而不是让数据源自己 {@code register(this)}：{@code @PostConstruct} 的执行顺序
 * 由 AOT 按全限定类名排序决定，让各实现自行登记会把「谁先注册」变成隐式依赖；
 * 集中在这里，装配顺序是这份代码的字面顺序。
 */
@Bean
public class PageDataSources {

    @Autowired
    private PageDataSourceRegistry pageDataSourceRegistry;

    // 字段名必须等于 bean 名（@Autowired 按名注入，AOT 硬约束）。
    @Autowired
    private RolePageDataSource rolePageDataSource;

    @Autowired
    private ActivityPageDataSource activityPageDataSource;

    @PostConstruct
    public void init() {
        pageDataSourceRegistry.register(rolePageDataSource);
        pageDataSourceRegistry.register(activityPageDataSource);
    }
}
