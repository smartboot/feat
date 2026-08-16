package tech.smartboot.feat.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.Value;

import java.util.Properties;

@Controller
public class ConfigBootstrap {

    @Value("${nacos.serverAddr}")
    private String serverAddr;

    @Value("${nacos.config.dataId}")
    private String configDataId;

    @Value("${nacos.config.group}")
    private String configGroup;

    @Value("${nacos.config.timeout}")
    private int configTimeout;

    private volatile String config = "";
    private ConfigService configService;

    private final AbstractListener configListener = new AbstractListener() {
        @Override
        public void receiveConfigInfo(String configInfo) {
            config = configInfo == null ? "" : configInfo;
        }
    };

    @PostConstruct
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);

        try {
            configService = NacosFactory.createConfigService(properties);
            String initialConfig = configService.getConfigAndSignListener(
                    configDataId,
                    configGroup,
                    configTimeout,
                    configListener
            );
            config = initialConfig == null ? "" : initialConfig;
        } catch (Exception e) {
            destroy();
            throw e;
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (configService == null) {
            return;
        }
        configService.removeListener(configDataId, configGroup, configListener);
        configService.shutDown();
        configService = null;
    }

    @RequestMapping("/nacos")
    public String nacos() {
        return config;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(opt -> opt.setPackages(ConfigBootstrap.class.getName())).listen(8081);
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public void setConfigDataId(String configDataId) {
        this.configDataId = configDataId;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public void setConfigTimeout(int configTimeout) {
        this.configTimeout = configTimeout;
    }
}
