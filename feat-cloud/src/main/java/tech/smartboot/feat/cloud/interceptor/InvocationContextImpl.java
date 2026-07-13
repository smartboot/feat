package tech.smartboot.feat.cloud.interceptor;

import java.lang.reflect.Method;

public class InvocationContextImpl implements InvocationContext{
    @Override
    public Object getTarget() {
        return null;
    }

    @Override
    public Method getMethod() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return new Object[0];
    }

    @Override
    public void setParameters(Object[] params) {

    }

    @Override
    public Object proceed() throws Exception {
        return null;
    }
}
