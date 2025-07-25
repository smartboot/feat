/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.controller;


import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.router.Interceptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@Controller(value = "controller1", gzip = true)
public class Controller1 {
    @RequestMapping("/helloworld")
    public String helloworld() {
        return "hello " + Controller1.class.getSimpleName();
    }

    @RequestMapping("/test2")
    public RestResult<List<Map<String, String>>> hello11ss2(HttpRequest request, HttpResponse response) {
        RestResult<List<Map<String, String>>> result = new RestResult<>();
        result.setData(Collections.singletonList(Collections.singletonMap("hello", "world")));
        return result;
    }

    @RequestMapping("/big")
    public RestResult<String> hello11ssss2(HttpRequest request, HttpResponse response) {
        response.setHeader(HeaderName.CONNECTION, "keep-alive");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            stringBuilder.append("hello world");
        }
        return RestResult.ok(stringBuilder.toString());
    }

    @InterceptorMapping({"/controller1/*"})
    public Interceptor interceptor() {
        return (context, completableFuture, chain) -> {
            System.out.println("interceptor...");
            chain.proceed(context, completableFuture);
        };
    }
}
