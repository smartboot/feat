package tech.smartboot.feat.cloud.interceptor;

public interface InterceptorFunction {
    Object apply(InvocationContext context) throws Exception;
}
