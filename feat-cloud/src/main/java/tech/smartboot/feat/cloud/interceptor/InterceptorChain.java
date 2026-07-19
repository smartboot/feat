package tech.smartboot.feat.cloud.interceptor;

import java.lang.reflect.Method;
import java.util.List;

public class InterceptorChain {
    private Object target;
    private Method method;
    private Object[] params;
    private List<InvocationContext> list;
    private int location = 0;

    public InterceptorChain(Object target, Method method, Object[] params) {
        this.target = target;
        this.method = method;
        this.params = params;
    }

    public Object proceed() {
        int index = location++;
        if (index < list.size()) {
            InvocationContext filterInfo = list.get(index);
            try {
                return filterInfo.proceed();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return finalProcess();
    }

    public Object finalProcess() {
        return null;
    }
}
