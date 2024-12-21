/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: FileSmartHttp.java
 * Date: 2021-06-20
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo;

import tech.smartboot.feat.server.HttpBootstrap;
import tech.smartboot.feat.server.handler.HttpStaticResourceHandler;

/**
 * 打开浏览器请求：http://127.0.0.0:8080/
 *
 * @author 三刀
 * @version V1.0 , 2019/11/3
 */
public class FileSmartHttp {
    public static void main(String[] args) {
        String webdir = System.getProperty("user.dir") + "/assembly/webapps";
        HttpBootstrap bootstrap = new HttpBootstrap();
        //配置HTTP消息处理管道
        bootstrap.httpHandler(new HttpStaticResourceHandler(webdir));

        //设定服务器配置并启动
        bootstrap.start();
    }
}
