package tech.smartboot.feat.xxljob;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSimpleExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Value;

/**
 * xxl-job config
 *
 * @author xuxueli 2017-04-28
 */
@Bean
public class XxlJobConfig {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.admin.timeout}")
    private int timeout;

    @Value("${xxl.job.executor.enabled}")
    private Boolean enabled;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.ip}")
    private String ip;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.address}")
    private String address;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Value("${xxl.job.executor.excludedpackage}")
    private String excludedPackage;

    @Value("${xxl.job.executor.glueenabled:true}")
    private Boolean glueEnabled;


    @Bean
    public XxlJobExecutor xxlJobExecutor() {
        logger.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSimpleExecutor xxlJobSpringExecutor = new XxlJobSimpleExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setTimeout(timeout);
        xxlJobSpringExecutor.setEnabled(enabled);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
//        xxlJobSpringExecutor.setExcludedPackage(excludedPackage);
//        xxlJobSpringExecutor.setGlueEnabled(glueEnabled);

        return xxlJobSpringExecutor;
    }

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public void setExcludedPackage(String excludedPackage) {
        this.excludedPackage = excludedPackage;
    }

    public void setGlueEnabled(Boolean glueEnabled) {
        this.glueEnabled = glueEnabled;
    }
}