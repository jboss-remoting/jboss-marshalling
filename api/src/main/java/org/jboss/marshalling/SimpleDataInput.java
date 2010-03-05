/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class SimpleDataInput extends InputStream implements DataInput, ByteInput {

    /** The current byte input. */
    protected ByteInput byteInput;
    protected final byte[] buffer;
    protected int position;
    protected int limit;

    public SimpleDataInput(int bufferSize) {
        buffer = new byte[bufferSize];
    }

    public SimpleDataInput(int bufferSize, ByteInput byteInput) {
        this(bufferSize);
        this.byteInput = byteInput;
    }

    public SimpleDataInput(final ByteInput byteInput) {
        this(8192, byteInput);
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        final int limit = this.limit;
        if (limit == -1) {
            return -1;
        }
        final int position = this.position;
        final byte[] buffer = this.buffer;
        if (position == limit) {
            if ((this.limit = byteInput.read(buffer)) == -1) {
                this.position = 0;
                return -1;
            } else {
                this.position = 1;
                return buffer[0] & 0xff;
            }
        } else {
            this.position = position + 1;
            return buffer[position] & 0xff;
        }
    }

    /** {@inheritDoc} */
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /** {@inheritDoc} */
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int limit = this.limit;
        if (limit == -1) {
            return -1;
        }
        final int position = this.position;
        final int remaining = limit - position;
        // pass through if the buffer is empty
        if (remaining == 0) {
            return byteInput.read(b, off, len);
        }
        final byte[] buffer = this.buffer;
        if (len > remaining) {
            System.arraycopy(buffer, position, b, off, remaining);
            this.limit = this.position = 0;
            final int res = byteInput.read(b, off + remaining, len - remaining);
            return res == -1 ? remaining : res + remaining;
        } else {
            System.arraycopy(buffer, position, b, off, len);
            this.position += len;
            return len;
        }
    }

    /** {@inheritDoc} */
    public long skip(final long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        }
        final int limit = this.limit;
        if (limit == -1) {
            return 0L;
        }
        final long remaining = limit - position;
        if (remaining > n) {
            position += (int) n;
            return n;
        } else {
            position = this.limit = 0;
            return byteInput.skip(n - remaining) + remaining;
        }
    }

    /** {@inheritDoc} */
    public int available() throws IOException {
        return limit - position + byteInput.available();
    }

    private static EOFException eofOnRead() {
        return new EOFException("Read past end of file");
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] b, int off, int len) throws IOException {
        if (limit == -1) {
            throw eofOnRead();
        }
        int remaining = limit - position;
        if (len > remaining) {
            if (remaining > 0) {
                System.arraycopy(buffer, position, b, off, remaining);
                limit = position = 0;
                off += remaining;
                len -= remaining;
            }
            do {
                remaining = byteInput.read(b, off, len);
                if (remaining == -1) {
                    throw eofOnRead();
                }
                off += remaining;
                len -= remaining;
            } while (len != 0);
        } else try {
            System.arraycopy(buffer, position, b, off, len);
            position += len;
        } catch (NullPointerException e) {
            throw eofOnRead();
        }
    }

    /** {@inheritDoc} */
    public int skipBytes(final int n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        }
        final int limit = this.limit;
        if (limit == -1) {
            return 0;
        }
        final int remaining = limit - position;
        if (remaining > n) {
            position += n;
            return n;
        } else {
            position = this.limit = 0;
            return (int) (byteInput.skip(n - remaining) + remaining);
        }
    }

    /** {@inheritDoc} */
    public boolean readBoolean() throws IOException {
        final int limit = this.limit;
        if (limit == -1) {
            throw eofOnRead();
        }
        int position;
        final byte[] buffer = this.buffer;
        if ((position = this.position++) == limit) {
            this.position = 1;
            if ((this.limit = byteInput.read(buffer)) == -1) {
                throw eofOnRead();
            }
            return buffer[0] != 0;
        }
        this.position = position + 1;
        return buffer[position] != 0;
    }

    /** {@inheritDoc} */
    public byte readByte() throws IOException {
        final int limit;
        if ((limit = this.limit) == -1) {
            throw eofOnRead();
        }
        int position;
        final byte[] buffer = this.buffer;
        if ((position = this.position++) == limit) {
            this.position = 1;
            if ((this.limit = byteInput.read(buffer)) == -1) {
                throw eofOnRead();
            }
            return buffer[0];
        }
        this.position = position + 1;
        return buffer[position];
    }

    /** {@inheritDoc} */
    public int readUnsignedByte() throws IOException {
        return readUnsignedByteDirect();
    }

    /** {@inheritDoc} */
    public short readShort() throws IOException {
        int position = this.position;
        int remaining = limit - position;
        if (remaining < 2) {
            return (short) (readUnsignedByteDirect() << 8 | readUnsignedByteDirect());
        } else {
            final byte[] buffer = this.buffer;
            this.position = position + 2;
            return (short) (buffer[position] << 8 | (buffer[position + 1] & 0xff));
        }
    }

    /** {@inheritDoc} */
    public int readUnsignedShort() throws IOException {
        int position = this.position;
        int remaining = limit - position;
        if (remaining < 2) {
            return readUnsignedByteDirect() << 8 | readUnsignedByteDirect();
        } else {
            final byte[] buffer = this.buffer;
            this.position = position + 2;
            return (buffer[position] & 0xff) << 8 | (buffer[position + 1] & 0xff);
        }
    }

    protected int readUnsignedByteDirect() throws IOException {
        final int limit;
        if ((limit = this.limit) == -1) {
            throw eofOnRead();
        }
        int position;
        final byte[] buffer = this.buffer;
        if ((position = this.position++) == limit) {
            this.position = 1;
            if ((this.limit = byteInput.read(buffer)) == -1) {
                throw eofOnRead();
            }
            return buffer[0] & 0xff;
        }
        return buffer[position] & 0xff;
    }

    /** {@inheritDoc} */
    public char readChar() throws IOException {
        int position = this.position;
        int remaining = limit - position;
        if (remaining < 2) {
            return (char) (readUnsignedByteDirect() << 8 | readUnsignedByteDirect());
        } else {
            final byte[] buffer = this.buffer;
            this.position = position + 2;
            return (char) (buffer[position] << 8 | (buffer[position + 1] & 0xff));
        }
    }

    /** {@inheritDoc} */
    public int readInt() throws IOException {
        return readIntDirect();
    }

    public long readLong() throws IOException {
        return readLongDirect();
    }

    /** {@inheritDoc} */
    protected long readLongDirect() throws IOException {
        return (long) readIntDirect() << 32L | (long) readIntDirect() & 0xffffffffL;
    }

    /** {@inheritDoc} */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readIntDirect());
    }

    protected int readIntDirect() throws IOException {
        int position = this.position;
        int remaining = limit - position;
        if (remaining < 4) {
            return readUnsignedByteDirect() << 24 | readUnsignedByteDirect() << 16 | readUnsignedByteDirect() << 8 | readUnsignedByteDirect();
        } else {
            final byte[] buffer = this.buffer;
            this.position = position + 4;
            return buffer[position] << 24 | (buffer[position + 1] & 0xff) << 16 | (buffer[position + 2] & 0xff) << 8 | (buffer[position + 3] & 0xff);
        }
    }

    /** {@inheritDoc} */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLongDirect());
    }

    /** {@inheritDoc} */
    public String readLine() throws IOException {
        throw new UnsupportedOperationException("readLine() not supported");
    }

    /** {@inheritDoc} */
    public String readUTF() throws IOException {
        return UTFUtils.readUTFBytesByByteCount(this, readUnsignedShort());
    }

    public void close() throws IOException {
        final ByteInput byteInput = this.byteInput;
        if (byteInput != null) byteInput.close();
    }

    protected void start(final ByteInput byteInput) throws IOException {
        this.byteInput = byteInput;
        position = limit = 0;
    }

    protected void finish() throws IOException {
        limit = -1;
        position = 0;
        byteInput = null;
    }
}
