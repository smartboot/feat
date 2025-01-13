package tech.smartboot.feat.restful;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.utils.DateUtils;
import tech.smartboot.feat.core.common.utils.Mimetypes;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.handler.HttpServerHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/26
 */
public class StaticResourceHandler extends HttpServerHandler {
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
            super.handle(request, completableFuture);
        } else {
            asyncExecutor.execute(() -> {
                try {
                    handle(request);
                    completableFuture.complete(null);
                } catch (IOException e) {
                    completableFuture.completeExceptionally(e);
                }
            });
        }
    }

    @Override
    public void handle(HttpRequest request) throws IOException {
        HttpResponse response=request.getResponse();
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
            while ((length = inputStream.read(bytes)) > 0) {
                response.getOutputStream().write(bytes, 0, length);
            }
        }
    }
}
