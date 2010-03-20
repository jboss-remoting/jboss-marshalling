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
import java.io.InterruptedIOException;

/**
 * A limited byte output stream.  Throws an exception if too many bytes are written.
 */
public class LimitedByteOutput extends ByteOutputStream {

    private final long limit;
    private long count;

    /**
     * Construct a new instance.
     *
     * @param byteOutput the byte output to write to
     * @param limit the byte limit
     */
    public LimitedByteOutput(final ByteOutput byteOutput, final long limit) {
        super(byteOutput);
        this.limit = limit;
    }

    /** {@inheritDoc} */
    public void write(final int b) throws IOException {
        final long count = this.count;
        if (count < limit) {
            super.write(b);
            this.count = count + 1;
        } else {
            throw new IOException("Limit exceeded");
        }
    }

    /** {@inheritDoc} */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        final long count = this.count;
        if (count + len > limit) {
            throw new IOException("Limit exceeded");
        }
        try {
            super.write(b, off, len);
        } catch (InterruptedIOException e) {
            this.count = count + e.bytesTransferred;
            throw e;
        }
        this.count = count + len;
    }
}
