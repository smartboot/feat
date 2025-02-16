package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

@Controller
public class FeatCloudDemo {
    @RequestMapping("/cloud")
    public String helloWorld() {
        return "hello Feat Cloud";
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer().listen();
    }
}
