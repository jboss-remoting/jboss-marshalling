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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@code OutputStream} which implements {@code ByteInput} and reads data from another {@code OutputStream}.
 * Usually the {@link Marshalling#createByteOutput(java.nio.ByteBuffer)} method should be used to create instances because
 * it can detect when the target already extends {@code OutputStream}.
 */
public class OutputStreamByteOutput extends FilterOutputStream implements ByteOutput {

    /**
     * Construct a new instance.
     *
     * @param outputStream the output stream to write to
     */
    public OutputStreamByteOutput(final OutputStream outputStream) {
        super(outputStream);
    }

    /**
     * Writes {@code len} bytes from the specified {@code byte} array starting at offset {@code off} to this
     * output stream.
     *
     * @param b the data
     * @param off the start offset in the data
     * @param len the number of bytes to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
    }
}
