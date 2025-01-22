package tech.smartboot.feat.test.restful;


import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.PostConstruct;
import tech.smartboot.feat.core.apt.annotation.PreDestroy;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;

@Controller
class Demo1Controller {

    @PostConstruct
    public void init() {
        System.out.println("init");
    }

    @RequestMapping
    public String test1() {
        return "hello";
    }


    @PreDestroy
    public void destroy() {
        System.out.println("destroy");
    }
}