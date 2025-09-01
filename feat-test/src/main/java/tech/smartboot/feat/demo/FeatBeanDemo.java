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
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@Bean
public class FeatBeanDemo {
    @Autowired
    private String hello;

    @PostConstruct
    public void init() {
        System.out.println(hello);
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(opts -> opts.registerBean("hello", "你好~").debug(true)).listen();
    }

}
