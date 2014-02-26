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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * An output stream of bytes.
 */
public interface ByteOutput extends Closeable, Flushable {
    /**
     * Writes to the output stream the eight low-order bits of the argument {@code b}. The 24 high-order bits of
     * {@code b} are ignored.
     *
     * @param b the byte to write
     * @throws IOException if an error occurs
     */
    void write(int b) throws IOException;

    /**
     * Write all the bytes from the given array to the stream.
     *
     * @param b the byte array
     * @throws IOException if an error occurs
     */
    void write(byte[] b) throws IOException;

    /**
     * Write some of the bytes from the given array to the stream.
     *
     * @param b the byte array
     * @param off the index to start writing from
     * @param len the number of bytes to write
     * @throws IOException if an error occurs
     */
    void write(byte[] b, int off, int len) throws IOException;
}
