package tech.smartboot.feat.core.common;

import tech.smartboot.feat.core.common.utils.FileReleaseTracker;
import java.io.File;

/**
 * @Description: TODO
 * @Author MiSinG
 * @Date 2024/6/26
 * @Version V1.0
 **/
public class Multipart {
    public static final byte CR = 13;
    public static final byte LF = 10;
    public static final byte DASH = 45;
    public static final int HEADER_PART_SIZE_MAX = 10240;
    public static final int DEFAULT_BUF_SIZE = 4096;
    public static final byte[] HEADER_SEPARATOR = new byte[]{13, 10, 13, 10};
    public static final byte[] FIELD_SEPARATOR = new byte[]{13, 10};
    public static final byte[] STREAM_TERMINATOR = new byte[]{45, 45};
    public static final byte[] BOUNDARY_PREFIX = new byte[]{13, 10, 45, 45};
    private static final FileReleaseTracker fileCleaningTracker = new FileReleaseTracker();
    private int boundaryLength;
    private final byte[] boundary;
    private final byte[] separator;
    private int bufferPos = 0;
    private int bodyLength;
    private int separatorIndex;
    private int CRLFIndex;
    private boolean isMatch;

    public Multipart(byte[] boundary) {
        this.boundary = boundary;
        this.separator = ("--" + new String(boundary)).getBytes();
        this.boundaryLength = boundary.length;
    }

    public void addItemTracker(Part part) {
        // 跟踪 MultipartItem 的临时文件
        File tempFile = part.getTempFile();
        if (tempFile != null) {
            fileCleaningTracker.track(tempFile, part);
        }
    }

    public void stopTacking() {
        fileCleaningTracker.stop();
    }

    public int getBoundaryLength() {
        return boundaryLength;
    }

    public void setBoundaryLength(int boundaryLength) {
        this.boundaryLength = boundaryLength;
    }

    public byte[] getBoundary() {
        return boundary;
    }

    public int getBufferPos() {
        return bufferPos;
    }

    public void setBufferPos(int bufferPos) {
        this.bufferPos = bufferPos;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public int getSeparatorIndex() {
        return separatorIndex;
    }

    public void setSeparatorIndex(int separatorIndex) {
        this.separatorIndex = separatorIndex;
    }

    public int getCRLFIndex() {
        return CRLFIndex;
    }

    public void setCRLFIndex(int CRLFIndex) {
        this.CRLFIndex = CRLFIndex;
    }

    public boolean isMatch() {
        return isMatch;
    }

    public void setMatch(boolean match) {
        isMatch = match;
    }

    public byte[] getSeparator() {
        return separator;
    }
}
