/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.DateUtils;
import tech.smartboot.feat.core.common.utils.Mimetypes;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
class StaticResourceHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticResourceHandler.class);
    private final Date lastModifyDate = new Date(System.currentTimeMillis() / 1000 * 1000);

    private final String lastModifyDateFormat = DateUtils.formatRFC1123(lastModifyDate);

    private final ExecutorService asyncExecutor;

    public StaticResourceHandler() {
        this(null);
    }

    public StaticResourceHandler(ExecutorService asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
        if (asyncExecutor == null) {
            try {
                handle0(request, completableFuture);
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
            }
        } else {
            asyncExecutor.execute(() -> {
                try {
                    handle0(request, completableFuture);
                } catch (IOException e) {
                    completableFuture.completeExceptionally(e);
                }
            });
        }
    }

    @Override
    public void handle(HttpRequest request) throws Throwable {
        throw new UnsupportedOperationException();
    }

    public void handle0(HttpRequest request, CompletableFuture<Object> completableFuture) throws IOException {
        HttpResponse response = request.getResponse();
        String fileName = request.getRequestURI();

        if (StringUtils.endsWith(fileName, "/")) {
            fileName += "index.html";
        }

        //304
        try {
            String requestModified = request.getHeader(HeaderNameEnum.IF_MODIFIED_SINCE.getName());
            if (StringUtils.isNotBlank(requestModified) && lastModifyDate.getTime() <= DateUtils.parseRFC1123(requestModified).getTime()) {
                response.setHttpStatus(HttpStatus.NOT_MODIFIED);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("exception", e);
        }
        response.setHeader(HeaderNameEnum.LAST_MODIFIED.getName(), lastModifyDateFormat);

        try (InputStream inputStream = StaticResourceHandler.class.getClassLoader().getResourceAsStream("static" + fileName)) {
            if (inputStream == null) {
                response.setHttpStatus(HttpStatus.NOT_FOUND);
                return;
            }
            String contentType = Mimetypes.getInstance().getMimetype(fileName);
            response.setHeader(HeaderNameEnum.CONTENT_TYPE.getName(), contentType + "; charset=utf-8");

            byte[] bytes = new byte[1024];
            int length;
            if ((length = inputStream.read(bytes)) > 0) {
                response.getOutputStream().write(bytes, 0, length, new Consumer<FeatOutputStream>() {

                    @Override
                    public void accept(FeatOutputStream featOutputStream) {
                        int length;
                        try {
                            if ((length = inputStream.read(bytes)) > 0) {
                                response.getOutputStream().write(bytes, 0, length, this);
                            } else {
                                completableFuture.complete(null);
                            }
                        } catch (Throwable throwable) {
                            completableFuture.completeExceptionally(throwable);
                        }
                    }
                });
            }
        }
    }


}
