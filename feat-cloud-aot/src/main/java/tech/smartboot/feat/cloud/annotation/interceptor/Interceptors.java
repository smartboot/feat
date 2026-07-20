
package tech.smartboot.feat.cloud.annotation.interceptor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 为类、方法或构造方法声明要使用的 Feat 方法拦截器。
 * <p>
 * 注解值是一个有序的拦截器类型列表。当前 AOT 代码生成流程会为符合条件的公共实例方法
 * 构造拦截器链，并按照数组中的声明顺序进入各拦截器；调用返回时则按照相反顺序退出。
 * 列表中的类型通常应使用 {@link Interceptor} 标记，并提供唯一的 {@link AroundInvoke} 方法。
 * </p>
 *
 * <h2>作用域与合并规则</h2>
 * <ul>
 *     <li>标注在类上：作为类中所有可拦截方法的默认拦截器列表。</li>
 *     <li>标注在方法上：仅应用于当前方法。</li>
 *     <li>类和方法同时标注：方法级列表不会覆盖类级列表，而是追加到类级列表之后。</li>
 *     <li>当前 AOT 实现只为 {@code public}、非 {@code static}、非 {@code final} 的实例方法
 *     生成调用链；构造方法虽然是合法的注解目标，但不会进入当前的方法拦截链。</li>
 * </ul>
 *
 * <h2>执行顺序示例</h2>
 * <pre>{@code
 * @Controller("/orders")
 * @Interceptors(AuthenticationInterceptor.class)
 * public class OrderController {
 *
 *     @RequestMapping("/create")
 *     @Interceptors({LoggingInterceptor.class, TransactionInterceptor.class})
 *     public Order createOrder() {
 *         // 实际进入顺序：Authentication -> Logging -> Transaction -> 目标方法
 *         // 正常退出顺序：目标方法 -> Transaction -> Logging -> Authentication
 *         return new Order();
 *     }
 * }
 * }</pre>
 *
 * <p>
 * 本注解仅在编译期由 Feat AOT 注解处理器读取，不支持通过运行时反射查询。
 * </p>
 *
 * @see Interceptor
 * @see AroundInvoke
 */
@Target({TYPE, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface Interceptors {

    /**
     * 按执行顺序声明拦截器类型。
     * <p>
     * 数组中的第一个拦截器最先接收调用，随后由各拦截器通过
     * {@link tech.smartboot.feat.cloud.interceptor.InvocationContext#proceed() proceed()}
     * 将调用传递给下一个拦截器，最后执行目标方法。若某个拦截器未调用 {@code proceed()}，
     * 其后的拦截器和目标方法都不会执行。
     * </p>
     *
     * @return 有序的拦截器类数组
     */
    Class[] value();
}
