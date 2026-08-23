package tech.smartboot.feat.xxljob;

import tech.smartboot.feat.cloud.FeatCloud;

public class Bootstrap {
    public static void main(String[] args) {
        FeatCloud.cloudServer().listen(8888);
    }
}
