/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;

import java.io.InputStream;


/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class SimpleSmartHttp {
    public static void main(String[] args) {
        Feat.httpServer(options -> options.debug(true))
                .httpHandler(request -> {
                    InputStream in = request.getInputStream();
                    byte[] b = new byte[1024];
                    int i;
                    while ((i = in.read(b)) > 0) {
                        System.out.println(new String(b, 0, i));
                    }
                    request.getResponse().write("hello feat<br/>".getBytes());
                }).listen(8080);
    }
}