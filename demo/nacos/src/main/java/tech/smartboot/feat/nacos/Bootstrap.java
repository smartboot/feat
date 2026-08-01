package tech.smartboot.feat.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.Value;

import java.util.Properties;
import java.util.concurrent.Executor;

@Controller
public class Bootstrap {

    @Value("${nacos.serverAddr}")
    private String serverAddr;
    private String config;

    @PostConstruct
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
        // 初始化配置中心的Nacos Java SDK
        ConfigService configService = NacosFactory.createConfigService(properties);
        config = configService.getConfig("config1", "feat", 1000);
        configService.addListener("config1", "feat", new AbstractListener() {

            @Override
            public void receiveConfigInfo(String configInfo) {
                config = configInfo;
            }
        });
    }

    @RequestMapping("/nacos")
    public String nacos() {
        return config;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer().listen();
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }
}
