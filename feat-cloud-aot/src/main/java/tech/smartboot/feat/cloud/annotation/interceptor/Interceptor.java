
package tech.smartboot.feat.cloud.annotation.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * 标记一个由 Feat AOT 注解处理器管理的方法拦截器。
 * <p>
 * 编译期间，Feat 会为被标记的类生成加载代码，创建该类的实例，并将它注册到应用上下文的
 * 拦截器集合中。业务类可以通过 {@link Interceptors} 引用该拦截器，使方法调用进入对应的
 * 拦截器链。
 * </p>
 *
 * <h2>定义约束</h2>
 * <ul>
 *     <li>该注解只能标注类。</li>
 *     <li>类中必须且只能有一个使用 {@link AroundInvoke} 标记的拦截方法。</li>
 *     <li>拦截方法应接收一个
 *     {@link tech.smartboot.feat.cloud.interceptor.InvocationContext InvocationContext} 参数，
 *     并返回 {@link Object}。</li>
 *     <li>拦截方法需要调用
 *     {@link tech.smartboot.feat.cloud.interceptor.InvocationContext#proceed() proceed()}，
 *     才会继续执行后续拦截器及最终的目标方法；不调用该方法即可提前结束调用链。</li>
 * </ul>
 *
 * <h2>示例</h2>
 * <pre>{@code
 * @Interceptor
 * public class LoggingInterceptor {
 *
 *     @AroundInvoke
 *     public Object intercept(InvocationContext context) throws Exception {
 *         System.out.println("before: " + context.getMethod().getName());
 *         try {
 *             return context.proceed();
 *         } finally {
 *             System.out.println("after: " + context.getMethod().getName());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>
 * 本注解仅保留在源代码阶段，由注解处理器在编译期消费，不会出现在运行时字节码中，
 * 因而不能通过运行时反射读取。
 * </p>
 *
 * @see AroundInvoke
 * @see Interceptors
 * @see tech.smartboot.feat.cloud.interceptor.InvocationContext
 */
@Retention(RetentionPolicy.SOURCE)
@Target(TYPE)
@Documented
public @interface Interceptor {

}
