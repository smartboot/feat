package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

@Controller("demo2")
class Demo2Controller {
    @RequestMapping("/hello")
    public String test1() {
        return "hello world";
    }

    @RequestMapping("/param1")
    public String test2(@Param("param") String param) {
        return "hello " + param;
    }

    @RequestMapping("/param2")
    public String test3(@Param("param1") String param1, @Param("param2") String param2) {
        return "hello " + param1 + " " + param2;
    }

    @RequestMapping("/param3")
    public String test4(TestParam param) {
        return "hello " + param.getParam1() + " " + param.getParam2();
    }

    @RequestMapping("/param4")
    public String test5(@Param("param") TestParam param) {
        return "hello param is " + param;
    }

    @RequestMapping("/param5")
    public String test6(@Param("param1") TestParam param) {
        return "hello param is " + param.getParam1();
    }

    @RequestMapping("/test61")
    public RestResult<TestParam0<String,TestParam>> test61(@Param("param1") TestParam param) {
        return RestResult.ok(new TestParam0<String,TestParam>());
    }
}
