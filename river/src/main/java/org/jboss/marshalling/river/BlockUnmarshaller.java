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

import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.UTFUtils;
import static org.jboss.marshalling.Marshalling.createOptionalDataException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.EOFException;

/**
 *
 */
public final class BlockUnmarshaller implements Unmarshaller {

    private final RiverUnmarshaller riverUnmarshaller;
    private int remaining;

    BlockUnmarshaller(final RiverUnmarshaller riverUnmarshaller) {
        this.riverUnmarshaller = riverUnmarshaller;
    }

    boolean inBlock() {
        return remaining > 0;
    }

    int remaining() {
        return remaining;
    }

    void endOfStream() {
        if (remaining == 0) {
            remaining = -1;
        } else {
            throw new IllegalStateException("Not at end of block");
        }
    }

    void unblock() {
        if (remaining == -1) {
            remaining = 0;
        }
    }

    void readBlockHeader(int leadByte) throws IOException {
        switch (leadByte) {
            case Protocol.ID_START_BLOCK_SMALL:
                remaining = riverUnmarshaller.readUnsignedByte();
                return;
            case Protocol.ID_START_BLOCK_MEDIUM:
                remaining = riverUnmarshaller.readUnsignedShort();
                return;
            case Protocol.ID_START_BLOCK_LARGE:
                final int len = riverUnmarshaller.readInt();
                if (len < 0) {
                    throw new StreamCorruptedException("Invalid block length");
                }
                remaining = len;
                return;
            case Protocol.ID_END_BLOCK_DATA:
                remaining = -1;
                return;
            default:
                throw badLeadByte(leadByte);
        }
    }

    void readToEndBlockData() throws IOException, ClassNotFoundException {
        for (;;) {
            while (remaining > 0) {
                skipBytes(remaining);
            }
            final int b = riverUnmarshaller.read();
            switch (b) {
                case -1:
                    remaining = -1;
                    return;
                case Protocol.ID_END_BLOCK_DATA:
                    remaining = -1;
                    return;
                case Protocol.ID_START_BLOCK_SMALL:
                case Protocol.ID_START_BLOCK_MEDIUM:
                case Protocol.ID_START_BLOCK_LARGE:
                    readBlockHeader(b);
                    break;
                default:
                    // consume object... or whatever
                    riverUnmarshaller.doReadObject(b, false);
                    break;
            }
        }
    }

    private StreamCorruptedException badLeadByte(final int leadByte) {
        return new StreamCorruptedException("Unexpected lead byte " + leadByte);
    }

    public Object readObjectUnshared() throws ClassNotFoundException, IOException {
        return readObject(true);
    }

    public Object readObject() throws ClassNotFoundException, IOException {
        return readObject(false);
    }

    private Object readObject(boolean unshared) throws ClassNotFoundException, IOException {
        if (remaining > 0) {
            throw createOptionalDataException(remaining);
        } else if (remaining == -1) {
            throw createOptionalDataException(true);
        }
        final int leadByte = riverUnmarshaller.read();
        if (leadByte == -1 || leadByte == Protocol.ID_END_BLOCK_DATA) {
            remaining = -1;
            throw createOptionalDataException(true);
        }
        return riverUnmarshaller.doReadObject(leadByte, unshared);
    }

    public int read() throws IOException {
        while (remaining == 0) {
            final int v = riverUnmarshaller.read();
            if (v == -1) {
                return -1;
            }
            readBlockHeader(v);
        }
        if (remaining == -1) {
            return -1;
        }
        remaining--;
        return riverUnmarshaller.read();
    }

    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        while (remaining == 0) {
            final int v = riverUnmarshaller.read();
            if (v == -1) {
                return -1;
            }
            readBlockHeader(v);
        }
        final int remaining = this.remaining;
        if (remaining == -1) {
            return -1;
        }
        final int cnt = riverUnmarshaller.read(b, off, Math.min(remaining, len));
        this.remaining = remaining - cnt;
        return cnt;
    }

    public long skip(final long n) throws IOException {
        while (remaining == 0) {
            final int v = riverUnmarshaller.read();
            if (v == -1) {
                return -1;
            }
            readBlockHeader(v);
        }
        final int remaining = this.remaining;
        if (remaining == -1) {
            return -1;
        }
        final int cnt = riverUnmarshaller.skipBytes((int)Math.min((long)remaining, n));
        this.remaining = remaining - cnt;
        return cnt;
    }

    public int available() throws IOException {
        return Math.min(remaining, riverUnmarshaller.available());
    }

    public void readFully(final byte[] b) throws IOException {
        Marshalling.readFully(this, b);
    }

    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        Marshalling.readFully(this, b, off, len);
    }

    public int skipBytes(final int n) throws IOException {
        while (remaining == 0) {
            final int v = riverUnmarshaller.read();
            if (v == -1) {
                return -1;
            }
            readBlockHeader(v);
        }
        final int remaining = this.remaining;
        if (remaining == -1) {
            return -1;
        }
        final int cnt = riverUnmarshaller.skipBytes(Math.min(remaining, n));
        this.remaining = remaining - cnt;
        return cnt;
    }

    public boolean readBoolean() throws IOException {
        while (remaining == 0) {
            readBlockHeader(riverUnmarshaller.readUnsignedByte());
        }
        if (remaining == -1) {
            throw new EOFException();
        }
        remaining--;
        return riverUnmarshaller.readBoolean();
    }

    public byte readByte() throws IOException {
        while (remaining == 0) {
            readBlockHeader(riverUnmarshaller.readUnsignedByte());
        }
        if (remaining == -1) {
            throw new EOFException();
        }
        remaining--;
        return riverUnmarshaller.readByte();
    }

    public int readUnsignedByte() throws IOException {
        while (remaining == 0) {
            readBlockHeader(riverUnmarshaller.readUnsignedByte());
        }
        if (remaining == -1) {
            throw new EOFException();
        }
        remaining--;
        return riverUnmarshaller.readUnsignedByte();
    }

    public short readShort() throws IOException {
        if (remaining < 2) {
            return (short) (readUnsignedByte() << 8 | readUnsignedByte());
        } else {
            remaining -= 2;
            return riverUnmarshaller.readShort();
        }
    }

    public int readUnsignedShort() throws IOException {
        if (remaining < 2) {
            return readUnsignedByte() << 8 | readUnsignedByte();
        } else {
            remaining -= 2;
            return riverUnmarshaller.readUnsignedShort();
        }
    }

    public char readChar() throws IOException {
        if (remaining < 2) {
            return (char) (readUnsignedByte() << 8 | readUnsignedByte());
        } else {
            remaining -= 2;
            return riverUnmarshaller.readChar();
        }
    }

    public int readInt() throws IOException {
        if (remaining < 4) {
            return readUnsignedByte() << 24 | readUnsignedByte() << 16 | readUnsignedByte() << 8 | readUnsignedByte();
        } else {
            remaining -= 4;
            return riverUnmarshaller.readInt();
        }
    }

    public long readLong() throws IOException {
        if (remaining < 8) {
            return (long) readUnsignedByte() << 56L | (long) readUnsignedByte() << 48L | (long) readUnsignedByte() << 40L | (long) readUnsignedByte() << 32L |
                    (long) readUnsignedByte() << 24L | (long) readUnsignedByte() << 16L | (long) readUnsignedByte() << 8L | (long) readUnsignedByte();
        } else {
            remaining -= 8;
            return riverUnmarshaller.readLong();
        }
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public String readLine() throws IOException {
        throw new UnsupportedOperationException("readLine() is deprecated anyway!");
    }

    public String readUTF() throws IOException {
        final int len = readInt();
        if (len < 0) {
            throw new StreamCorruptedException("Invalid string length");
        }
        return UTFUtils.readUTFBytes(this, len);
    }

    public void clearInstanceCache() throws IOException {
        throw new IllegalStateException("clearInstanceCache() may not be called in this context");
    }

    public void clearClassCache() throws IOException {
        throw new IllegalStateException("clearClassCache() may not be called in this context");
    }

    public void start(final ByteInput newInput) throws IOException {
        throw new IllegalStateException("start() may not be called in this context");
    }

    public void finish() throws IOException {
        throw new IllegalStateException("finish() may not be called in this context");
    }

    public void close() throws IOException {
        throw new IllegalStateException("close() may not be called in this context");
    }
}
