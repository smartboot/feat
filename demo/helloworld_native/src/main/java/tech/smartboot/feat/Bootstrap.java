/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat;

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

/**
 * Feat框架的启动引导类
 * 该类演示了如何使用FeatCloud框架创建一个简单的Web应用程序
 *
 * @author smartboot
 * @version 1.0
 */
@Controller
public class Bootstrap {

    /**
     * 处理HTTP GET请求的控制器方法
     * 当访问路径"/hello"时，会返回"hello Feat Cloud"字符串
     *
     * @return 响应字符串"hello Feat Cloud"
     */
    @RequestMapping("/hello")
    public String helloWorld() {
        return "hello Feat Cloud";
    }

    /**
     * 程序入口点
     * 启动FeatCloud服务器并开始监听请求
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 创建并启动FeatCloud服务器
        FeatCloud.cloudServer().listen();
    }
}