/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

import java.io.InputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.ArrayDeque;

/**
 * A {@code ByteInput} implementation which is populated asynchronously with {@link ByteBuffer} instances.
 */
public class NioByteInput extends InputStream implements ByteInput {
    private final Queue<ByteBuffer> queue;
    private final InputHandler inputHandler;

    // protected by "queue"
    private boolean eof;
    private IOException failure;

    /**
     * Construct a new instance.  The given {@code inputHandler} will
     * be invoked after each buffer is fully read and when the stream is closed.
     *
     * @param inputHandler the input events handler
     */
    public NioByteInput(final InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        queue = new ArrayDeque<ByteBuffer>();
    }

    /**
     * Push a buffer into the queue.  There is no mechanism to limit the number of pushed buffers; if such a mechanism
     * is desired, it must be implemented externally, for example maybe using a {@link java.util.concurrent.Semaphore Semaphore}.
     *
     * @param buffer the buffer from which more data should be read
     */
    public void push(final ByteBuffer buffer) {
        synchronized (this) {
            if (!eof && failure == null) {
                queue.add(buffer);
            }
        }
    }

    /**
     * Push the EOF condition into the queue.  After this method is called, no further buffers may be pushed into this
     * instance.
     */
    public void pushEof() {
        synchronized (this) {
            eof = true;
        }
    }

    /**
     * Push an exception condition into the queue.  After this method is called, no further buffers may be pushed into this
     * instance.
     *
     * @param e the exception to push
     */
    public void pushException(IOException e) {
        synchronized (this) {
            if (! eof) {
                failure = e;
            }
        }
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        final Queue<ByteBuffer> queue = this.queue;
        synchronized (this) {
            while (queue.isEmpty()) {
                if (eof) {
                    return -1;
                }
                checkFailure();
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Interrupted on read()");
                }
            }
            final ByteBuffer buf = queue.peek();
            final int v = buf.get() & 0xff;
            if (buf.remaining() == 0) {
                queue.poll();
                try {
                    inputHandler.acknowledge();
                } catch (IOException e) {
                    eof = true;
                    queue.clear();
                    throw e;
                }
            }
            return v;
        }
    }

    /** {@inheritDoc} */
    public int read(final byte[] b, final int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        final Queue<ByteBuffer> queue = this.queue;
        synchronized (this) {
            while (queue.isEmpty()) {
                if (eof) {
                    return -1;
                }
                checkFailure();
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Interrupted on read()");
                }
            }
            int total = 0;
            while (len > 0) {
                final ByteBuffer buffer = queue.peek();
                if (buffer == null) {
                    break;
                }
                final int bytecnt = Math.min(buffer.remaining(), len);
                buffer.get(b, off, bytecnt);
                total += bytecnt;
                len -= bytecnt;
                if (buffer.remaining() == 0) {
                    try {
                        inputHandler.acknowledge();
                    } catch (IOException e) {
                        eof = true;
                        queue.clear();
                        throw e;
                    }
                }
            }
            return total;
        }
    }

    /** {@inheritDoc} */
    public int available() throws IOException {
        synchronized (this) {
            int total = 0;
            for (ByteBuffer buffer : queue) {
                total += buffer.remaining();
                if (total < 0) {
                    return Integer.MAX_VALUE;
                }
            }
            return total;
        }
    }

    public long skip(long qty) throws IOException {
        final Queue<ByteBuffer> queue = this.queue;
        synchronized (this) {
            while (queue.isEmpty()) {
                if (eof) {
                    return 0L;
                }
                checkFailure();
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Interrupted on read()");
                }
            }
            long skipped = 0L;
            while (qty > 0L) {
                final ByteBuffer buffer = queue.peek();
                if (buffer == null) {
                    break;
                }
                final int bytecnt = Math.min(buffer.remaining(), (int) Math.max((long)Integer.MAX_VALUE, qty));
                buffer.position(buffer.position() + bytecnt);
                skipped += bytecnt;
                qty -= bytecnt;
                if (buffer.remaining() == 0) {
                    queue.poll();
                    try {
                        inputHandler.acknowledge();
                    } catch (IOException e) {
                        eof = true;
                        queue.clear();
                        throw e;
                    }
                }
            }
            return skipped;
        }
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        synchronized (this) {
            if (! eof) {
                queue.clear();
                eof = true;
                inputHandler.close();
            }
        }
    }

    private void checkFailure() throws IOException {
        final IOException failure = this.failure;
        if (failure != null) {
            failure.fillInStackTrace();
            try {
                throw failure;
            } finally {
                eof = true;
                this.failure = null;
            }
        }
    }

    /**
     * A handler for events relating to the consumption of data from a {@link NioByteInput} instance.
     */
    public interface InputHandler extends Closeable {

        /**
         * Acknowledges the successful processing of an input buffer.
         *
         * @throws IOException if an I/O error occurs sending the acknowledgement
         */
        void acknowledge() throws IOException;

        /**
         * Signifies that the user of the enclosing {@link NioByteInput} has called the {@code close()} method
         * explicitly.
         *
         * @throws IOException if an I/O error occurs
         */
        void close() throws IOException;
    }
}
