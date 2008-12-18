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
