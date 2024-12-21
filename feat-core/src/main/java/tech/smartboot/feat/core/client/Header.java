package tech.smartboot.feat.core.client;

import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
public interface Header<T> {
    /**
     * 添加 header，支持同名追加
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    Header<T> add(String headerName, String headerValue);

    /**
     * 添加 header，支持同名追加
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    default Header<T> add(String headerName, int headerValue) {
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
    Header<T> set(String headerName, String headerValue);

    /**
     * 设置header，覆盖同名header
     *
     * @param headerName  header名
     * @param headerValue header值
     * @return 当前Header对象
     */
    default Header<T> set(String headerName, int headerValue) {
        set(headerName, String.valueOf(headerValue));
        return this;
    }

    Header<T> setContentType(String contentType);

    Header<T> setContentLength(int contentLength);

    default Header<T> keepalive(boolean flag) {
        return keepalive(flag ? HeaderValueEnum.KEEPALIVE.getName() : HeaderValueEnum.CLOSE.getName());
    }

    default Header<T> keepalive(String headerValue) {
        return set(HeaderNameEnum.CONNECTION.getName(), headerValue);
    }

    /**
     * 结束header设置
     *
     * @return header归属的HTTP请求主体
     */
    T done();
}
