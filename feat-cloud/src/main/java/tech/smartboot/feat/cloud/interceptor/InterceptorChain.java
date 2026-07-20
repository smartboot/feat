package tech.smartboot.feat.cloud.interceptor;

import tech.smartboot.feat.core.common.exception.FeatException;

import java.lang.reflect.Method;
import java.util.List;

/**
 * {@link InvocationContext} 的默认调用链实现。
 * <p>
 * 每个被拦截的方法调用都会创建一个独立的调用链实例。调用链持有目标对象、目标方法、调用参数
 * 以及按执行顺序排列的 {@link InterceptorFunction} 列表，并通过内部游标记录下一个待执行的
 * 拦截器。当全部拦截器执行完毕后，调用 {@link #apply()} 执行最终目标逻辑。
 * </p>
 *
 * <h2>执行过程</h2>
 * <ol>
 *     <li>业务方法入口调用 {@link #proceed()}。</li>
 *     <li>{@code proceed()} 取出当前位置的拦截器并推进游标。</li>
 *     <li>拦截器再次调用 {@code proceed()}，将控制权交给下一个拦截器。</li>
 *     <li>游标到达列表末尾后执行 {@link #apply()}。</li>
 *     <li>执行结果按照与进入顺序相反的方向逐层返回。</li>
 * </ol>
 *
 * <p>
 * 该类包含可变的执行游标，设计为单次方法调用使用，不应跨调用复用，也不保证线程安全。
 * </p>
 *
 * @see InvocationContext
 * @see InterceptorFunction
 */
public abstract class InterceptorChain implements InvocationContext {
    /** 当前方法调用的目标 Bean 或增强实例。 */
    private final Object target;

    /** 当前被拦截的业务方法。 */
    private final Method method;

    /** 创建调用链时记录的业务方法实参。 */
    private final Object[] params;

    /** 按声明顺序排列的拦截器函数。 */
    private final List<InterceptorFunction> list;

    /** 下一个待执行拦截器在 {@link #list} 中的位置。 */
    private int location = 0;

    /**
     * 创建一次方法调用对应的拦截器链。
     *
     * @param target 当前方法调用的目标对象
     * @param method 当前被拦截的业务方法
     * @param params 当前调用的实参数组；无参数方法应传入空数组
     * @param list   按执行顺序排列的拦截器函数列表
     */
    public InterceptorChain(Object target, Method method, Object[] params, List<InterceptorFunction> list) {
        this.target = target;
        this.method = method;
        this.params = params;
        this.list = list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Method getMethod() {
        return method;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getParameters() {
        return params;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 拦截器函数抛出的 {@link Exception} 会被包装为 {@link FeatException}，以便调用链通过统一的运行时
     * 异常向外传播失败信息。
     * </p>
     *
     * @throws FeatException 当前拦截器或后续调用抛出异常时抛出
     */
    @Override
    public Object proceed() {
        int index = location++;
        try {
            if (index < list.size()) {
                InterceptorFunction filterInfo = list.get(index);
                return filterInfo.apply(this);
            }
            return apply();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new FeatException(e);
        }
    }

    /**
     * 执行拦截器链末端的目标逻辑。
     * <p>
     * AOT 生成代码通过匿名子类实现该方法，并在其中调用原始业务方法。仅当所有前置拦截器都
     * 调用了 {@link #proceed()} 时，该方法才会执行。
     * </p>
     *
     * @return 目标方法的执行结果；目标方法返回 {@code void} 时返回 {@code null}
     * @throws Exception 目标方法声明的受检异常
     */
    public abstract Object apply() throws Exception;

}
