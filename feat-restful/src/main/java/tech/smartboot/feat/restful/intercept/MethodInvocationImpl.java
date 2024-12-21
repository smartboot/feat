package tech.smartboot.feat.restful.intercept;

import tech.smartboot.feat.server.HttpRequest;
import tech.smartboot.feat.server.HttpResponse;

import java.lang.reflect.Method;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/25
 */
public final class MethodInvocationImpl implements MethodInvocation {
    private final Method method;
    private final Object[] args;
    private final Object object;
    private final HttpRequest request;
    private final HttpResponse response;

    public MethodInvocationImpl(Method method, Object[] args, Object object, HttpRequest request, HttpResponse response) {
        this.method = method;
        this.args = args;
        this.object = object;
        this.request = request;
        this.response = response;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return args;
    }

    @Override
    public Object getThis() {
        return object;
    }

    @Override
    public Object proceed() throws Throwable {
        return method.invoke(object, args);
    }

    @Override
    public HttpRequest request() {
        return request;
    }

    @Override
    public HttpResponse response() {
        return response;
    }

}
