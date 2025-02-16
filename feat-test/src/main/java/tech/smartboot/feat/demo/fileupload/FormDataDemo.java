package tech.smartboot.feat.demo.fileupload;

import tech.smartboot.feat.core.common.multipart.Part;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.router.Router;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 **/
public class FormDataDemo {
    public static void main(String[] args) {

        Router routeHandler = new Router();
        routeHandler.http("/", new HttpHandler() {
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
                .http("/upload", request -> {
                    try {
                        for (Part item : request.getParts()) {
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
