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
import java.io.IOException;

/**
 * An input stream of bytes.
 */
public interface ByteInput extends Closeable {

    /**
     * Reads the next byte of data from the input stream.  If no byte is available because the end of the stream has
     * been reached, the value -1 is returned. This method blocks until input data is available, the end of the stream
     * is detected, or an exception is thrown.
     *
     * @return the next byte, or -1 if the end of stream has been reached
     * @throws IOException if an error occurs
     */
    int read() throws IOException;

    /**
     * Read some bytes from the input stream into the given array.  Returns the number of bytes actually read (possibly
     * zero), or -1 if the end of stream has been reached.
     *
     * @param b the destination array
     * @return the number of bytes read (possibly zero), or -1 if the end of stream has been reached
     * @throws IOException if an error occurs
     */
    int read(byte[] b) throws IOException;

    /**
     * Read some bytes from the input stream into the given array.  Returns the number of bytes actually read (possibly
     * zero), or -1 if the end of stream has been reached.
     *
     * @param b the destination array
     * @param off the offset into the array into which data should be read
     * @param len the number of bytes to attempt to fill in the destination array
     * @return the number of bytes read (possibly zero), or -1 if the end of stream has been reached
     * @throws IOException if an error occurs
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Returns an estimate of the number of bytes that can be read (or skipped over) from this input stream without
     * blocking by the next invocation of a method for this input stream.
     *
     * @return the number of bytes
     * @throws IOException if an error occurs
     */
    int available() throws IOException;

    /**
     * Skips over and discards up to {@code n} bytes of data from this input stream.  If the end of stream is reached,
     * this method returns {@code 0} in order to be consistent with {@link java.io.InputStream#skip(long)}.
     *
     * @param n the number of bytes to attempt to skip
     * @return the number of bytes skipped
     * @throws IOException if an error occurs
     */
    long skip(long n) throws IOException;
}
