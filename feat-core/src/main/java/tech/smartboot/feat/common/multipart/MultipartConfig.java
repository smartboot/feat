package tech.smartboot.feat.common.multipart;


import tech.smartboot.feat.common.utils.StringUtils;

import java.nio.file.Paths;

public class MultipartConfig {

    private String location;
    private long maxFileSize;
    private long maxRequestSize;
    private int fileSizeThreshold;

    public MultipartConfig() {
    }

    /**
     * Constructs an instance with all values specified.
     *
     * @param location          the directory location where files will be stored
     * @param maxFileSize       the maximum size allowed for uploaded files
     * @param maxRequestSize    the maximum size allowed for multipart/form-data requests
     * @param fileSizeThreshold the size threshold after which files will be written to disk
     */
    public MultipartConfig(String location, long maxFileSize, long maxRequestSize, int fileSizeThreshold) {
        if (location == null) {
            this.location = "";
        } else {
            this.location = location;
        }
        if (StringUtils.isNotBlank(this.location) && !Paths.get(this.location).isAbsolute()) {
            throw new IllegalStateException("location must be absolute");
        }

        this.maxFileSize = maxFileSize;
        this.maxRequestSize = maxRequestSize;
        this.fileSizeThreshold = fileSizeThreshold;
    }


    /**
     * Gets the directory location where files will be stored.
     *
     * @return the directory location where files will be stored
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Gets the maximum size allowed for uploaded files.
     *
     * @return the maximum size allowed for uploaded files
     */
    public long getMaxFileSize() {
        return this.maxFileSize;
    }

    /**
     * Gets the maximum size allowed for multipart/form-data requests.
     *
     * @return the maximum size allowed for multipart/form-data requests
     */
    public long getMaxRequestSize() {
        return this.maxRequestSize;
    }

    /**
     * Gets the size threshold after which files will be written to disk. A value of zero means files must always be written
     * to disk.
     *
     * @return the size threshold after which files will be written to disk
     */
    public int getFileSizeThreshold() {
        return this.fileSizeThreshold;
    }
}
