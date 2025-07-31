package tech.smartboot.feat.demo.benchmark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SpringBootApp {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        SpringApplication.run(SpringBootApp.class, args);
        System.out.println("启动时间：" + (System.currentTimeMillis() - start));
    }

    @RestController
    public static class HelloController {
        @GetMapping("/hello")
        public String hello() {
            return "Hello World!";
        }

        @GetMapping("/json")
        public Response json() {
            return new Response("Hello", "World");
        }
    }


} 