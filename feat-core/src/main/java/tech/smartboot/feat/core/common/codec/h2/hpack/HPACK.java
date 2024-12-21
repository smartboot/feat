/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package tech.smartboot.feat.core.common.codec.h2.hpack;

import java.nio.ByteBuffer;

/**
 * Internal utilities and stuff.
 */
public final class HPACK {


    private HPACK() {
    }

    // -- low-level utilities --

    @FunctionalInterface
    interface BufferUpdateConsumer {
        void accept(long data, int len);
    }

    @SuppressWarnings("fallthrough")
    public static int read(ByteBuffer source,
                           long buffer,
                           int bufferLen,
                           BufferUpdateConsumer consumer) {
        // read as much as possible (up to 8 bytes)
        int nBytes = Math.min((64 - bufferLen) >> 3, source.remaining());
        switch (nBytes) {
            case 0:
                break;
            case 3:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 2:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 1:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
                consumer.accept(buffer, bufferLen);
                break;
            case 7:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 6:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 5:
                buffer |= ((source.get() & 0x00000000000000ffL) << (56 - bufferLen));
                bufferLen += 8;
            case 4:
                buffer |= ((source.getInt() & 0x00000000ffffffffL) << (32 - bufferLen));
                bufferLen += 32;
                consumer.accept(buffer, bufferLen);
                break;
            case 8:
                buffer = source.getLong();
                bufferLen = 64;
                consumer.accept(buffer, bufferLen);
                break;
            default:
                throw new InternalError(String.valueOf(nBytes));
        }
        return nBytes;
    }

    // The number of bytes that can be written at once
    // (calculating in bytes, not bits, since
    //  destination.remaining() * 8 might overflow)
    @SuppressWarnings("fallthrough")
    public static int write(long buffer,
                            int bufferLen,
                            BufferUpdateConsumer consumer,
                            ByteBuffer destination) {
        int nBytes = Math.min(bufferLen >> 3, destination.remaining());
        switch (nBytes) {
            case 0:
                break;
            case 3:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 2:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 1:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
                consumer.accept(buffer, bufferLen);
                break;
            case 7:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 6:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 5:
                destination.put((byte) (buffer >>> 56));
                buffer <<= 8;
                bufferLen -= 8;
            case 4:
                destination.putInt((int) (buffer >>> 32));
                buffer <<= 32;
                bufferLen -= 32;
                consumer.accept(buffer, bufferLen);
                break;
            case 8:
                destination.putLong(buffer);
                buffer = 0;
                bufferLen = 0;
                consumer.accept(buffer, bufferLen);
                break;
            default:
                throw new InternalError(String.valueOf(nBytes));
        }
        return nBytes;
    }

    /*
     * Returns the number of bytes the given number of bits constitute.
     */
    static int bytesForBits(int n) {
        assert (n / 8 + (n % 8 != 0 ? 1 : 0)) == (n + 7) / 8
                && (n + 7) / 8 == ((n + 7) >> 3) : n;
        return (n + 7) >> 3;
    }
}
