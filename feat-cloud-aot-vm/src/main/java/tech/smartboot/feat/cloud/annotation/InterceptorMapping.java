/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 拦截器映射注解，用于将实现了Interceptor接口的方法映射到指定的URL路径上。
 * 该注解只能应用于方法上，并在编译时处理（RetentionPolicy.SOURCE）。
 * <p>
 * 拦截器可以拦截匹配指定URL模式的请求，并在请求处理前执行自定义逻辑，
 * 如权限验证、日志记录、请求参数验证等。
 * <p>
 * 使用方式：
 * 1. 在返回Interceptor类型的方法上添加@InterceptorMapping注解
 * 2. 通过value属性指定一个或多个URL匹配模式
 * <p>
 * 示例：
 * <pre>
 * {@code
 * @InterceptorMapping({"/api/*", "/user/*"})
 * public Interceptor securityInterceptor() {
 *     return (context, completableFuture, chain) -> {
 *         // 执行拦截逻辑，如权限验证
 *         if (isAuthorized(context)) {
 *             // 继续处理请求
 *             chain.proceed(context, completableFuture);
 *         } else {
 *             // 拒绝请求
 *             context.status(401);
 *             completableFuture.complete(null);
 *         }
 *     };
 * }
 * }
 * </pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see tech.smartboot.feat.router.Interceptor
 * @see tech.smartboot.feat.router.Router#addInterceptors(java.util.List, tech.smartboot.feat.router.Interceptor)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InterceptorMapping {
    /**
     * 拦截器映射路径，用于指定拦截器应用的URL模式。
     * <p>
     * 支持以下匹配模式：
     * <ul>
     *   <li>"/path" - 精确匹配指定路径</li>
     *   <li>"/path/*" - 匹配指定路径下的所有子路径</li>
     * </ul>
     * <p>
     * 可以指定多个路径模式，拦截器将应用于所有匹配的请求。
     *
     * @return 拦截器应用的URL模式数组
     */
    String[] value() default "";

}
