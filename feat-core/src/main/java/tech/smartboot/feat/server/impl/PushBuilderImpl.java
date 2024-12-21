package tech.smartboot.feat.server.impl;

import tech.smartboot.feat.common.codec.h2.codec.ContinuationFrame;
import tech.smartboot.feat.common.codec.h2.codec.Http2Frame;
import tech.smartboot.feat.common.codec.h2.codec.PushPromiseFrame;
import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HttpMethodEnum;
import tech.smartboot.feat.common.utils.HttpUtils;
import tech.smartboot.feat.common.utils.StringUtils;
import tech.smartboot.feat.server.PushBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PushBuilderImpl implements PushBuilder {
    public static final List<String> IGNORE_HEADERS = Arrays.asList("if-match", "if-none-match", "if-modified-since",
            "if-unmodified-since", "if-range", "range", "proxy-authorization", "from", "user-agent", "range", "expect"
            , "max-forwards", "proxy-authenticate", "proxy-authorization", "age", "cache-control", "clear-site-data");
    private static final Set<String> UNSUPPORTED_METHODS = new HashSet<>(Arrays.asList("", "POST", "PUT", "DELETE",
            "CONNECT", "OPTIONS", "TRACE"));
    private final Http2RequestImpl pushRequest;
    private final int streamId;
    private String path;
    private String queryString;


    public PushBuilderImpl(int streamId, Http2ResponseImpl response, Http2Session session) {
        this.streamId = streamId;
        this.pushRequest = new Http2RequestImpl(session.getPushStreamId().addAndGet(2), session, true);
        response.getCookies().forEach(cookie -> pushRequest.addHeader(HeaderNameEnum.COOKIE.getLowCaseName(),
                HeaderNameEnum.COOKIE.getName(), cookie.getName() + "=" + cookie.getValue()));

        method(HttpMethodEnum.GET.getMethod());
    }

    @Override
    public PushBuilder method(String method) {
        if (method == null) {
            throw new NullPointerException();
        }
        if (UNSUPPORTED_METHODS.contains(method)) {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }
        pushRequest.setMethod(method);
        return this;
    }

    @Override
    public PushBuilder queryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    @Override
    public PushBuilder setHeader(String name, String value) {
        pushRequest.setHeader(name, value);
        return this;
    }

    @Override
    public PushBuilder addHeader(String name, String value) {
        pushRequest.addHeader(name.toLowerCase(), name, value);
        return this;
    }

    @Override
    public PushBuilder removeHeader(String name) {
        pushRequest.setHeader(name, null);
        return null;
    }

    @Override
    public PushBuilder path(String path) {
        this.path = path;
        return this;
    }

    @Override
    public void push() {
//        Executors.callable(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                pushRequest.getSession().getRequest().getConfiguration().getHttp2ServerHandler().handleHttpRequest
//                (pushRequest);
//            }
//        });
        if (StringUtils.isBlank(path)) {
            throw new IllegalStateException();
        }
        String path = this.path;
        if (queryString != null && !queryString.isEmpty()) {
            if (path.contains("?")) {
                path += "&" + queryString;
            } else {
                path += "?" + queryString;
            }
        }
        try {
            pushRequest.setHeader(":method", pushRequest.getMethod());
            pushRequest.setHeader(":scheme", pushRequest.getScheme());
            pushRequest.setHeader(":path", path);
            pushRequest.setHeader(":authority", pushRequest.getSession().getRequest().getHost());
            pushRequest.setUri(path);
            pushRequest.setRequestURI(path);
            List<ByteBuffer> buffers = HttpUtils.HPackEncoder(pushRequest.getSession().getHpackEncoder(),
                    pushRequest.getHeaders());
            PushPromiseFrame frame = new PushPromiseFrame(streamId, buffers.size() > 1 ? 0 :
                    Http2Frame.FLAG_END_HEADERS, 0);
            frame.setPromisedStream(pushRequest.getStreamId());
            if (!buffers.isEmpty()) {
                frame.setFragment(buffers.get(0));
            }
            frame.writeTo(pushRequest.getSession().getRequest().aioSession.writeBuffer());
            for (int i = 1; i < buffers.size() - 1; i++) {
                ContinuationFrame continuationFrame = new ContinuationFrame(streamId, 0, 0);
                continuationFrame.setFragment(buffers.get(i));
                continuationFrame.writeTo(pushRequest.getSession().getRequest().aioSession.writeBuffer());
            }
            if (buffers.size() > 1) {
                ContinuationFrame continuationFrame = new ContinuationFrame(streamId, Http2Frame.FLAG_END_HEADERS, 0);
                continuationFrame.setFragment(buffers.get(buffers.size() - 1));
                continuationFrame.writeTo(pushRequest.getSession().getRequest().aioSession.writeBuffer());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pushRequest.getSession().getRequest().getConfiguration().getHttp2ServerHandler().handleHttpRequest(pushRequest);
        pushRequest.reset();
        this.path = null;
        this.queryString = null;
//        pushRequest.getSession().getRequest().aioSession.writeBuffer().flush();
    }

    @Override
    public String getMethod() {
        return pushRequest.getMethod();
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public Set<String> getHeaderNames() {
        return new HashSet<>(pushRequest.getHeaderNames());
    }

    @Override
    public String getHeader(String name) {
        return pushRequest.getHeader(name);
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
