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

import java.io.ObjectOutput;
import java.io.IOException;

/**
 * A marshaller's object output.  This implementation delegates to a {@code Marshaller} implementation while throwing
 * an exception if {@code close()} is called.
 * <p>
 * This class is not part of the marshalling API; rather it is intended for marshaller implementors to make it easier
 * to develop Java serialization-compatible marshallers.
 */
public class MarshallerObjectOutput implements ObjectOutput {
    private final Marshaller marshaller;

    /**
     * Construct a new instance.
     *
     * @param marshaller the marshaller to delegate to
     */
    public MarshallerObjectOutput(final Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    /** {@inheritDoc} */
    public void writeObject(final Object obj) throws IOException {
        marshaller.writeObject(obj);
    }

    /** {@inheritDoc} */
    public void write(final int b) throws IOException {
        marshaller.write(b);
    }

    /** {@inheritDoc} */
    public void write(final byte[] b) throws IOException {
        marshaller.write(b);
    }

    /** {@inheritDoc} */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        marshaller.write(b, off, len);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException {
        marshaller.flush();
    }

    /** {@inheritDoc}  This implementation always throws an {@link IllegalStateException}. */
    public void close() throws IOException {
        throw new IllegalStateException("close() may not be called in this context");
    }

    /** {@inheritDoc} */
    public void writeBoolean(final boolean v) throws IOException {
        marshaller.writeBoolean(v);
    }

    /** {@inheritDoc} */
    public void writeByte(final int v) throws IOException {
        marshaller.writeByte(v);
    }

    /** {@inheritDoc} */
    public void writeShort(final int v) throws IOException {
        marshaller.writeShort(v);
    }

    /** {@inheritDoc} */
    public void writeChar(final int v) throws IOException {
        marshaller.writeChar(v);
    }

    /** {@inheritDoc} */
    public void writeInt(final int v) throws IOException {
        marshaller.writeInt(v);
    }

    /** {@inheritDoc} */
    public void writeLong(final long v) throws IOException {
        marshaller.writeLong(v);
    }

    /** {@inheritDoc} */
    public void writeFloat(final float v) throws IOException {
        marshaller.writeFloat(v);
    }

    /** {@inheritDoc} */
    public void writeDouble(final double v) throws IOException {
        marshaller.writeDouble(v);
    }

    /** {@inheritDoc} */
    public void writeBytes(final String s) throws IOException {
        marshaller.writeBytes(s);
    }

    /** {@inheritDoc} */
    public void writeChars(final String s) throws IOException {
        marshaller.writeChars(s);
    }

    /** {@inheritDoc} */
    public void writeUTF(final String s) throws IOException {
        marshaller.writeUTF(s);
    }
}
