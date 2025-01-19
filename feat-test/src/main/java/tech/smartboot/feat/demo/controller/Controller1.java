package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;

@Controller("controller1")
public class Controller1 {
    @RequestMapping("/helloworld")
    public String helloworld() {
        return "hello " + Controller1.class.getSimpleName();
    }
}
