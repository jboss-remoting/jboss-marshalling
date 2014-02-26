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
