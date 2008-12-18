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
     * Skips over and discards up to {@code n} bytes of data from this input stream.
     *
     * @param n the number of bytes to attempt to skip
     * @return the number of bytes skipped
     * @throws IOException if an error occurs
     */
    long skip(long n) throws IOException;
}
