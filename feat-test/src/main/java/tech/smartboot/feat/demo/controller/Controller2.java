package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

@Controller("controller2")
public class Controller2 {
    @RequestMapping("/helloworld")
    public String helloworld() {
        return "hello " + Controller2.class.getSimpleName();
    }
}
