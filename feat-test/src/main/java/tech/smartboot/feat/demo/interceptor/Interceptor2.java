package tech.smartboot.feat.demo.interceptor;

import tech.smartboot.feat.cloud.annotation.interceptor.AroundInvoke;
import tech.smartboot.feat.cloud.annotation.interceptor.Interceptor;
import tech.smartboot.feat.cloud.interceptor.InvocationContext;

@Interceptor
public class Interceptor2 {

    @AroundInvoke
    public Object intercept(InvocationContext chain) throws Exception {
        System.out.println("interceptor2 begin");
        try {
            return chain.proceed();
        } finally {
            System.out.println("interceptor2 end");
        }
    }

}
