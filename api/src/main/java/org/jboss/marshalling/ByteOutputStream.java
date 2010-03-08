/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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
