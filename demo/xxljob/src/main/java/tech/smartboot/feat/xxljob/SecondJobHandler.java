package tech.smartboot.feat.xxljob;

import com.xxl.job.core.handler.IJobHandler;
import tech.smartboot.feat.cloud.annotation.Bean;

@Bean
public class SecondJobHandler extends IJobHandler {

    @Override
    public void execute() throws Exception {
        System.out.println("execute second job");
    }
}
