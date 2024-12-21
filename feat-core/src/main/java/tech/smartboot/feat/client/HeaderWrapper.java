package tech.smartboot.feat.client;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
class HeaderWrapper<T extends HttpRest> implements Header<T> {
    private final T rest;

    private final Header<? extends HttpRest> header;

    public HeaderWrapper(T rest, Header<? extends HttpRest> header) {
        this.rest = rest;
        this.header = header;
    }

    @Override
    public Header<T> add(String headerName, String headerValue) {
        header.add(headerName, headerValue);
        return this;
    }

    @Override
    public Header<T> set(String headerName, String headerValue) {
        header.set(headerName, headerValue);
        return this;
    }

    @Override
    public Header<T> setContentType(String contentType) {
        header.setContentType(contentType);
        return this;
    }

    @Override
    public Header<T> setContentLength(int contentLength) {
        header.setContentLength(contentLength);
        return this;
    }

    @Override
    public T done() {
        return rest;
    }
}
