package tech.smartboot.feat.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.naming.NamingService;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.Value;

import java.util.Properties;

/**
 * 服务注册示例：将当前实例注册到 Nacos，供服务消费者发现。
 */
@Controller
public class RegisterBootstrap {

    @Value("${nacos.serverAddr}")
    private String serverAddr;

    @Value("${nacos.register.serviceName}")
    private String serviceName;

    @Value("${nacos.register.group}")
    private String namingGroup;

    @Value("${nacos.register.ip}")
    private String namingIp;

    @Value("${nacos.register.port}")
    private int namingPort;

    private NamingService namingService;

    @PostConstruct
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);

        namingService = NacosFactory.createNamingService(properties);
        namingService.registerInstance(serviceName, namingGroup, namingIp, namingPort);
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (namingService != null) {
            namingService.shutDown();
            namingService = null;
        }
    }

    @RequestMapping("/nacos/register")
    public String register() {
        return "registered: " + serviceName + " -> " + namingIp + ":" + namingPort;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(opt -> opt.setPackages(RegisterBootstrap.class.getName())).listen(8082);
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
