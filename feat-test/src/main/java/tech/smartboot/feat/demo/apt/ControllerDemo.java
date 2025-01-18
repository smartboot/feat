package tech.smartboot.feat.demo.apt;

import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;
import tech.smartboot.feat.core.apt.annotation.RequestMethod;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

@Controller("abc")
public class ControllerDemo {
    @RequestMapping(value = "/test", method = {RequestMethod.GET, RequestMethod.POST})
    public void hello(HttpRequest request, HttpResponse response) {

    }
}
