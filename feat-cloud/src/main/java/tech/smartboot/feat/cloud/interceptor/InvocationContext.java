
package tech.smartboot.feat.cloud.interceptor;


import java.lang.reflect.Method;

/**
 * 一次被拦截方法调用的上下文。
 * <p>
 * Feat 为每次进入拦截器链的方法调用创建独立上下文，并将同一个上下文实例依次传递给链中的
 * {@link InterceptorFunction}。拦截器可以通过该接口读取目标对象、目标方法和调用参数，并通过
 * {@link #proceed()} 将控制权交给下一个拦截器或最终的目标方法。
 * </p>
 *
 * <h2>典型用法</h2>
 * <pre>{@code
 * public Object intercept(InvocationContext context) throws Exception {
 *     Method method = context.getMethod();
 *     Object[] arguments = context.getParameters();
 *     System.out.println("invoke " + method.getName() + " with " + arguments.length + " arguments");
 *     return context.proceed();
 * }
 * }</pre>
 *
 * <p>
 * 调用 {@code proceed()} 是显式行为：若拦截器直接返回而不调用它，调用链会在当前拦截器处
 * 短路，后续拦截器和目标方法均不会执行。上下文只提供参数读取能力，不提供替换整个参数数组的
 * 方法。
 * </p>
 *
 * @see InterceptorChain
 * @see InterceptorFunction
 */
public interface InvocationContext {

    /**
     * 获取当前被调用的目标对象。
     * <p>
     * 在 AOT 生成的调用代码中，该对象通常是 Feat 创建的 Bean 增强实例，可用于读取实例状态
     * 或识别实际接收本次调用的对象。
     * </p>
     *
     * @return 当前方法调用的目标对象
     */
    Object getTarget();

    /**
     * 获取当前被拦截的目标方法。
     * <p>
     * 返回的 {@link Method} 描述业务方法本身，可用于读取方法名、参数类型、返回类型及运行时
     * 可见的注解等元数据。
     * </p>
     *
     * @return 当前被拦截方法的反射对象
     */
    Method getMethod();

    /**
     * 获取创建调用上下文时记录的实参数组。
     * <p>
     * 对于无参数方法，该方法返回长度为 {@code 0} 的数组。该数组用于向拦截器暴露调用参数；
     * 接口不提供替换整个参数数组的能力，调用方不应依赖修改返回数组来改变目标方法的实参。
     * </p>
     *
     * @return 当前调用的参数数组，参数顺序与 {@link #getMethod()} 的形参顺序一致
     */
    Object[] getParameters();

    /**
     * 推进当前调用链。
     * <p>
     * 若链中仍有未执行的拦截器，则调用下一个拦截器；否则执行最终的目标方法。该方法的返回值
     * 会沿调用链逐层返回，因此当前拦截器既可以原样返回，也可以对结果进行转换。
     * </p>
     * <p>
     * 每次调用都会继续推进链的位置，而不是从头重新执行。通常每个拦截器在一次调用过程中只应
     * 调用一次 {@code proceed()}。
     * </p>
     *
     * @return 后续拦截器或目标方法的执行结果；目标方法返回 {@code void} 时结果为 {@code null}
     * @throws Exception 后续拦截器或目标调用执行失败时抛出
     */
    Object proceed() throws Exception;
}
