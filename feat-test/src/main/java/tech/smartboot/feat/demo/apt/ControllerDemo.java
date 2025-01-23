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
            dto.setAddress("aa" + i);
            dto.setList(new ArrayList<>());
            for (int j = 0; j < 3; j++) {
                Dto1 dto1 = new Dto1();
                dto.getList().add(dto1);
                dto1.setName("bb" + j);
                dto1.setAge(18);
                dto1.setAddress("bbb" + j);
                dto1.setList(new ArrayList<>());
                for (int k = 0; k < 3; k++) {
                    Dto2 dto2 = new Dto2();
                    dto1.getList().add(dto2);
                    dto2.setName("cc" + k);
                    dto2.setAge(18);
                    dto2.setAddress("cc" + k);
                    dto2.setList(new ArrayList<>());
                    for (int l = 0; l < 3; l++) {
                        Dto3 dto3 = new Dto3();
                        dto2.getList().add(dto3);
                        dto3.setName("ddd" + l);
                        dto3.setAge(18);
                        dto3.setAddress("ddd" + l);
                    }
                }
            }
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
