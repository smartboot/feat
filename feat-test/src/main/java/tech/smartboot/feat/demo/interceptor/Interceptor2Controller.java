package tech.smartboot.feat.demo.interceptor;

import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.interceptor.Interceptors;
import tech.smartboot.feat.core.server.HttpRequest;

@Controller("interceptor2")
@Interceptors({Interceptor1.class, Interceptor2.class})
public class Interceptor2Controller {

    @RequestMapping("/test1")
    public String test1() {
        return "test1";
    }

    @RequestMapping("/test2")
    public String test2() {
        return "test2";
    }

    @RequestMapping("/test3")
    public void test3() {
    }

    @RequestMapping("/test4")
    @Interceptors(Interceptor2.class)
    public void test4(HttpRequest request) {
    }
}
