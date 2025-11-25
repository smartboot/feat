/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;


public class DeflaterInputStream extends FilterInputStream {
    /**
     * Compressor for this stream.
     */
    protected final Deflater def;

    /**
     * Input buffer for reading compressed data.
     */
    protected final byte[] buf;


    /**
     * Default compressor is used.
     */
    private boolean usesDefaultDeflater = false;


    /**
     * Check to make sure that this stream has not been closed.
     */
    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }


    public DeflaterInputStream(InputStream in) {
        this(in, new Deflater());
        usesDefaultDeflater = true;
    }


    public DeflaterInputStream(InputStream in, Deflater defl) {
        this(in, defl, 512);
    }

    public DeflaterInputStream(InputStream in, Deflater defl, int bufLen) {
        super(in);

        // Sanity checks
        if (in == null)
            throw new NullPointerException("Null input");
        if (defl == null)
            throw new NullPointerException("Null deflater");
        if (bufLen < 1)
            throw new IllegalArgumentException("Buffer size < 1");

        // Initialize
        def = defl;
        buf = new byte[bufLen];
    }


    public void close() throws IOException {
        if (in != null) {
            try {
                // Clean up
                if (usesDefaultDeflater) {
                    def.end();
                }

                in.close();
            } finally {
                in = null;
            }
        }
    }


    public int read() throws IOException {
        throw new UnsupportedOperationException("Reading single bytes not supported");
    }


    public int read(byte[] b, int off, int len) throws IOException {
        // Sanity checks
        ensureOpen();
        if (b == null) {
            throw new NullPointerException("Null buffer for read");
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        // Read and compress (deflate) input data bytes
        int cnt = 0;
        while (len > 0 && !def.finished()) {
            int n;

            // Read data from the input stream
            if (def.needsInput()) {
                n = in.read(buf, 0, buf.length);
                if (n < 0) {
                    // End of the input stream reached
                    def.finish();
                } else if (n > 0) {
                    def.setInput(buf, 0, n);
                } else {
                    break;
                }
            }

            // Compress the input data, filling the read buffer
            n = def.deflate(b, off, len);
            cnt += n;
            off += n;
            len -= n;
        }
        if (cnt == 0 && def.finished()) {
            cnt = -1;
        }

        return cnt;
    }


    public long skip(long n) {
        throw new UnsupportedOperationException("Skip not supported");
    }


    public int available() {
        throw new UnsupportedOperationException("Available not supported");
    }


    public boolean markSupported() {
        return false;
    }


    public void mark(int limit) {
        throw new UnsupportedOperationException("mark/reset not supported");
    }


    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
