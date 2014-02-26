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

/**
 * An {@code InputStream} which implements {@code ByteInput} and reads bytes from another {@code ByteInput}.
 * Usually the {@link Marshalling#createByteInput(InputStream)} method should be used to create instances because
 * it can detect when the target already implements {@code ByteInput}.
 */
public class ByteInputStream extends SimpleByteInput {

    protected volatile ByteInput byteInput;

    /**
     * Create a new instance.
     *
     * @param byteInput the byte input to read from
     */
    public ByteInputStream(final ByteInput byteInput) {
        this.byteInput = byteInput;
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        return byteInput.read();
    }

    /** {@inheritDoc} */
    public int read(final byte[] b) throws IOException {
        return byteInput.read(b);
    }

    /** {@inheritDoc} */
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return byteInput.read(b, off, len);
    }

    /** {@inheritDoc} */
    public long skip(final long n) throws IOException {
        return byteInput.skip(n);
    }

    /** {@inheritDoc} */
    public int available() throws IOException {
        return byteInput.available();
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        byteInput.close();
    }
}
