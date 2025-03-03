/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.benchmark;

import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpResponse;

@Controller
public class FeatController {
    static byte[] body = "Hello, World!".getBytes();

    @RequestMapping("/plaintext")
    public byte[] plaintext(HttpResponse response) {
        response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
        return body;
    }

    @RequestMapping("/json")
    public Message json(HttpResponse response) {
        response.setContentType(HeaderValue.ContentType.APPLICATION_JSON_UTF8);
        return new Message("Hello, World!");
    }
}
