package tech.smartboot.feat.server.impl;

import tech.smartboot.feat.common.HeaderValue;
import tech.smartboot.feat.common.Reset;
import tech.smartboot.feat.common.enums.HttpMethodEnum;
import tech.smartboot.feat.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.common.io.BodyInputStream;
import tech.smartboot.feat.common.io.ReadListener;
import tech.smartboot.feat.common.multipart.MultipartConfig;
import tech.smartboot.feat.common.multipart.Part;
import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.PushBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

public class Http2RequestImpl extends CommonRequest implements HttpRequest, Reset {
    private static final int INIT_CONTENT_LENGTH = -2;
    private static final int NONE_CONTENT_LENGTH = -1;
    public static final int STATE_HEADER_FRAME = 0;
    public static final int STATE_DATA_FRAME = 1;
    public static final int STATE_DONE = 2;
    private int state = STATE_HEADER_FRAME;
    private final int streamId;
    private ByteArrayOutputStream body;
    private BodyInputStream bodyInputStream = BodyInputStream.EMPTY_INPUT_STREAM;
    private final Http2ResponseImpl response;
    private final Http2Session session;

    public Http2RequestImpl(int streamId, Http2Session session, boolean push) {
        super(session.getRequest().aioSession, session.getRequest().getConfiguration());
        this.streamId = streamId;
        this.session = session;
        response = new Http2ResponseImpl(streamId, this, push);
        setProtocol(HttpProtocolEnum.HTTP_2);
    }


    public Map<String, HeaderValue> getHeaders() {
        return headers;
    }

    public void checkState(int state) {
        if (this.state != state) {
            throw new IllegalStateException("state:" + state + " not equals " + this.state);
        }
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public void reset() {
        super.reset();
        method = HttpMethodEnum.GET.getMethod();
        response.reset();
    }

    @Override
    public Collection<Part> getParts(MultipartConfig configElement) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteAddr() {
        return "";
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return "";
    }

    @Override
    public BodyInputStream getInputStream() {
        return bodyInputStream;
    }

    public int getStreamId() {
        return streamId;
    }


    public ByteArrayOutputStream getBody() {
        return body;
    }

    public void setBody(ByteArrayOutputStream body) {
        this.body = body;
    }

    public void bodyDone() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body.toByteArray());
        bodyInputStream = new BodyInputStream(null) {
            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return inputStream.read(b, off, len);
            }
        };
    }

    public AbstractResponse getResponse() {
        return response;
    }

    public Http2Session getSession() {
        return session;
    }

    @Override
    public PushBuilder newPushBuilder() {
        if (session.getSettings().getEnablePush() == 0) {
            return null;
        }
        PushBuilderImpl builder = new PushBuilderImpl(streamId, response, session);
        getHeaderNames().stream().filter(headerName -> !PushBuilderImpl.IGNORE_HEADERS.contains(headerName)).forEach(headerName -> getHeaders(headerName).forEach(headerValue -> builder.addHeader(headerName, headerValue)));
        return builder;
    }
}
