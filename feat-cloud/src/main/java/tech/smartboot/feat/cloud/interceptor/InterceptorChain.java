package tech.smartboot.feat.cloud.interceptor;

import tech.smartboot.feat.core.common.exception.FeatException;

import java.lang.reflect.Method;
import java.util.List;

public class InterceptorChain implements InvocationContext {
    private final Object target;
    private final Method method;
    private Object[] params;
    private final List<InterceptorFunction> list;
    private int location = 0;
    private boolean done;

    public InterceptorChain(Object target, Method method, Object[] params, List<InterceptorFunction> list) {
        this.target = target;
        this.method = method;
        this.params = params;
        this.list = list;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getParameters() {
        return params;
    }

    @Override
    public void setParameters(Object[] params) {
        this.params = params;
    }

    public Object proceed() {
        int index = location++;
        if (index < list.size()) {
            InterceptorFunction filterInfo = list.get(index);
            try {
                return filterInfo.apply(this);
            } catch (Exception e) {
                throw new FeatException(e);
            }
        }
        done = true;
        return null;
    }

    public boolean isDone() {
        return done;
    }
}
