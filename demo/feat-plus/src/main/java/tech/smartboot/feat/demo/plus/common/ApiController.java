package tech.smartboot.feat.demo.plus.common;

import java.util.function.Supplier;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.router.Context;

/**
 * Controller 统一入口：把业务异常翻译成合适的 HTTP 状态码 + RestResult。
 * <p>
 * Feat 没有全局异常处理链，异常的收敛只能放在调用点。
 * 前端 client 依赖 401 来清理本地令牌，因此状态码必须显式设置。
 */
public abstract class ApiController {

    protected <T> RestResult<T> execute(Context context, Supplier<T> action) {
        try {
            return RestResult.ok(action.get());
        } catch (DomainException e) {
            if (e.isUnauthorized()) {
                context.Response.setHttpStatus(401, "Unauthorized");
            } else if (e.isForbidden()) {
                context.Response.setHttpStatus(403, "Forbidden");
            } else if (e.isNotFound()) {
                context.Response.setHttpStatus(404, "Not Found");
            } else {
                context.Response.setHttpStatus(400, "Bad Request");
            }
            return RestResult.fail(e.getMessage());
        } catch (RuntimeException e) {
            context.Response.setHttpStatus(500, "Internal Server Error");
            return RestResult.fail("服务器处理失败：" + e.getMessage());
        }
    }
}
