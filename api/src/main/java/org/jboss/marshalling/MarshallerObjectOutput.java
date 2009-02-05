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
