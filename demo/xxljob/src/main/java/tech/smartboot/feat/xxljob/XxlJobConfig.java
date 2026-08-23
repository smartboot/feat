package tech.smartboot.feat.xxljob;

import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Value;

/**
 * xxl-job config
 *
 * @author xuxueli 2017-04-28
 */
@Bean
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.admin.timeout}")
    private int timeout;

    @Value("${xxl.job.executor.enabled}")
    private boolean enabled;

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


    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setEnabled(boolean enabled) {
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

    public String getAdminAddresses() {
        return adminAddresses;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getAppname() {
        return appname;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    public String getLogPath() {
        return logPath;
    }

    public int getLogRetentionDays() {
        return logRetentionDays;
    }
}