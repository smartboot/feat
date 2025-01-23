package tech.smartboot.feat.demo.apt;

import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.PostConstruct;
import tech.smartboot.feat.core.apt.annotation.PreDestroy;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;
import tech.smartboot.feat.core.apt.annotation.RequestMethod;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.restful.RestResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller("abc")
public class ControllerDemo {
    @RequestMapping(value = "/test", method = {RequestMethod.GET, RequestMethod.POST})
    public void hello(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");

    }

    @RequestMapping(value = "/test1", method = {RequestMethod.GET, RequestMethod.POST})
    public String hello1(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");
        return "hello";
    }

    @RequestMapping(value = "/test2", method = {RequestMethod.GET, RequestMethod.POST})
    public List<String> hello2(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");
        return Collections.emptyList();
    }

    @RequestMapping(value = "/test11", method = {RequestMethod.GET, RequestMethod.POST})
    public RestResult<String> hello112(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");
        return RestResult.ok("hello");
    }

    @RequestMapping(value = "/testObj", method = {RequestMethod.GET, RequestMethod.POST})
    public RestResult<Dto> hello11ss2(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");
        Dto dto = new Dto();
        dto.setName("aaa");
        dto.setAge(18);
        dto.setAddress("bbb");
        return RestResult.ok(dto);
    }

    @RequestMapping(value = "/testObjs", method = {RequestMethod.GET, RequestMethod.POST})
    public RestResult<List<Dto>> hello11ss2s(HttpRequest request, HttpResponse response) {
        System.out.println("aaa");
        List<Dto> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Dto dto = new Dto();
            dto.setName("aaa" + i);
            dto.setAge(18);
            dto.setAddress("bbb" + i);
            list.add(dto);
        }
        return RestResult.ok(list);
    }

    @PostConstruct
    public void init() {

    }

    @PreDestroy
    public void destroy() {

    }
}
