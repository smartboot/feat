/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: Request.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioSession;
import tech.smartboot.feat.core.common.DecodeState;
import tech.smartboot.feat.core.common.Reset;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.common.io.ChunkedInputStream;
import tech.smartboot.feat.core.common.io.PostInputStream;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.multipart.MultipartConfig;
import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public final class HttpEndpoint extends Endpoint implements HttpRequest, Reset {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpEndpoint.class);

    private final DecoderUnit decodeState = new DecoderUnit();
    private BaseHttpHandler serverHandler;
    /**
     * 释放维持长连接
     */
    private boolean keepAlive;
    private List<Part> parts;
    private boolean multipartParsed;

    private final HttpResponseImpl response;
    /**
     * 剩余可读字节数
     */
    private long remainingThreshold;
    private Upgrade upgradeHandler;

    private TimerTask httpIdleTask;

    private BodyInputStream inputStream;
    private Map<String, String> trailerFields;
    private int state;

    public int getState() {
        return state;
    }


    public void setState(int flag) {
        this.state = this.state | flag;
    }

    void cancelHttpIdleTask() {
        synchronized (this) {
            if (httpIdleTask != null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("cancel http idle monitor, request:{}", this);
                }
                httpIdleTask.cancel();
                httpIdleTask = null;
            }
        }
    }

    HttpEndpoint(ServerOptions configuration, AioSession aioSession) {
        super(aioSession, configuration);
        this.remainingThreshold = configuration.getMaxRequestSize();
        this.response = new HttpResponseImpl(this);
        if (configuration.getHttpIdleTimeout() > 0) {
            httpIdleTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                LOGGER.debug("check httpIdle monitor");
                if (System.currentTimeMillis() - latestIo > configuration.getHttpIdleTimeout()) {
                    LOGGER.debug("close http connection by idle monitor");
                    try {
                        aioSession.close();
                    } finally {
                        cancelHttpIdleTask();
                    }
                }
            }, configuration.getHttpIdleTimeout(), TimeUnit.MILLISECONDS);
        }
    }

    public void setInputStream(BodyInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public AbstractResponse getResponse() {
        return response;
    }


    @Override
    public BodyInputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        if (getHeader(HeaderNameEnum.UPGRADE) != null) {
            inputStream = new PostInputStream(aioSession, Long.MAX_VALUE, Long.MAX_VALUE);
        }
        //如果一个消息即存在传输译码（Transfer-Encoding）头域并且也 Content-Length 头域，后者会被忽略。
        else if (HeaderValueEnum.TransferEncoding.CHUNKED.equalsIgnoreCase(getHeader(HeaderNameEnum.TRANSFER_ENCODING))) {
            inputStream = new ChunkedInputStream(aioSession, remainingThreshold, stringStringMap -> this.trailerFields = stringStringMap);
        } else {
            long contentLength = getContentLength();
            if (contentLength > 0) {
                inputStream = new PostInputStream(aioSession, contentLength, remainingThreshold);
            } else {
                inputStream = BodyInputStream.EMPTY_INPUT_STREAM;
            }
        }
        return inputStream;
    }


    void decodeSize(int size) {
        remainingThreshold -= size;
        if (remainingThreshold < 0) {
            throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    public BaseHttpHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(BaseHttpHandler serverHandler) {
        this.serverHandler = serverHandler;
    }


    /**
     * Returns the Internet Protocol (IP) address of the client
     * or last proxy that sent the request.
     * For HTTP servlets, same as the value of the
     * CGI variable <code>REMOTE_ADDR</code>.
     *
     * @return a <code>String</code> containing the
     * IP address of the client that sent the request
     */

    public String getRemoteAddr() {
        if (remoteAddr != null) {
            return remoteAddr;
        }
        try {
            InetSocketAddress remote = aioSession.getRemoteAddress();
            InetAddress address = remote.getAddress();
            if (address == null) {
                remoteAddr = remote.getHostString();
            } else {
                remoteAddr = address.getHostAddress();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return remoteAddr;
    }


    public InetSocketAddress getRemoteAddress() {
        try {
            return aioSession.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public InetSocketAddress getLocalAddress() {
        try {
            return aioSession.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the fully qualified name of the client
     * or the last proxy that sent the request.
     * If the engine cannot or chooses not to resolve the hostname
     * (to improve performance), this method returns the dotted-string form of
     * the IP address. For HTTP servlets, same as the value of the CGI variable
     * <code>REMOTE_HOST</code>.
     *
     * @return a <code>String</code> containing the fully
     * qualified name of the client
     */

    public String getRemoteHost() {
        if (remoteHost != null) {
            return remoteHost;
        }
        try {
            remoteHost = aioSession.getRemoteAddress().getHostString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return remoteHost;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    @Override
    public Map<String, String> getTrailerFields() {
        return trailerFields == null ? Collections.emptyMap() : trailerFields;
    }

    @Override
    public boolean isTrailerFieldsReady() {
        return !HeaderValueEnum.TransferEncoding.CHUNKED.equals(getHeader(HeaderNameEnum.TRANSFER_ENCODING)) || trailerFields != null;
    }


    public DecoderUnit getDecodeState() {
        return decodeState;
    }

    public Upgrade getUpgradeHandler() {
        return upgradeHandler;
    }

    public void setUpgradeHandler(Upgrade upgradeHandler) {
        this.upgradeHandler = upgradeHandler;
    }

    @Override
    public void upgrade(Upgrade upgrade) throws IOException {
        setUpgradeHandler(upgrade);
        response.getOutputStream().disableChunked();
        //升级后取消http空闲监听
        cancelHttpIdleTask();
        upgrade.setRequest(this);
        upgrade.init(this, response);
        upgrade.onBodyStream(this.getAioSession().readBuffer());

    }

    public Collection<Part> getParts(MultipartConfig configElement) throws IOException {
        if (!multipartParsed) {
            MultipartFormDecoder multipartFormDecoder = new MultipartFormDecoder(this, configElement);
            long remaining = getContentLength();
            if (configElement.getMaxRequestSize() > 0 && configElement.getMaxRequestSize() < remaining) {
                throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
            }
            int p = aioSession.readBuffer().position();
            while (!multipartFormDecoder.decode(aioSession.readBuffer(), this)) {
                remaining -= aioSession.readBuffer().position() - p;
                int readSize = aioSession.read();
                p = aioSession.readBuffer().position();
                if (readSize == -1) {
                    break;
                }
            }
            multipartParsed = true;
            setInputStream(BodyInputStream.EMPTY_INPUT_STREAM);
            remaining -= aioSession.readBuffer().position() - p;
            if (remaining != 0) {
                throw new HttpException(HttpStatus.BAD_REQUEST);
            }
        }
        if (parts == null) {
            parts = new ArrayList<>();
        }
        return parts;
    }

    public void setPart(Part part) {
        if (parts == null) {
            parts = new ArrayList<>();
        }
        this.parts.add(part);
    }

    public void reset() {
        super.reset();
        serverHandler = null;
        remainingThreshold = configuration.getMaxRequestSize();
        method = null;
        decodeState.setState(DecodeState.STATE_METHOD);
        trailerFields = null;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
        response.reset();

        if (parts != null) {
            for (Part part : parts) {
                try {
                    part.delete();
                } catch (IOException ignore) {
                }
            }
            parts = null;
        }
        multipartParsed = false;
    }
}
