/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.fileupload;

import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Context;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.RouterHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class FormDataDemo {
    public static void main(String[] args) {

        Router routeHandler = new Router();
        routeHandler.route("/", new RouterHandler() {
                    byte[] body = ("<html>" +
                            "<head><title>feat demo</title></head>" +
                            "<body>" +
                            "GET 表单提交<form action='/get' method='get'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "POST 表单提交<form action='/post' method='post'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "文件上传<form action='/upload' method='post' enctype='multipart/form-data'><input type='file' name='text'/><input type='submit'/></form></br>" +
                            "</body></html>").getBytes();

                    @Override
                    public void handle(Context ctx) throws IOException {
                        HttpResponse response = ctx.Response;
                        response.setContentLength(body.length);
                        response.getOutputStream().write(body);
                    }
                })
                .route("/upload", ctx -> {
                    try {
                        for (Part item : ctx.Request.getParts()) {
                            String name = item.getName();
                            System.out.println("name = " + name);
                            InputStream inputStream = item.getInputStream();
                            if (item.getSubmittedFileName() != null) {
                                System.out.println("filename = " + item.getSubmittedFileName());
                                //保存到指定路径
                                Path filePath = Paths.get("feat-test", "src", "main", "resources").resolve(item.getSubmittedFileName());
                                Files.createDirectories(filePath.getParent());
                                Files.copy(inputStream, filePath);
                                item.delete();
                            } else {
                                //打印inputStream
                                try (Scanner scanner = new Scanner(inputStream)) {
                                    while (scanner.hasNextLine()) {
                                        System.out.println(scanner.nextLine());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });


        HttpServer bootstrap = new HttpServer();
        //配置HTTP消息处理管道
        bootstrap.httpHandler(routeHandler);

        //设定服务器配置并启动
        bootstrap.listen();
    }

}
