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

/**
 * A limited byte input stream.  Throws an exception if too many bytes are read.
 */
public class LimitedByteInput extends ByteInputStream {
    private final long limit;
    private long count;

    /**
     * Create a new instance.
     *
     * @param byteInput the byte input to read from
     * @param limit the maximum number of bytes to read
     */
    public LimitedByteInput(final ByteInput byteInput, final long limit) {
        super(byteInput);
        this.limit = limit;
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        final long count = this.count;
        if (count < limit) try {
            return super.read();
        } finally {
            this.count = count + 1;
        }
        throw new IOException("Limit exceeded");
    }

    /** {@inheritDoc} */
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final long count = this.count;
        final long limit = this.limit;
        final int ret;
        if (count >= limit) {
            throw new IOException("Limit exceeded");
        } else if (count + len >= limit) {
            int rem = (int) (limit - count);
            ret = super.read(b, off, rem);
        } else {
            ret = super.read(b, off, len);
        }
        this.count = count + (long) ret;
        return ret;
    }

    /** {@inheritDoc} */
    public long skip(final long n) throws IOException {
        final long count = this.count;
        final long limit = this.limit;
        final long ret;
        if (count >= limit) {
            throw new IOException("Limit exceeded");
        } else if (count + n >= limit) {
            int rem = (int) (limit - count);
            ret = super.skip(rem);
        } else {
            ret = super.skip(n);
        }
        this.count = count + ret;
        return ret;
    }
}
