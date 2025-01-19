package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.Feat;
import tech.smartboot.feat.core.apt.ApplicationContext;
import tech.smartboot.feat.core.apt.annotation.Controller;
import tech.smartboot.feat.core.apt.annotation.RequestMapping;
import tech.smartboot.feat.core.apt.annotation.RequestMethod;
import tech.smartboot.feat.core.server.handler.Router;
import tech.smartboot.feat.restful.RestfulBootstrap;

import java.util.concurrent.Executors;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/27
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
//    @RequestMapping("/upload")
//    public String upload(@Param("text") MultipartFile file, @Param("name") String name) {
//        return "aa";
//    }

    public static void main(String[] args) throws Exception {
        Router router = new Router();
        ApplicationContext context = new ApplicationContext();
        context.start(router);
        Feat.createHttpServer().httpHandler(router).listen(8080);
    }
}
