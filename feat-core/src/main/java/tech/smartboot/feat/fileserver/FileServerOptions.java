package tech.smartboot.feat.fileserver;

import tech.smartboot.feat.core.server.ServerOptions;

public class FileServerOptions extends ServerOptions {
    private String baseDir = "./";
    /**
     * 若设置为true，则允许以目录列表的形式展现
     */
    private boolean autoIndex = false;

    public String baseDir() {
        return baseDir;
    }

    public FileServerOptions baseDir(String baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public boolean autoIndex() {
        return autoIndex;
    }

    public FileServerOptions autoIndex(boolean autoIndex) {
        this.autoIndex = autoIndex;
        return this;
    }
}
