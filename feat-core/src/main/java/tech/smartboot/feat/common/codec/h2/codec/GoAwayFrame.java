/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package tech.smartboot.feat.common.codec.h2.codec;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GoAwayFrame extends Http2Frame {
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_DEBUG_DATA = 1;
    private int state = STATE_DEFAULT;
    private int lastStream;
    private int errorCode;
    private byte[] debugData;

    public GoAwayFrame(int streamId, int flags, int remaining) {
        super(streamId, flags, remaining);
    }


    @Override
    public boolean decode(ByteBuffer buffer) {
        if (finishDecode()) {
            return true;
        }
        switch (state) {
            case STATE_DEFAULT:
                if (buffer.remaining() < 8) {
                    return false;
                }
                lastStream = buffer.getInt();
                errorCode = buffer.getInt();
                remaining -= 8;
                state = STATE_DEBUG_DATA;
            case STATE_DEBUG_DATA:
                if (remaining > 0) {
                    if (buffer.remaining() < remaining) {
                        return false;
                    }
                    debugData = new byte[remaining];
                    buffer.get(debugData);
                } else {
                    debugData = new byte[0];
                }
                remaining = 0;
                break;
            default:
                throw new IllegalStateException();
        }
        checkEndRemaining();
        return true;
    }

    @Override
    public int type() {
        return FRAME_TYPE_GOAWAY;
    }


    @Override
    public String toString() {
        return super.toString() + " Debugdata: " + new String(debugData, UTF_8);
    }

    public int getLastStream() {
        return this.lastStream;
    }

    public byte[] getDebugData() {
        return debugData.clone();
    }

}
