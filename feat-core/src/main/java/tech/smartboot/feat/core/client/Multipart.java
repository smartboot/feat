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

import java.io.File;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class Multipart {
    public static FormItemMultipart newFormMultipart(String name, String value) {
        return new FormItemMultipart(name, value);
    }

    public static FileItemMultipart newFileMultipart(String name, File file) {
        return new FileItemMultipart(name,file);
    }

    abstract void write(PostBody post);
}
