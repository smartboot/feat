package tech.smartboot.feat.gateway.proxy;

import tech.smartboot.feat.core.client.stream.Stream;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 统一的响应输出。所有错误都按 OpenAI 的 error 结构返回，
 * 这样现成的 SDK（openai-java / langchain）能直接解析出 message。
 */
public final class Responder {
    private static final Logger LOGGER = LoggerFactory.getLogger(Responder.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Responder() {
    }

    public static void json(HttpRequest request, int status, String body) {
        HttpResponse response = request.getResponse();
        try {
            byte[] bytes = body.getBytes(UTF8);
            response.setHttpStatus(status, reason(status));
            response.setHeader("Content-Type", "application/json;charset=utf-8");
            response.setHeader("Content-Length", String.valueOf(bytes.length));
            response.write(bytes);
        } catch (IOException e) {
            LOGGER.debug("写响应失败: {}", e.getMessage());
        } finally {
            response.close();
        }
    }

    public static void error(HttpRequest request, int status, String message, String type, String code) {
        StringBuilder sb = new StringBuilder(160);
        sb.append("{\"error\":{\"message\":\"");
        escape(sb, message);
        sb.append("\",\"type\":\"").append(type).append('"');
        if (code != null) {
            sb.append(",\"code\":\"").append(code).append('"');
        }
        sb.append("}}");
        json(request, status, sb.toString());
    }

    public static void ok(HttpRequest request, String body) {
        json(request, 200, body);
    }

    private static String reason(int status) {
        switch (status) {
            case 200:
                return "OK";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 409:
                return "Conflict";
            case 413:
                return "Payload Too Large";
            case 429:
                return "Too Many Requests";
            case 499:
                return "Client Closed Request";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Timeout";
            default:
                return HttpStatus.OK.getReasonPhrase();
        }
    }

    private static void escape(StringBuilder sb, String text) {
        if (text == null) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
    }

    /**
     * 空实现，用于占位（例如上游返回空的非流式响应时）。
     */
    public static final Stream NOOP_STREAM = new Stream() {
        @Override
        public void stream(tech.smartboot.feat.core.client.HttpResponse response, byte[] bytes, boolean end) {
        }
    };
}
