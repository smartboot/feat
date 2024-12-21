package tech.smartboot.feat.core.common;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class Part {
    private String contentType;
    private String contentDisposition;
    private String fileName;
    private String name;
    private boolean isFile = false;
    private transient File tempFile;
    private FileOutputStream fos;
    private int sizeThreshold = 1024 * 8;
    private byte[] cachedContent = new byte[sizeThreshold];;
    private int cachedContentLength;
    private List<HeaderValue> headers;

    public Part(){
    }

    public InputStream getInputStream() throws IOException {
        if (isFile){
            return Files.newInputStream(tempFile.toPath());
        }
        return new ByteArrayInputStream(cachedContent, 0, cachedContentLength);
    }

    public boolean delete(){
        if (isFile && Files.exists(tempFile.toPath())){
            return tempFile.delete();
        }
        cachedContent = null;
        return true;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setIsFile(boolean file) {
        isFile = file;
    }

    public int getSizeThreshold() {
        return sizeThreshold;
    }

    public void setSizeThreshold(int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public byte[] getCachedContent() {
        return cachedContent;
    }

    public void setCachedContent(byte[] cachedContent) {
        this.cachedContent = cachedContent;
    }

    public File getTempFile() {
        return tempFile;
    }

    public void setTempFile(File tempFile) {
        this.tempFile = tempFile;
    }

    public FileOutputStream getFos() {
        return fos;
    }

    public void setFos(FileOutputStream fos) {
        this.fos = fos;
    }

    public List<HeaderValue> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HeaderValue> headers) {
        this.headers = headers;
    }

    public int getCachedContentLength() {
        return cachedContentLength;
    }

    public void setCachedContentLength(int cachedContentLength) {
        this.cachedContentLength = cachedContentLength;
    }
}