/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class FormItemMultipart extends Multipart {
    private final String name;
    private final String value;

    FormItemMultipart(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    void write(PostBody post) {
        post.write("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n");
    }
}
