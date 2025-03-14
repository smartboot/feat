/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo;

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

/**
 * @author 三刀
 * @version v1.0.0
 */
@Controller
public class FeatCloudDemo {
    @RequestMapping("/cloud")
    public String helloWorld() {
        return "hello Feat Cloud";
    }

    @RequestMapping("/cloud2")
    public A helloWorld2() {
        A a = new A();
        a.setA("123\"adf\"");
        return a;
    }

    @PreDestroy
    public void destroy() {
        System.out.println("destroy...");
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer().listen();
    }

    public class A {
        String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }
}
