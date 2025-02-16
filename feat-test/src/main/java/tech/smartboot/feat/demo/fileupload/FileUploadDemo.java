/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: FileUploadDemo.java
 * Date: 2020-04-03
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.demo.fileupload;

import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.router.Router;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author 三刀
 * @version V1.0 , 2019/11/24
 */
public class FileUploadDemo {
    public static void main(String[] args) {

        Router routeHandler = new Router();
        routeHandler.route("/", new HttpHandler() {
                    byte[] body = ("<html>" +
                            "<head><title>feat demo</title></head>" +
                            "<body>" +
                            "GET 表单提交<form action='/get' method='get'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "POST 表单提交<form action='/post' method='post'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "文件上传<form action='/upload' method='post' enctype='multipart/form-data'><input type='file' name='text'/><input type='submit'/></form></br>" +
                            "</body></html>").getBytes();

                    @Override
                    public void handle(HttpRequest request) throws IOException {
                        HttpResponse response=request.getResponse();
                        response.setContentLength(body.length);
                        response.getOutputStream().write(body);
                    }
                })
                .route("/upload", request -> {
                    try {
                        for (Part part : request.getParts()) {
                            String name = part.getName();
                            InputStream stream = part.getInputStream();
                            if (part.getSubmittedFileName() == null) {
                                System.out.println("Form field " + name + " with value "
                                        + stream + " detected.");
                            } else {
                                System.out.println("File field " + name + " with file name "
                                        + part.getName() + " detected.");
                                // Process the input stream
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });


        HttpServer bootstrap = new HttpServer();
        bootstrap.options().readBufferSize(1024 * 1024).debug(true);
        //配置HTTP消息处理管道
        bootstrap.httpHandler(routeHandler);

        //设定服务器配置并启动
        bootstrap.listen();
    }
}
