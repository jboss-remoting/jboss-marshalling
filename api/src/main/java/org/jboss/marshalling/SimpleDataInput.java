/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.marshalling;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

/**
 * A simple base implementation of {@link DataInput} which wraps a {@link ByteInput}.  This implementation maintains
 * an internal buffer.
 */
public class SimpleDataInput extends ByteInputStream implements DataInput {

    /**
     * The internal buffer.
     */
    protected final byte[] buffer;
    /**
     * The buffer position.
     */
    protected int position;
    /**
     * The buffer limit.
     */
    protected int limit;

    /**
     * Construct a new instance which wraps nothing.
     *
     * @param bufferSize the internal buffer size to use
     */
    public SimpleDataInput(int bufferSize) {
        this(bufferSize, null);
    }

    /**
     * Construct a new instance.
     *
     * @param bufferSize the internal buffer size to use
     * @param byteInput the byte input to initially wrap
     */
    public SimpleDataInput(int bufferSize, ByteInput byteInput) {
        super(byteInput);
        buffer = new byte[bufferSize];
        this.byteInput = byteInput;
    }

    /**
     * Construct a new instance.  A default buffer size is used.
     *
     * @param byteInput the byte input to initially wrap
     */
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
        final int position = this.position;
        int remaining = limit - position;
        if (len > remaining) {
            if (remaining > 0) {
                System.arraycopy(buffer, position, b, off, remaining);
                limit = this.position = 0;
                off += remaining;
                len -= remaining;
            }
            final ByteInput byteInput = this.byteInput;
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
            this.position = position + len;
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

    /**
     * Read an unsigned byte directly.
     *
     * @return the unsigned byte
     * @throws IOException if an error occurs
     */
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

    /**
     * Read an int value.
     *
     * @return the value
     * @throws IOException if an error occurs
     */
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

    /** {@inheritDoc} */
    public void close() throws IOException {
        final ByteInput byteInput = this.byteInput;
        if (byteInput != null) byteInput.close();
    }

    /**
     * Start reading from the given input.  The internal buffer is discarded.
     *
     * @param byteInput the new input from which to read
     * @throws IOException not thrown by this implementation, but may be overridden to be thrown if a problem occurs
     */
    protected void start(final ByteInput byteInput) throws IOException {
        this.byteInput = byteInput;
        position = limit = 0;
    }

    /**
     * Finish reading from the current input.  The internal buffer is discarded, not flushed.
     *
     * @throws IOException not thrown by this implementation, but may be overridden to be thrown if a problem occurs
     */
    protected void finish() throws IOException {
        limit = -1;
        position = 0;
        byteInput = null;
    }
}
