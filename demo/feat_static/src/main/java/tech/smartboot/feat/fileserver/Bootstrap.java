/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.fileserver;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Bootstrap {
    public static void main(String[] args) throws IOException {
        boolean autoIndex = "true".equals(System.getenv("FEAT_AUTO_INDEX"));
        String webRoot = System.getenv("FEAT_WEB_ROOT");
        Path path;
        if (FeatUtils.isBlank(webRoot)) {
            webRoot = "./webroot";
            path = Paths.get(webRoot);
            if (!path.toFile().isDirectory()) {
                System.err.println("System env:FEAT_WEB_ROOT is empty, and there is no default web resource dir: " + path.toFile().getCanonicalPath());
                return;
            }
        } else {
            path = Paths.get(webRoot);
        }

        if (!path.toFile().isDirectory()) {
            System.err.println("system env FEAT_WEB_ROOT:[" + path.toFile().getCanonicalPath() + "] is not a directory");
            return;
        }
        String baseDir = path.toFile().getCanonicalPath();
        Feat.httpServer().httpHandler(new HttpStaticResourceHandler(opt -> opt.autoIndex(autoIndex).baseDir(baseDir))).listen();
    }

}
