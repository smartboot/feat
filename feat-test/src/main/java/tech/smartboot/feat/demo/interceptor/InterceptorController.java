package tech.smartboot.feat.demo.interceptor;

import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.interceptor.Interceptors;

@Controller("interceptor")
public class InterceptorController {

    @RequestMapping("/test1")
    @Interceptors({Interceptor1.class, Interceptor2.class})
    public String test1() {
        return "test1";
    }

    @RequestMapping("/test2")
    @Interceptors({Interceptor2.class, Interceptor1.class})
    public String test2() {
        return "test2";
    }
}
