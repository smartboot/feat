package tech.smartboot.feat.demo.plus.common;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.router.Context;

/**
 * Controller 统一入口：把业务异常翻译成合适的 HTTP 状态码 + RestResult。
 * <p>
 * Feat 没有全局异常处理链，异常的收敛只能放在调用点。
 * 前端 client 依赖 401 来清理本地令牌，因此状态码必须显式设置。
 * <p>
 * 错误契约的三条保证都在这里落地：
 * <ul>
 * <li>响应体的 {@code code} 与 HTTP 状态码一致——RestResult.fail 默认写 500，
 *     如果不纠正，HTTP 403 的响应体里 code 也是 500，第三方没法按 body 判断；</li>
 * <li>每个经过这里的响应都带 {@code X-Trace-Id}，前端报障时把这个头带上，
 *     服务端就能按 id 找到对应的错误日志；</li>
 * <li>未预期异常（RuntimeException）不再吞栈：完整堆踪带着同一个 traceId 进服务端日志，
 *     响应体只给一句通用文案加 traceId——堆踪泄漏到 HTTP 响应等于把实现细节交出去。</li>
 * </ul>
 */
public abstract class ApiController {

    private static final Logger LOGGER = Logger.getLogger(ApiController.class.getName());

    /** 响应头与服务端日志共用的关联标识。 */
    public static final String TRACE_HEADER = "X-Trace-Id";

    protected <T> RestResult<T> execute(Context context, Supplier<T> action) {
        String traceId = newTraceId();
        context.Response.setHeader(TRACE_HEADER, traceId);
        try {
            return RestResult.ok(action.get());
        } catch (DomainException e) {
            ErrorCode errorCode = e.getErrorCode();
            context.Response.setHttpStatus(errorCode.getHttpStatus(), errorCode.getReason());
            RestResult<T> body = RestResult.fail(e.getMessage());
            body.setCode(errorCode.getHttpStatus());
            return body;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "[" + traceId + "] " + context.Request.getMethod()
                    + " " + context.Request.getRequestURI() + " failed", e);
            context.Response.setHttpStatus(500, ErrorCode.INTERNAL_ERROR.getReason());
            RestResult<T> body = RestResult.fail("服务器处理失败（traceId: " + traceId + "）");
            body.setCode(500);
            return body;
        }
    }

    /**
     * 16 位十六进制的短 id。不用完整 UUID：它只需要在「一台服务的日志里能检索」，
     * 不需要跨机器全局唯一，短一点在前端报障单里更可读。
     */
    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
