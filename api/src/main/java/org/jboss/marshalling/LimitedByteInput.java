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
