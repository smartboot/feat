package tech.smartboot.feat.cloud.interceptor;

/**
 * Feat 拦截器的统一运行时函数接口。
 * <p>
 * AOT 注解处理器会将使用
 * {@code tech.smartboot.feat.cloud.annotation.interceptor.AroundInvoke} 标记的方法适配为该接口，
 * 并以拦截器类型为键注册到应用上下文。业务方法执行时，{@link InterceptorChain} 按声明顺序
 * 取得并调用这些函数。
 * </p>
 *
 * <p>
 * 实现可以在调用 {@link InvocationContext#proceed()} 前后执行环绕逻辑，也可以不调用
 * {@code proceed()} 而直接返回，从而短路后续调用链。
 * </p>
 *
 * @see InvocationContext
 * @see InterceptorChain
 */
@FunctionalInterface
public interface InterceptorFunction {

    /**
     * 执行一次拦截逻辑。
     *
     * @param context 当前方法调用的上下文；同一次调用链中的拦截器共享该实例
     * @return 当前拦截器的处理结果，通常为 {@code context.proceed()} 的返回值
     * @throws Exception 拦截逻辑或后续调用执行失败时抛出
     */
    Object apply(InvocationContext context) throws Exception;
}
