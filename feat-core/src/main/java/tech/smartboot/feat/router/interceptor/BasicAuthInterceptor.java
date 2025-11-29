/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router.interceptor;

import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.router.Chain;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Interceptor;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * 基础认证拦截器，用于实现HTTP Basic Authentication认证机制
 * <p>
 * BasicAuthInterceptor实现了HTTP基础认证协议，通过检查请求头中的Authorization字段
 * 来验证用户身份。如果认证失败，会返回401未授权状态码并提示客户端进行认证。
 * </p>
 * <p>
 * HTTP Basic Authentication是一种简单的认证方式，将用户名和密码以"用户名:密码"的格式
 * 组合后进行Base64编码，然后在请求头中添加"Authorization: Basic [编码后的字符串]"。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/18/25
 */
public class BasicAuthInterceptor implements Interceptor {
    /**
     * 预计算的基础认证字符串
     * <p>
     * 在构造函数中预先计算好正确的Basic认证头值，避免每次请求都进行字符串拼接和编码操作
     * </p>
     */
    private final String basic;

    /**
     * 构造一个基础认证拦截器实例
     *
     * @param username 用户名
     * @param password 密码
     */
    public BasicAuthInterceptor(String username, String password) {
        basic = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    /**
     * 拦截并处理请求，执行基础认证检查
     * <p>
     * 检查请求头中的Authorization字段是否与预设的认证信息匹配：
     * <ul>
     *   <li>如果匹配成功，则继续执行后续拦截器或处理器</li>
     *   <li>如果匹配失败，则返回401未授权状态码并提示认证</li>
     * </ul>
     * </p>
     *
     * @param context             请求上下文
     * @param completableFuture   异步完成回调
     * @param chain               拦截器链
     * @throws Throwable          处理过程中可能抛出的异常
     */
    @Override
    public void intercept(Context context, CompletableFuture<Void> completableFuture, Chain chain) throws Throwable {
        String clientBasic = context.Request.getHeader(HeaderName.AUTHORIZATION);
        if (FeatUtils.equals(clientBasic, this.basic)) {
            chain.proceed(context, completableFuture);
        } else {
            HttpResponse response = context.Response;
            response.setHeader(HeaderName.WWW_AUTHENTICATE, "Basic realm=\"feat\"");
            response.setHttpStatus(HttpStatus.UNAUTHORIZED);
            response.close();
        }
    }
}