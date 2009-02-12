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

import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * A Marshaller which simply wraps an object stream.  Useful for retrofitting and testing applications.
 */
public class ObjectOutputStreamMarshaller implements Marshaller {
    private final ObjectOutputStream oos;

    /**
     * Construct a new instance which wraps the given stream.
     *
     * @param oos the object stream to wrap
     */
    public ObjectOutputStreamMarshaller(final ObjectOutputStream oos) {
        this.oos = oos;
    }

    /** {@inheritDoc} */
    public void writeObject(final Object obj) throws IOException {
        oos.writeObject(obj);
    }

    /** {@inheritDoc} */
    public void write(final int val) throws IOException {
        oos.write(val);
    }

    /** {@inheritDoc} */
    public void write(final byte[] buf) throws IOException {
        oos.write(buf);
    }

    /** {@inheritDoc} */
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        oos.write(buf, off, len);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException {
        oos.flush();
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        oos.close();
    }

    /** {@inheritDoc} */
    public void writeBoolean(final boolean val) throws IOException {
        oos.writeBoolean(val);
    }

    /** {@inheritDoc} */
    public void writeByte(final int val) throws IOException {
        oos.writeByte(val);
    }

    /** {@inheritDoc} */
    public void writeShort(final int val) throws IOException {
        oos.writeShort(val);
    }

    /** {@inheritDoc} */
    public void writeChar(final int val) throws IOException {
        oos.writeChar(val);
    }

    /** {@inheritDoc} */
    public void writeInt(final int val) throws IOException {
        oos.writeInt(val);
    }

    /** {@inheritDoc} */
    public void writeLong(final long val) throws IOException {
        oos.writeLong(val);
    }

    /** {@inheritDoc} */
    public void writeFloat(final float val) throws IOException {
        oos.writeFloat(val);
    }

    /** {@inheritDoc} */
    public void writeDouble(final double val) throws IOException {
        oos.writeDouble(val);
    }

    /** {@inheritDoc} */
    public void writeBytes(final String str) throws IOException {
        oos.writeBytes(str);
    }

    /** {@inheritDoc} */
    public void writeChars(final String str) throws IOException {
        oos.writeChars(str);
    }

    /** {@inheritDoc} */
    public void writeUTF(final String str) throws IOException {
        oos.writeUTF(str);
    }

    /** {@inheritDoc} */
    public void writeObjectUnshared(final Object obj) throws IOException {
        oos.writeUnshared(obj);
    }

    /** {@inheritDoc} */
    public void start(final ByteOutput newOutput) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void clearInstanceCache() throws IOException {
        oos.reset();
    }

    /** {@inheritDoc} */
    public void clearClassCache() throws IOException {
        oos.reset();
    }

    /** {@inheritDoc} */
    public void finish() throws IOException {
    }
}
