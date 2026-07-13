
package tech.smartboot.feat.cloud.interceptor;


import java.lang.reflect.Method;

public interface InvocationContext {

    Object getTarget();


    Method getMethod();

    Object[] getParameters();


    void setParameters(Object[] params);


    Object proceed() throws Exception;
}
