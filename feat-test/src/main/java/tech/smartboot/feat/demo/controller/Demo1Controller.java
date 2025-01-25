package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

@Controller
class Demo1Controller {

    @PostConstruct
    public void init() {
        System.out.println("init");
    }

    @RequestMapping("/test1")
    public String test1() {
        return "hello";
    }


    @PreDestroy
    public void destroy() {
        System.out.println("destroy");
    }
}