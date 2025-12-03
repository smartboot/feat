/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package tech.smartboot.feat.core.client.stream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * 此类实现了一个流过滤器，用于读取 GZIP 文件格式的压缩数据。
 * <p>
 * 此类是从 JDK 源代码复制的，除了将 InflaterInputStream 替换为本地实现外，
 * 没有做其他修改。
 *
 * @author David Connelly, 三刀
 * @see InflaterInputStream
 */
public class GZIPInputStream extends InflaterInputStream {
    /**
     * CRC-32 for uncompressed data.
     */
    protected CRC32 crc = new CRC32();
    private final CheckedInputStream headInput;
    /**
     * Indicates end of input stream.
     */
    protected boolean eos;

    private boolean closed = false;
    private static final int STATE_MAGIC = 0;
    private static final int STATE_COMPRESSION_METHOD = 1;
    private static final int STATE_FLAGS = 2;
    private static final int STATE_FEXTRA_LEN = 3;
    private static final int STATE_FEXTRA_DATA = 4;
    private static final int STATE_FNAME = 5;
    private static final int STATE_FCOMMENT = 6;
    private static final int STATE_HCRC = 7;
    private static final int STATE_INFLATE = 10;
    private static final int STATE_CRC_CHECK = 20;

    private int state = STATE_MAGIC;
    private int flags;
    private int extraReaming;

    /**
     * Check to make sure that this stream has not been closed
     */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param in   the input stream
     * @param size the input buffer size
     * @throws ZipException             if a GZIP format error has occurred or the
     *                                  compression method used is unsupported
     * @throws IOException              if an I/O error has occurred
     * @throws IllegalArgumentException if {@code size <= 0}
     */
    public GZIPInputStream(InputStream in, int size) throws IOException {
        super(in, new Inflater(true), size);
        usesDefaultInflater = true;
        headInput = new CheckedInputStream(in, crc);
    }

    /**
     * Creates a new input stream with a default buffer size.
     *
     * @param in the input stream
     * @throws ZipException if a GZIP format error has occurred or the
     *                      compression method used is unsupported
     * @throws IOException  if an I/O error has occurred
     */
    public GZIPInputStream(InputStream in) throws IOException {
        this(in, 512);
    }

    /**
     * Reads uncompressed data into an array of bytes. If <code>len</code> is not
     * zero, the method will block until some input can be decompressed; otherwise,
     * no bytes are read and <code>0</code> is returned.
     *
     * @param buf the buffer into which the data is read
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the
     * compressed input stream is reached
     * @throws NullPointerException      If <code>buf</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>buf.length - off</code>
     * @throws ZipException              if the compressed input data is corrupt.
     * @throws IOException               if an I/O error has occurred.
     *
     */
    public int read(byte[] buf, int off, int len) throws IOException {
        ensureOpen();
        if (eos) {
            return -1;
        }
        switch (state) {
            case STATE_MAGIC: {
                if (headInput.available() < 2) {
                    return 0;
                }
                // Check header magic
                if (readUShort(headInput) != GZIP_MAGIC) {
                    throw new ZipException("Not in GZIP format");
                }
                state = STATE_COMPRESSION_METHOD;
            }
            case STATE_COMPRESSION_METHOD: {
                if (headInput.available() < 1) {
                    return 0;
                }
                // Check compression method
                if (readUByte(headInput) != 8) {
                    throw new ZipException("Unsupported compression method");
                }
            }
            case STATE_FLAGS: {
                if (headInput.available() < 7) {
                    return 0;
                }
                flags = readUByte(headInput);
                // Skip extra flags and modification time
                skipBytes(headInput, 6);
                if ((flags & FEXTRA) == FEXTRA) {
                    state = STATE_FEXTRA_LEN;
                } else {
                    state = STATE_FNAME;
                    return this.read(buf, off, len);
                }
            }
            case STATE_FEXTRA_LEN: {
                if (headInput.available() < 2) {
                    return 0;
                }
                extraReaming = readUShort(headInput);
                state = STATE_FEXTRA_DATA;
            }
            case STATE_FEXTRA_DATA: {
                if (extraReaming > 0) {
                    if (headInput.available() == 0) {
                        return 0;
                    }
                    int n = Math.min(extraReaming, headInput.available());
                    skipBytes(headInput, n);
                    extraReaming -= n;
                    return this.read(buf, off, len);
                } else if (extraReaming == 0) {
                    state = STATE_FNAME;
                } else {
                    throw new ZipException("Invalid extra data size");
                }
            }
            case STATE_FNAME: {
                if ((flags & FNAME) == FNAME) {
                    while (headInput.available() > 0) {
                        int c = headInput.read();
                        if (c == -1) {
                            throw new ZipException("Unexpected end of stream");
                        } else if (c == 0) {
                            state = STATE_FCOMMENT;
                            return this.read(buf, off, len);
                        }
                    }
                    return 0;
                } else {
                    state = STATE_FCOMMENT;
                }
            }
            case STATE_FCOMMENT: {
                if ((flags & FCOMMENT) == FCOMMENT) {
                    while (headInput.available() > 0) {
                        int c = headInput.read();
                        if (c == -1) {
                            throw new ZipException("Unexpected end of stream");
                        } else if (c == 0) {
                            state = STATE_HCRC;
                            return this.read(buf, off, len);
                        }
                    }
                    return 0;
                } else {
                    state = STATE_HCRC;
                }
            }
            case STATE_HCRC: {
                if ((flags & FHCRC) == FHCRC) {
                    if (headInput.available() < 2) {
                        return 0;
                    }
                    int v = (int) crc.getValue() & 0xffff;
                    if (readUShort(headInput) != v) {
                        throw new ZipException("Corrupt GZIP header");
                    }
                }
                state = STATE_INFLATE;
                crc.reset();
            }
            case STATE_INFLATE: {
                int n = super.read(buf, off, len);
                if (n == -1) {
                    state = STATE_CRC_CHECK;
                } else {
                    crc.update(buf, off, n);
                    return n;
                }
            }
            case STATE_CRC_CHECK: {
                if ((inf.getRemaining() + in.available()) < 8) {
                    return 0;
                } else if (readTrailer()) {
                    eos = true;
                    return -1;
                } else {
                    //同JDK源码不一致，此处中断阻塞
                    return this.read(buf, off, len);
                }
            }
        }
        throw new IllegalStateException();
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (!closed) {
            super.close();
            eos = true;
            closed = true;
        }
    }

    /**
     * GZIP header magic number.
     */
    public final static int GZIP_MAGIC = 0x8b1f;

    /*
     * File header flags.
     */
    private final static int FTEXT = 1;    // Extra text
    private final static int FHCRC = 2;    // Header CRC
    private final static int FEXTRA = 4;    // Extra field
    private final static int FNAME = 8;    // File name
    private final static int FCOMMENT = 16;   // File comment

    /*
     * Reads GZIP member trailer and returns true if the eos
     * reached, false if there are more (concatenated gzip
     * data set)
     */
    private boolean readTrailer() throws IOException {
        InputStream in = this.in;
        int n = inf.getRemaining();
        if (n > 0) {
            in = new SequenceInputStream(new ByteArrayInputStream(buf, len - n, n), new FilterInputStream(in) {
                public void close() throws IOException {
                }
            });
        }
        // Uses left-to-right evaluation order
        if ((readUInt(in) != crc.getValue()) ||
                // rfc1952; ISIZE is the input size modulo 2^32
                (readUInt(in) != (inf.getBytesWritten() & 0xffffffffL))) throw new ZipException("Corrupt GZIP trailer");

        // If there are more bytes available in "in" or
        // the leftover in the "inf" is > 26 bytes:
        // this.trailer(8) + next.header.min(10) + next.trailer(8)
        // try concatenated case
        if (this.in.available() > 0 || n > 26) {
            throw new UnsupportedEncodingException("Concatenated gzip streams not supported");
//            int m = 8;                  // this.trailer
//            try {
//                m += readHeader(in);    // next.header
//            } catch (IOException ze) {
//                return true;  // ignore any malformed, do nothing
//            }
//            inf.reset();
//            if (n > m) inf.setInput(buf, len - n + m, n - m);
//            return false;
        }
        return true;
    }

    /*
     * Reads unsigned integer in Intel byte order.
     */
    private long readUInt(InputStream in) throws IOException {
        long s = readUShort(in);
        return ((long) readUShort(in) << 16) | s;
    }

    /*
     * Reads unsigned short in Intel byte order.
     */
    private int readUShort(InputStream in) throws IOException {
        int b = readUByte(in);
        return (readUByte(in) << 8) | b;
    }

    /*
     * Reads unsigned byte.
     */
    private int readUByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    private final static byte[] tmpbuf = new byte[128];

    /*
     * Skips bytes of input data blocking until all bytes are skipped.
     * Does not assume that the input stream is capable of seeking.
     */
    private void skipBytes(InputStream in, int n) throws IOException {
        while (n > 0) {
            int len = in.read(tmpbuf, 0, Math.min(n, tmpbuf.length));
            if (len == -1) {
                throw new EOFException();
            }
            n -= len;
        }
    }
}
