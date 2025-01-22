package tech.smartboot.feat.demo.apt;

import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.PostConstruct;
import tech.smartboot.feat.core.apt.annotation.PreDestroy;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;
import tech.smartboot.feat.core.apt.annotation.RequestMethod;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.util.Collections;
import java.util.List;

@Controller("abc")
public class ControllerDemo {
    @RequestMapping(value = "/test", method = {RequestMethod.GET, RequestMethod.POST})
    public void hello(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");

    }

    @RequestMapping(value = "/test", method = {RequestMethod.GET, RequestMethod.POST})
    public String hello1(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");
        return "hello";
    }

    @RequestMapping(value = "/test", method = {RequestMethod.GET, RequestMethod.POST})
    public List<String> hello2(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");
        return Collections.emptyList();
    }


    @PostConstruct
    public void init() {

    }

    @PreDestroy
    public void destroy() {

    }
}
