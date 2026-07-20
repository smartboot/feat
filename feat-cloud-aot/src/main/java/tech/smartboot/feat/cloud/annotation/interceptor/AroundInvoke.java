package tech.smartboot.feat.cloud.annotation.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * 标记拦截器类中负责环绕目标调用的方法。
 * <p>
 * Feat AOT 注解处理器会在 {@link Interceptor} 类中查找该注解，并把被标记的方法转换为
 * 拦截器函数。每次命中 {@link Interceptors} 配置时，该方法都会收到当前调用的
 * {@link tech.smartboot.feat.cloud.interceptor.InvocationContext InvocationContext}，从中可读取
 * 目标对象、目标方法和调用参数，并决定是否继续调用链。
 * </p>
 *
 * <h2>方法约束</h2>
 * <ul>
 *     <li>每个 {@link Interceptor} 类必须且只能声明一个该注解方法。</li>
 *     <li>方法应接收一个 {@code InvocationContext} 参数并返回 {@code Object}。</li>
 *     <li>方法可以声明 {@code throws Exception}，以便将目标调用或拦截逻辑中的异常继续向外传递。</li>
 * </ul>
 *
 * <h2>调用语义</h2>
 * <ul>
 *     <li>调用 {@code context.proceed()}：继续执行下一个拦截器；链尾会执行目标方法。</li>
 *     <li>不调用 {@code context.proceed()}：短路调用链，当前方法的返回值即为本次调用结果。</li>
 *     <li>在 {@code proceed()} 前后编写逻辑：可实现鉴权、日志、计时、事务等环绕行为。</li>
 *     <li>通过 {@code context.getParameters()}：可读取本次调用的参数；上下文不提供参数替换方法。</li>
 * </ul>
 *
 * <pre>{@code
 * @AroundInvoke
 * public Object intercept(InvocationContext context) throws Exception {
 *     long start = System.nanoTime();
 *     try {
 *         return context.proceed();
 *     } finally {
 *         System.out.println("cost(ns): " + (System.nanoTime() - start));
 *     }
 * }
 * }</pre>
 *
 * <p>
 * 本注解采用 {@link RetentionPolicy#SOURCE SOURCE} 保留策略，仅用于编译期代码生成。
 * </p>
 *
 * @see Interceptor
 * @see Interceptors
 * @see tech.smartboot.feat.cloud.interceptor.InvocationContext
 */
@Target(METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AroundInvoke {
}
