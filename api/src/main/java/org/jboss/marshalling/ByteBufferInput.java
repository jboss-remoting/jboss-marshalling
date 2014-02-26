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

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * An {@code InputStream} which implements {@code ByteInput} and reads bytes from a {@code ByteBuffer}.
 */
public class ByteBufferInput extends InputStream implements ByteInput {

    private final ByteBuffer buffer;

    /**
     * Construct a new instance.
     *
     * @param buffer the buffer to read from
     */
    public ByteBufferInput(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        try {
            return buffer.get() & 0xff;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    /** {@inheritDoc} */
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /** {@inheritDoc} */
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        final int rem = buffer.remaining();
        if (rem == 0) {
            return -1;
        }
        final int c = Math.min(len, rem);
        buffer.get(b, 0, c);
        return c;
    }

    /** {@inheritDoc} */
    public int available() throws IOException {
        return buffer.remaining();
    }

    /** {@inheritDoc} */
    public long skip(final long n) throws IOException {
        if (n > 0L) {
            final long c = Math.min((long) buffer.remaining(), n);
            buffer.position(buffer.position() + (int) c);
            return c;
        } else {
            return 0L;
        }
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
    }
}
