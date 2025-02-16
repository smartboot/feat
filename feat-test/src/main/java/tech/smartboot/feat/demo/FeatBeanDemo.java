package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;

@Bean
public class FeatBeanDemo {
    @Autowired
    private String hello;

    @PostConstruct
    public void init() {
        System.out.println(hello);
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(opts -> opts.addExternalBean("hello", "你好~")).listen();
    }

    public void setHello(String hello) {
        this.hello = hello;
    }
}
