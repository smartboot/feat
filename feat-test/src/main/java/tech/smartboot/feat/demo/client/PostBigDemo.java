/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.client;

import tech.smartboot.feat.core.client.HttpClient;

import java.util.HashMap;
import java.util.Map;

public class PostBigDemo {
    public static void main(String[] args) throws Exception {


        //---------------

        Map<String, String> params = new HashMap<String, String>();
        StringBuilder bigParam = new StringBuilder();
        while (bigParam.length() < 2) {
//            while (bigParam.length() < 1024 * 1024 * 2) {
            bigParam.append("123456789abcdef;");
        }
        params.put("demo", bigParam.toString());

        HttpClient client = new HttpClient("localhost", 8080);
        client.post("/demo").body().formUrlencoded(params)
                .onFailure(e -> {
                    System.out.println("resp err: ");
                    e.printStackTrace();
                }).onSuccess(resp -> {
                    System.out.println("resp code: " + resp.statusCode());
                })
                .submit();


    }
}