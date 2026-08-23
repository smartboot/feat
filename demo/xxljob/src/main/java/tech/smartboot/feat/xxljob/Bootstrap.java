package tech.smartboot.feat.xxljob;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSimpleExecutor;
import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;

@Bean
public class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);
    @Autowired
    private XxlJobConfig xxlJobConfig;

    @Autowired
    private IJobHandler firstJobHandler;

    @Autowired
    private IJobHandler secondJobHandler;

    private XxlJobSimpleExecutor xxlJobSpringExecutor;

    @PostConstruct
    public void init() {
        logger.info(">>>>>>>>>>> xxl-job config init.");
        xxlJobSpringExecutor = new XxlJobSimpleExecutor();
        xxlJobSpringExecutor.setAdminAddresses(xxlJobConfig.getAdminAddresses());
        xxlJobSpringExecutor.setTimeout(xxlJobConfig.getTimeout());
        xxlJobSpringExecutor.setEnabled(xxlJobConfig.isEnabled());
        xxlJobSpringExecutor.setAppname(xxlJobConfig.getAppname());
        xxlJobSpringExecutor.setAccessToken(xxlJobConfig.getAccessToken());
        xxlJobSpringExecutor.setIp(xxlJobConfig.getIp());
        xxlJobSpringExecutor.setPort(xxlJobConfig.getPort());
        xxlJobSpringExecutor.setAddress(xxlJobConfig.getAddress());
        xxlJobSpringExecutor.setLogPath(xxlJobConfig.getLogPath());
        xxlJobSpringExecutor.setLogRetentionDays(xxlJobConfig.getLogRetentionDays());
        xxlJobSpringExecutor.start();
        XxlJobExecutor.registryJobHandler("feat-first-handler", firstJobHandler);
        XxlJobExecutor.registryJobHandler("feat-second-handler", secondJobHandler);
    }

    @PreDestroy
    public void destroy() {
        xxlJobSpringExecutor.destroy();
    }

    public void setXxlJobConfig(XxlJobConfig xxlJobConfig) {
        this.xxlJobConfig = xxlJobConfig;
    }

    public void setFirstJobHandler(IJobHandler firstJobHandler) {
        this.firstJobHandler = firstJobHandler;
    }

    public void setSecondJobHandler(IJobHandler secondJobHandler) {
        this.secondJobHandler = secondJobHandler;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer().listen(8888);
    }
}
