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
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
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