package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

@Controller("controller1")
public class Controller1 {
    @RequestMapping("/helloworld")
    public String helloworld() {
        return "hello " + Controller1.class.getSimpleName();
    }
}
