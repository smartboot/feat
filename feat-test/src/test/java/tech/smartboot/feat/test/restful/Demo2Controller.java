package tech.smartboot.feat.test.restful;

import tech.smartboot.feat.restful.annotation.Controller;
import tech.smartboot.feat.restful.annotation.Param;
import tech.smartboot.feat.restful.annotation.RequestMapping;

@Controller("demo2")
class Demo2Controller {
    @RequestMapping
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
}
