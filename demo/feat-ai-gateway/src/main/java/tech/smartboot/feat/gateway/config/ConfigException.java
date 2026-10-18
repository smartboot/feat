package tech.smartboot.feat.gateway.config;

/**
 * 配置解析/校验失败。消息里会带全部错误项，方便一次性改完。
 */
public class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
