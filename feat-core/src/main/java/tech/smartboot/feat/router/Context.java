/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.Session;

import java.util.Map;

/**
 * 请求上下文对象，封装了一次HTTP请求处理过程中的所有相关信息
 * <p>
 * Context作为请求处理的核心对象，在整个请求生命周期中传递，
 * 包含了请求对象、响应对象、路径参数以及会话信息等关键数据。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Context {
    /**
     * HTTP请求对象，包含了客户端发送的所有请求信息
     * <p>
     * 提供访问请求头、请求体、请求方法、请求URI等信息的方法
     * </p>
     */
    public final HttpRequest Request;

    /**
     * HTTP响应对象，用于构建和发送响应给客户端
     * <p>
     * 提供设置响应状态码、响应头、响应体等方法
     * </p>
     */
    public final HttpResponse Response;

    /**
     * 路径参数映射表，存储URL路径中的动态参数
     * <p>
     * 例如对于路径/user/:id，当访问/user/123时，此映射表将包含{id=123}
     * </p>
     */
    private final Map<String, String> pathParams;

    /**
     * 关联的路由器实例，用于获取会话等服务
     */
    private final Router router;

    /**
     * 当前请求的会话对象，延迟初始化
     */
    private Session session;

    /**
     * 构造一个新的请求上下文实例
     *
     * @param router     关联的路由器实例
     * @param request    HTTP请求对象
     * @param pathParams 路径参数映射表
     */
    Context(Router router, HttpRequest request, Map<String, String> pathParams) {
        this.router = router;
        this.Request = request;
        this.Response = request.getResponse();
        this.pathParams = pathParams;
    }

    /**
     * 获取指定名称的路径参数值
     *
     * @param key 路径参数名称
     * @return 对应的路径参数值，如果不存在则返回null
     */
    public String pathParam(String key) {
        return pathParams.get(key);
    }

    /**
     * 获取当前请求的会话对象
     * <p>
     * 如果会话对象尚未创建，则会自动创建一个新的会话
     * </p>
     *
     * @return 当前请求的会话对象
     */
    public Session session() {
        if (session != null) {
            return session;
        }
        session = router.getSessionManager().getSession(Request, true);
        return session;
    }
}