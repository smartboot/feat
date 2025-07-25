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


import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.server.Session;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@Controller("controller2")
public class Controller2 {
    @RequestMapping("/helloworld")
    public String helloworld() {
        return "hello " + Controller2.class.getSimpleName();
    }


    @RequestMapping("/session")
    public String helloworld2(Session session) {
        return "hello " + session.getSessionId();
    }

    @RequestMapping("/session/:abc")
    public String helloworld3(@PathParam("abc") String a, Session session) {
        return "hello " + a;
    }

    @RequestMapping("/object")
    public String object(TestParam param) {
        return "hello " + param.getParam1();
    }

}
