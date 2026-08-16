package tech.smartboot.feat.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 服务发现示例：订阅 Nacos 中的服务实例变更，并对外暴露当前可用实例列表。
 */
@Controller
public class DiscoverBootstrap {

    @Value("${nacos.serverAddr}")
    private String serverAddr;

    @Value("${nacos.discover.serviceName}")
    private String serviceName;

    @Value("${nacos.discover.group}")
    private String namingGroup;

    private volatile List<Instance> instances = Collections.emptyList();
    private NamingService namingService;

    private final EventListener namingListener = event -> {
        if (event instanceof NamingEvent) {
            updateInstances(((NamingEvent) event).getInstances());
        }
    };

    @PostConstruct
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);

        namingService = NacosFactory.createNamingService(properties);
        namingService.subscribe(serviceName, namingGroup, namingListener);
    }

    private void updateInstances(List<Instance> changedInstances) {
        if (changedInstances == null || changedInstances.isEmpty()) {
            instances = Collections.emptyList();
        } else {
            instances = Collections.unmodifiableList(new ArrayList<>(changedInstances));
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (namingService != null) {
            namingService.unsubscribe(serviceName, namingGroup, namingListener);
            namingService.shutDown();
            namingService = null;
        }
    }

    @RequestMapping("/nacos/instances")
    public List<Instance> instances() {
        return instances;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(opt -> opt.setPackages(DiscoverBootstrap.class.getName())).listen(8083);
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setNamingGroup(String namingGroup) {
        this.namingGroup = namingGroup;
    }
}
