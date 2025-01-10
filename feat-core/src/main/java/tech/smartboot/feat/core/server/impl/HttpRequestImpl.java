/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpRequestImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.io.BodyInputStream;
import tech.smartboot.feat.core.common.multipart.MultipartConfig;
import tech.smartboot.feat.core.common.multipart.Part;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public class HttpRequestImpl extends AbstractRequest {
    /**
     * 释放维持长连接
     */
    private boolean keepAlive;
    private List<Part> parts;
    private boolean multipartParsed;

    private final HttpResponseImpl response;

    HttpRequestImpl(Request request) {
        super(request);
        this.response = new HttpResponseImpl(this);
    }

    public final AbstractResponse getResponse() {
        return response;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    @Override
    public BodyInputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    @Override
    public Map<String, String> getTrailerFields() {
        return request.getTrailerFields() == null ? super.getTrailerFields() : request.getTrailerFields();
    }

    @Override
    public boolean isTrailerFieldsReady() {
        return !HeaderValueEnum.TransferEncoding.CHUNKED.equals(getHeader(HeaderNameEnum.TRANSFER_ENCODING)) || request.getTrailerFields() != null;
    }

    @Override
    public void upgrade(HttpUpgradeHandler upgradeHandler) throws IOException {
        request.setUpgradeHandler(upgradeHandler);
        upgradeHandler.setRequest(request);
        upgradeHandler.init();
        upgradeHandler.onBodyStream(request.getAioSession().readBuffer());
    }

    public void reset() {
        request.reset();
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

    public Collection<Part> getParts(MultipartConfig configElement) throws IOException {
        if (!multipartParsed) {
            MultipartFormDecoder multipartFormDecoder = new MultipartFormDecoder(this, configElement);
            long remaining = getContentLength();
            if (configElement.getMaxRequestSize() > 0 && configElement.getMaxRequestSize() < remaining) {
                throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
            }
            int p = request.getAioSession().readBuffer().position();
            while (!multipartFormDecoder.decode(request.getAioSession().readBuffer(), this)) {
                remaining -= request.getAioSession().readBuffer().position() - p;
                int readSize = request.getAioSession().read();
                p = request.getAioSession().readBuffer().position();
                if (readSize == -1) {
                    break;
                }
            }
            multipartParsed = true;
            request.setInputStream(BodyInputStream.EMPTY_INPUT_STREAM);
            remaining -= request.getAioSession().readBuffer().position() - p;
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

}
