package tech.smartboot.feat.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
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

@Controller
public class NamingBootstrap {

    @Value("${nacos.serverAddr}")
    private String serverAddr;

    @Value("${nacos.naming.serviceName}")
    private String serviceName;

    @Value("${nacos.naming.group}")
    private String namingGroup;

    @Value("${nacos.naming.ip}")
    private String namingIp;

    @Value("${nacos.naming.port}")
    private int namingPort;

    private volatile List<Instance> instances = Collections.emptyList();
    private NamingService namingService;
    private boolean registered;
    private boolean subscribed;

    private final EventListener namingListener = new EventListener() {
        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                updateInstances(((NamingEvent) event).getInstances());
            }
        }
    };

    @PostConstruct
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);

        try {
            namingService = NacosFactory.createNamingService(properties);
            namingService.registerInstance(serviceName, namingGroup, namingIp, namingPort);
            registered = true;
            namingService.subscribe(serviceName, namingGroup, namingListener);
            subscribed = true;
            updateInstances(namingService.selectInstances(serviceName, namingGroup, true, false));
        } catch (Exception e) {
            destroy();
            throw e;
        }
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
        Exception failure = null;
        if (namingService != null) {
            if (subscribed) {
                try {
                    namingService.unsubscribe(serviceName, namingGroup, namingListener);
                } catch (Exception e) {
                    failure = e;
                }
                subscribed = false;
            }
            if (registered) {
                try {
                    namingService.deregisterInstance(serviceName, namingGroup, namingIp, namingPort);
                } catch (Exception e) {
                    if (failure == null) {
                        failure = e;
                    }
                }
                registered = false;
            }
            try {
                namingService.shutDown();
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                }
            }
            namingService = null;
        }
        if (failure != null) {
            throw failure;
        }
    }

    @RequestMapping("/nacos/instances")
    public List<Instance> instances() {
        return instances;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(opt -> opt.setPackages(NamingBootstrap.class.getName())).listen();
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

    public void setNamingIp(String namingIp) {
        this.namingIp = namingIp;
    }

    public void setNamingPort(int namingPort) {
        this.namingPort = namingPort;
    }
}
