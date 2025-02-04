package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
public interface Header {
    /**
     * 添加 header，支持同名追加
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    Header add(String headerName, String headerValue);

    /**
     * 添加 header，支持同名追加
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    default Header add(String headerName, int headerValue) {
        add(headerName, String.valueOf(headerValue));
        return this;
    }

    /**
     * 设置header，覆盖同名header
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    Header set(String headerName, String headerValue);

    /**
     * 设置header，覆盖同名header
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    default Header set(String headerName, int headerValue) {
        set(headerName, String.valueOf(headerValue));
        return this;
    }

    Header setContentType(String contentType);

    Header setContentLength(int contentLength);

    default Header keepalive(boolean flag) {
        return keepalive(flag ? HeaderValue.Connection.KEEPALIVE : HeaderValue.Connection.CLOSE);
    }

    default Header keepalive(String headerValue) {
        return set(HeaderNameEnum.CONNECTION.getName(), headerValue);
    }
}
