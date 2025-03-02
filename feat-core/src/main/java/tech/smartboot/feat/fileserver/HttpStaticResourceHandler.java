/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: StaticResourceHandle.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.fileserver;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.HttpMethod;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.exception.HttpException;
import tech.smartboot.feat.core.common.io.FeatOutputStream;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.DateUtils;
import tech.smartboot.feat.core.common.utils.Mimetypes;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 静态资源加载Handle
 *
 * @author 三刀
 * @version V1.0 , 2018/2/7
 */
public class HttpStaticResourceHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpStaticResourceHandler.class);

    private final File baseDir;
    private final FileServerOptions options;

    public HttpStaticResourceHandler() {
        this(new FileServerOptions().baseDir("./"));
    }

    public HttpStaticResourceHandler(FileServerOptions options) {
        try {
            this.options = options;
            this.baseDir = new File(options.baseDir()).getCanonicalFile();
        } catch (IOException e) {
            throw new FeatException(e);
        }
        if (!this.baseDir.isDirectory()) {
            throw new RuntimeException(baseDir + " is not a directory");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("dir is:{}", this.baseDir.getAbsolutePath());
        }
    }

    @Override
    public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws IOException {
        HttpResponse response = request.getResponse();
        String fileName = request.getRequestURI();
        File file = new File(baseDir, URLDecoder.decode(fileName, StandardCharsets.UTF_8.name()));

        //404
        if (file.isDirectory()) {
            if (options.autoIndex()) {
                String path = file.getParentFile().getAbsolutePath().substring(baseDir.getAbsolutePath().length());
                if (StringUtils.length(path) <= 1) {
                    path = "/";
                }
                response.write("返回上一级：<a href='" + path + "'>&gt;" + path + "</a>");
                response.write("<ul>");
                for (File f : Objects.requireNonNull(file.listFiles(File::isDirectory))) {
                    if (request.getRequestURI().endsWith("/")) {
                        request.getResponse().write("<li><a href='" + request.getRequestURI() + f.getName() + "'>&gt;\uD83D\uDCC1 &nbsp;" + f.getName() + "</a></li>");
                    } else {
                        request.getResponse().write("<li><a href='" + request.getRequestURI() + "/" + f.getName() + "'>&gt;\uD83D\uDCC1 &nbsp;" + f.getName() + "</a></li>");
                    }

                }
                for (File f : Objects.requireNonNull(file.listFiles(File::isFile))) {
                    if (request.getRequestURI().endsWith("/")) {
                        request.getResponse().write("<li><a href='" + request.getRequestURI() + f.getName() + "'>" + f.getName() + "</a></li>");
                    } else {
                        request.getResponse().write("<li><a href='" + request.getRequestURI() + "/" + f.getName() + "'>" + f.getName() + "</a></li>");
                    }

                }
                response.write("</ul>");
                completableFuture.complete(null);
                return;
            } else {
                file = new File(file, "index.html");
            }
        }
        if (!file.isFile()) {
            fileNotFound(request, response);
            completableFuture.complete(null);
            return;
        }
        //304
        Date lastModifyDate = new Date(file.lastModified() / 1000 * 1000);
        try {
            String requestModified = request.getHeader(HeaderNameEnum.IF_MODIFIED_SINCE.getName());
            if (StringUtils.isNotBlank(requestModified) && lastModifyDate.getTime() <= DateUtils.parseRFC1123(requestModified).getTime()) {
                response.setHttpStatus(HttpStatus.NOT_MODIFIED);
                completableFuture.complete(null);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("exception", e);
        }

        response.setHeader(HeaderNameEnum.LAST_MODIFIED.getName(), DateUtils.formatRFC1123(lastModifyDate));
        response.setHeader(HeaderNameEnum.CONTENT_TYPE.getName(), Mimetypes.getInstance().getMimetype(file) + "; charset=utf-8");
        //HEAD不输出内容
        if (HttpMethod.HEAD.equals(request.getMethod())) {
            completableFuture.complete(null);
            return;
        }

        response.setContentLength((int) file.length());

        FileInputStream fis = new FileInputStream(file);
        completableFuture.whenComplete((o, throwable) -> {
            try {
                fis.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        long fileSize = response.getContentLength();
        AtomicLong readPos = new AtomicLong(0);
        FileChannel fileChannel = fis.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int len;
        len = fileChannel.read(buffer);
        buffer.flip();
        if (len == -1) {
            completableFuture.completeExceptionally(new IOException("EOF"));
        } else if (readPos.addAndGet(len) >= fileSize) {
            response.getOutputStream().transferFrom(buffer, bufferOutputStream -> completableFuture.complete(null));

        } else {
            response.getOutputStream().transferFrom(buffer, new Consumer<FeatOutputStream>() {
                @Override
                public void accept(FeatOutputStream result) {
                    try {
                        buffer.compact();
                        int len = fileChannel.read(buffer);
                        buffer.flip();
                        if (len == -1) {
                            completableFuture.completeExceptionally(new IOException("EOF"));
                        } else if (readPos.addAndGet(len) >= fileSize) {
                            response.getOutputStream().transferFrom(buffer, bufferOutputStream -> completableFuture.complete(null));

                        } else {
                            response.getOutputStream().transferFrom(buffer, this);
                        }
                    } catch (Throwable throwable) {
                        completableFuture.completeExceptionally(throwable);
                    }
                }
            });
        }
    }

    @Override
    public void handle(HttpRequest request) throws Throwable {
        throw new UnsupportedOperationException();
    }

    private void fileNotFound(HttpRequest request, HttpResponse response) throws IOException {
        if (request.getRequestURI().equals("/favicon.ico")) {
            try (InputStream inputStream = HttpStaticResourceHandler.class.getClassLoader().getResourceAsStream("favicon.ico")) {
                if (inputStream == null) {
                    response.setHttpStatus(HttpStatus.NOT_FOUND);
                    return;
                }
                String contentType = Mimetypes.getInstance().getMimetype("favicon.ico");
                response.setHeader(HeaderNameEnum.CONTENT_TYPE.getName(), contentType + "; charset=utf-8");
                byte[] bytes = new byte[4094];
                int length;
                while ((length = inputStream.read(bytes)) != -1) {
                    response.getOutputStream().write(bytes, 0, length);
                }
            }
            return;
        }
        LOGGER.warn("file: {} not found!", request.getRequestURI());
        response.setHttpStatus(HttpStatus.NOT_FOUND);
        response.setHeader(HeaderNameEnum.CONTENT_TYPE.getName(), "text/html; charset=utf-8");

        if (!HttpMethod.HEAD.equals(request.getMethod())) {
            throw new HttpException(HttpStatus.NOT_FOUND);
        }
    }

}
