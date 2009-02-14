/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.marshalling.river;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.UTFUtils;
import java.io.IOException;

/**
 *
 */
public final class BlockMarshaller implements Marshaller {
    private final RiverMarshaller riverMarshaller;
    private final byte[] buffer;
    private int position;

    BlockMarshaller(RiverMarshaller riverMarshaller, int blockSize) {
        this.riverMarshaller = riverMarshaller;
        buffer = new byte[blockSize];
    }

    public void start(final ByteOutput newOutput) throws IOException {
        throw new IllegalStateException("start() not allowed in this context");
    }

    public void clearInstanceCache() throws IOException {
        throw new IllegalStateException("clearInstanceCache() not allowed in this context");
    }

    public void clearClassCache() throws IOException {
        throw new IllegalStateException("clearClassCache() not allowed in this context");
    }

    public void finish() throws IOException {
        throw new IllegalStateException("finish() not allowed in this context");
    }

    public void writeObject(final Object obj) throws IOException {
        doWriteObject(obj, false);
    }

    public void writeObjectUnshared(final Object obj) throws IOException {
        doWriteObject(obj, true);
    }

    private void doWriteObject(final Object obj, final boolean unshared) throws IOException {
        flush();
        riverMarshaller.doWriteObject(obj, unshared);
        flush();
    }

    public void write(final int v) throws IOException {
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining == 0) {
            flush();
            buffer[0] = (byte) v;
            position = 1;
        } else {
            buffer[position++] = (byte) v;
        }
    }

    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(final byte[] bytes, final int off, final int len) throws IOException {
        if (len < 1) {
            return;
        }
        final int bl = buffer.length;
        final int position = this.position;
        if (len > bl - position || len > bl >> 1) {
            flush();
            if (len < 256) {
                riverMarshaller.write(Protocol.ID_START_BLOCK_SMALL);
                riverMarshaller.writeByte(len);
                riverMarshaller.write(bytes, off, len);
            } else if (len < 65536) {
                riverMarshaller.write(Protocol.ID_START_BLOCK_MEDIUM);
                riverMarshaller.writeShort(len);
                riverMarshaller.write(bytes, off, len);
            } else {
                riverMarshaller.write(Protocol.ID_START_BLOCK_LARGE);
                riverMarshaller.writeInt(len);
                riverMarshaller.write(bytes, off, len);
            }
        } else {
            System.arraycopy(bytes, off, buffer, position, len);
            this.position = position + len;
        }
    }

    public void writeBoolean(final boolean v) throws IOException {
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining == 0) {
            flush();
            buffer[0] = (byte) (v ? 1 : 0);
            position = 1;
        } else {
            buffer[position++] = (byte) (v ? 1 : 0);
        }
    }

    public void writeByte(final int v) throws IOException {
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining == 0) {
            flush();
            buffer[0] = (byte) v;
            position = 1;
        } else {
            buffer[position++] = (byte) v;
        }
    }

    public void writeShort(final int v) throws IOException {
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining < 2) {
            flush();
            buffer[0] = (byte) (v >> 8);
            buffer[1] = (byte) v;
            position = 2;
        } else {
            final int s = position;
            position = s + 2;
            buffer[s]   = (byte) (v >> 8);
            buffer[s+1] = (byte) v;
        }
    }

    public void writeChar(final int v) throws IOException {
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining < 2) {
            flush();
            buffer[0] = (byte) (v >> 8);
            buffer[1] = (byte) v;
            position = 2;
        } else {
            final int s = position;
            position = s + 2;
            buffer[s]   = (byte) (v >> 8);
            buffer[s+1] = (byte) v;
        }
    }

    public void writeInt(final int v) throws IOException {
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining < 4) {
            flush();
            buffer[0] = (byte) (v >> 24);
            buffer[1] = (byte) (v >> 16);
            buffer[2] = (byte) (v >> 8);
            buffer[3] = (byte) v;
            position = 4;
        } else {
            final int s = position;
            position = s + 4;
            buffer[s]   = (byte) (v >> 24);
            buffer[s+1] = (byte) (v >> 16);
            buffer[s+2] = (byte) (v >> 8);
            buffer[s+3] = (byte) v;
        }
    }

    public void writeLong(final long v) throws IOException {
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining < 8) {
            flush();
            buffer[0] = (byte) (v >> 56L);
            buffer[1] = (byte) (v >> 48L);
            buffer[2] = (byte) (v >> 40L);
            buffer[3] = (byte) (v >> 32L);
            buffer[4] = (byte) (v >> 24L);
            buffer[5] = (byte) (v >> 16L);
            buffer[6] = (byte) (v >> 8L);
            buffer[7] = (byte) v;
            position = 8;
        } else {
            final int s = position;
            position = s + 8;
            buffer[s]   = (byte) (v >> 56L);
            buffer[s+1] = (byte) (v >> 48L);
            buffer[s+2] = (byte) (v >> 40L);
            buffer[s+3] = (byte) (v >> 32L);
            buffer[s+4] = (byte) (v >> 24L);
            buffer[s+5] = (byte) (v >> 16L);
            buffer[s+6] = (byte) (v >> 8L);
            buffer[s+7] = (byte) v;
        }
    }

    public void writeFloat(final float v) throws IOException {
        final int bits = Float.floatToIntBits(v);
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining < 4) {
            flush();
            buffer[0] = (byte) (bits >> 24);
            buffer[1] = (byte) (bits >> 16);
            buffer[2] = (byte) (bits >> 8);
            buffer[3] = (byte) bits;
            position = 4;
        } else {
            final int s = position;
            position = s + 4;
            buffer[s]   = (byte) (bits >> 24);
            buffer[s+1] = (byte) (bits >> 16);
            buffer[s+2] = (byte) (bits >> 8);
            buffer[s+3] = (byte) bits;
        }
    }

    public void writeDouble(final double v) throws IOException {
        final long bits = Double.doubleToLongBits(v);
        final byte[] buffer = this.buffer;
        final int remaining = buffer.length - position;
        if (remaining < 8) {
            flush();
            buffer[0] = (byte) (bits >> 56L);
            buffer[1] = (byte) (bits >> 48L);
            buffer[2] = (byte) (bits >> 40L);
            buffer[3] = (byte) (bits >> 32L);
            buffer[4] = (byte) (bits >> 24L);
            buffer[5] = (byte) (bits >> 16L);
            buffer[6] = (byte) (bits >> 8L);
            buffer[7] = (byte) bits;
            position = 8;
        } else {
            final int s = position;
            position = s + 8;
            buffer[s]   = (byte) (bits >> 56L);
            buffer[s+1] = (byte) (bits >> 48L);
            buffer[s+2] = (byte) (bits >> 40L);
            buffer[s+3] = (byte) (bits >> 32L);
            buffer[s+4] = (byte) (bits >> 24L);
            buffer[s+5] = (byte) (bits >> 16L);
            buffer[s+6] = (byte) (bits >> 8L);
            buffer[s+7] = (byte) bits;
        }
    }

    public void writeBytes(final String s) throws IOException {
        final int len = s.length();
        for (int i = 0; i < len; i ++) {
            write(s.charAt(i));
        }
    }

    public void writeChars(final String s) throws IOException {
        final int len = s.length();
        for (int i = 0; i < len; i ++) {
            writeChar(s.charAt(i));
        }
    }

    public void writeUTF(final String s) throws IOException {
        // span multiple blocks, no doubt, but should be reasonably fast
        writeInt(s.length());
        UTFUtils.writeUTFBytes(this, s);
    }

    public void flush() throws IOException {
        final int position = this.position;
        if (position == 0) {
            return;
        } else if (position < 256) {
            riverMarshaller.write(Protocol.ID_START_BLOCK_SMALL);
            riverMarshaller.writeByte(position);
        } else if (position < 65536) {
            riverMarshaller.write(Protocol.ID_START_BLOCK_MEDIUM);
            riverMarshaller.writeShort(position);
        } else {
            riverMarshaller.write(Protocol.ID_START_BLOCK_LARGE);
            riverMarshaller.writeInt(position);
        }
        riverMarshaller.write(buffer, 0, position);
        this.position = 0;
    }

    public void close() throws IOException {
    }
}
