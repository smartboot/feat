package tech.smartboot.feat.demo.plus.dw.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;

/**
 * 数据源注册表：页面配置可绑定的数据源白名单。
 * <p>
 * 无反射、无扫描——登记动作由业务侧的组合根显式调用（见 {@code service/PageDataSources}），
 * 与 Permission / MenuRoutes 的「编译期白名单」是同一套哲学：datasourceKey 只能从登记过的实现里选，
 * 配置里填错的后果是保存配置时 400，而不是渲染时静默空页。
 * <p>
 * 登记点放在业务侧而不是这里，是为了让 {@code dw} 包不反向依赖任何业务实现——
 * 平台层只定义 {@link PageDataSource} 这个契约，谁来实现由业务决定。
 */
@Bean
public class PageDataSourceRegistry {

    /** 插入顺序即配置管理页下拉的顺序，按登记顺序稳定。 */
    private final Map<String, PageDataSource> byKey = new LinkedHashMap<>();

    /**
     * 登记一个数据源。由组合根的 {@code @PostConstruct} 调用，AOT 下无并发登记。
     * <p>
     * key 重复直接启动失败：两个实现抢同一个 key，运行期谁赢取决于装配顺序，
     * 那种 bug 只会表现为「某个页面数据变了」，极难定位，不如启动就炸。
     */
    public void register(PageDataSource dataSource) {
        PageDataSource previous = byKey.put(dataSource.key(), dataSource);
        if (previous != null && previous != dataSource) {
            throw new IllegalStateException("数据源 key 重复：" + dataSource.key()
                    + "（" + previous.getClass().getSimpleName() + " 与 "
                    + dataSource.getClass().getSimpleName() + "）");
        }
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
