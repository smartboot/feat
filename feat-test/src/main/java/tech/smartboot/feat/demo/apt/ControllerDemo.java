/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.apt;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.cloud.annotation.Value;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@Controller("abc")
public class ControllerDemo {

    @Value("${a}")
    private String a;

    @Value("${b:abc}")
    private String b;
    @Value("${c.d}")
    private int c;

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

    public void setA(String a) {
        this.a = a;
    }

    public void setB(String b) {
        this.b = b;
    }

    public void setC(int c) {
        this.c = c;
    }

    @PreDestroy
    public void destroy() {

    }
}
