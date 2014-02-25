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
import java.io.OutputStream;

/**
 * An {@code OutputStream} which implements {@code ByteOutput} and writes bytes to another {@code ByteOutput}.
 * Usually the {@link Marshalling#createByteOutput(OutputStream)} method should be used to create instances because
 * it can detect when the target already implements {@code ByteOutput}.
 */
public class ByteOutputStream extends SimpleByteOutput {

    protected volatile ByteOutput byteOutput;

    /**
     * Construct a new instance.
     *
     * @param byteOutput the byte output to write to
     */
    public ByteOutputStream(final ByteOutput byteOutput) {
        this.byteOutput = byteOutput;
    }

    /** {@inheritDoc} */
    public void write(final int b) throws IOException {
        byteOutput.write(b);
    }

    /** {@inheritDoc} */
    public void write(final byte[] b) throws IOException {
        byteOutput.write(b);
    }

    /** {@inheritDoc} */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        byteOutput.write(b, off, len);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException {
        byteOutput.flush();
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        byteOutput.close();
    }
}
