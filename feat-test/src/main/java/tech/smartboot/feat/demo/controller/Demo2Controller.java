/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
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
    public RestResult<TestParam0<String, TestParam>> test61(@Param("param1") TestParam param) {
        return RestResult.ok(new TestParam0<String, TestParam>());
    }
}
