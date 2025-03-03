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

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
@Controller
public class RestfulDemo {

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String helloworld() {
        return "hello world";
    }

    @RequestMapping("/")
    public String index() {
        return ("<html>" +
                "<head><title>feat demo</title></head>" +
                "<body>" +
                "GET 表单提交<form action='/get' method='get'><input type='text' name='text'/><input type='submit'/></form></br>" +
                "POST 表单提交<form action='/post' method='post'><input type='text' name='text'/><input type='submit'/></form></br>" +
                "文件上传<form action='/upload' method='post' enctype='multipart/form-data'>表单name:<input type='text' name='name'/> <input type='file' name='text'/><input type='submit'/></form></br>" +
                "</body></html>");
    }

    @RequestMapping(value = "/async", async = true)
    public String async() throws InterruptedException {
        int sleep = (int) (Math.random() * 5000);
        Thread.sleep(sleep);
        return "sleep: " + sleep + "ms";
    }


    public static void main(String[] args) throws Exception {
        //扫描指定路径下的Controller、Bean
        FeatCloud.cloudServer(opts -> opts.setPackages("tech.smartboot.feat.demo.apt")).listen();
    }
}
