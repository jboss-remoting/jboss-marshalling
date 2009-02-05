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

import java.io.ObjectInput;
import java.io.IOException;

/**
 * A marshaller's object input.  This implementation delegates to an {@code Unmarshaller} implementation while throwing
 * an exception if {@code close()} is called.
 * <p>
 * This class is not part of the marshalling API; rather it is intended for marshaller implementors to make it easier
 * to develop Java serialization-compatible marshallers.
 */
public class MarshallerObjectInput implements ObjectInput {

    private final Unmarshaller unmarshaller;

    /**
     * Construct a new instance.
     *
     * @param unmarshaller the unmarshaller to delegate to
     */
    public MarshallerObjectInput(final Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    /** {@inheritDoc} */
    public Object readObject() throws ClassNotFoundException, IOException {
        return unmarshaller.readObject();
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        return unmarshaller.read();
    }

    /** {@inheritDoc} */
    public int read(final byte[] b) throws IOException {
        return unmarshaller.read(b);
    }

    /** {@inheritDoc} */
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return unmarshaller.read(b, off, len);
    }

    /** {@inheritDoc} */
    public long skip(final long n) throws IOException {
        return unmarshaller.skip(n);
    }

    /** {@inheritDoc} */
    public int available() throws IOException {
        return unmarshaller.available();
    }

    /** {@inheritDoc}  This implementation always throws an {@link IllegalStateException}. */
    public void close() throws IOException {
        throw new IllegalStateException("close() may not be called in this context");
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] b) throws IOException {
        unmarshaller.readFully(b);
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        unmarshaller.readFully(b, off, len);
    }

    /** {@inheritDoc} */
    public int skipBytes(final int n) throws IOException {
        return unmarshaller.skipBytes(n);
    }

    /** {@inheritDoc} */
    public boolean readBoolean() throws IOException {
        return unmarshaller.readBoolean();
    }

    /** {@inheritDoc} */
    public byte readByte() throws IOException {
        return unmarshaller.readByte();
    }

    /** {@inheritDoc} */
    public int readUnsignedByte() throws IOException {
        return unmarshaller.readUnsignedByte();
    }

    /** {@inheritDoc} */
    public short readShort() throws IOException {
        return unmarshaller.readShort();
    }

    /** {@inheritDoc} */
    public int readUnsignedShort() throws IOException {
        return unmarshaller.readUnsignedShort();
    }

    /** {@inheritDoc} */
    public char readChar() throws IOException {
        return unmarshaller.readChar();
    }

    /** {@inheritDoc} */
    public int readInt() throws IOException {
        return unmarshaller.readInt();
    }

    /** {@inheritDoc} */
    public long readLong() throws IOException {
        return unmarshaller.readLong();
    }

    /** {@inheritDoc} */
    public float readFloat() throws IOException {
        return unmarshaller.readFloat();
    }

    /** {@inheritDoc} */
    public double readDouble() throws IOException {
        return unmarshaller.readDouble();
    }

    /** {@inheritDoc} */
    public String readLine() throws IOException {
        return unmarshaller.readLine();
    }

    /** {@inheritDoc} */
    public String readUTF() throws IOException {
        return unmarshaller.readUTF();
    }
}
