package tech.smartboot.feat.fileserver;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Bootstrap {
    public static void main(String[] args) throws IOException {
        boolean autoIndex = "true".equals(System.getenv("FEAT_AUTO_INDEX"));
        String webRoot = System.getenv("FEAT_WEB_ROOT");
        Path path;
        if (StringUtils.isBlank(webRoot)) {
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
        Feat.fileServer(opts -> opts.autoIndex(autoIndex).baseDir(baseDir)).listen();
    }
}
