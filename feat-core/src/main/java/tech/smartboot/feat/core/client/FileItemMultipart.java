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

import tech.smartboot.feat.core.common.Mimetypes;

import java.io.File;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/23/25
 */
public class FileItemMultipart extends Multipart {
    private final String name;
    private final File file;

    public FileItemMultipart(String name, File file) {
        this.name = name;
        this.file = file;
    }

    @Override
    void write(PostBody post) {
        try {
            String fileName = file.getName();
            // 获取文件的MIME类型
            String contentType = Mimetypes.getInstance().getMimetype(file);

            // 写入Content-Disposition头
            post.write("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\n");
            // 写入Content-Type头
            post.write("Content-Type: " + contentType + "\r\n\r\n");

            // 读取文件内容并写入
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[8192]; // 8KB缓冲区
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    post.write(buffer, 0, bytesRead);
                }
            }

            // 写入结束换行
            post.write("\r\n");
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file content", e);
        }
    }
}
