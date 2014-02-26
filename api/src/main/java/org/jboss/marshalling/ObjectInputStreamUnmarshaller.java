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

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * An Unmarshaller which simply wraps an object stream.  Useful for retrofitting and testing applications.
 */
public class ObjectInputStreamUnmarshaller implements Unmarshaller {
    private final ObjectInputStream ois;

    /**
     * Construct a new instance which wraps the given stream.
     *
     * @param ois the object stream to wrap
     */
    public ObjectInputStreamUnmarshaller(final ObjectInputStream ois) {
        this.ois = ois;
    }

    /** {@inheritDoc} */
    public Object readObject() throws IOException, ClassNotFoundException {
        return ois.readObject();
    }

    /** {@inheritDoc} */
    public Object readObjectUnshared() throws IOException, ClassNotFoundException {
        return ois.readUnshared();
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        return ois.read();
    }

    /** {@inheritDoc} */
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        return ois.read(buf, off, len);
    }

    /** {@inheritDoc} */
    public int available() throws IOException {
        return ois.available();
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        ois.close();
    }

    /** {@inheritDoc} */
    public boolean readBoolean() throws IOException {
        return ois.readBoolean();
    }

    /** {@inheritDoc} */
    public byte readByte() throws IOException {
        return ois.readByte();
    }

    /** {@inheritDoc} */
    public int readUnsignedByte() throws IOException {
        return ois.readUnsignedByte();
    }

    /** {@inheritDoc} */
    public char readChar() throws IOException {
        return ois.readChar();
    }

    /** {@inheritDoc} */
    public short readShort() throws IOException {
        return ois.readShort();
    }

    /** {@inheritDoc} */
    public int readUnsignedShort() throws IOException {
        return ois.readUnsignedShort();
    }

    /** {@inheritDoc} */
    public int readInt() throws IOException {
        return ois.readInt();
    }

    /** {@inheritDoc} */
    public long readLong() throws IOException {
        return ois.readLong();
    }

    /** {@inheritDoc} */
    public float readFloat() throws IOException {
        return ois.readFloat();
    }

    /** {@inheritDoc} */
    public double readDouble() throws IOException {
        return ois.readDouble();
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] buf) throws IOException {
        ois.readFully(buf);
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] buf, final int off, final int len) throws IOException {
        ois.readFully(buf, off, len);
    }

    /** {@inheritDoc} */
    public int skipBytes(final int len) throws IOException {
        return ois.skipBytes(len);
    }

    /** {@inheritDoc} */
    @Deprecated
    public String readLine() throws IOException {
        return ois.readLine();
    }

    /** {@inheritDoc} */
    public String readUTF() throws IOException {
        return ois.readUTF();
    }

    /** {@inheritDoc} */
    public int read(final byte[] b) throws IOException {
        return ois.read(b);
    }

    /** {@inheritDoc} */
    public long skip(final long n) throws IOException {
        return ois.skip(n);
    }

    /** {@inheritDoc} */
    public void start(final ByteInput newInput) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void clearInstanceCache() throws IOException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void clearClassCache() throws IOException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void finish() throws IOException {
    }

    /** {@inheritDoc} */
    public <T> T readObject(final Class<T> type) throws ClassNotFoundException, IOException {
        final Object obj = readObject();
        try {
            return type.cast(obj);
        } catch (ClassCastException e) {
            throw wrongType(e, type, obj.getClass());
        }
    }

    /** {@inheritDoc} */
    public <T> T readObjectUnshared(final Class<T> type) throws ClassNotFoundException, IOException {
        final Object obj = readObjectUnshared();
        try {
            return type.cast(obj);
        } catch (ClassCastException e) {
            throw wrongType(e, type, obj.getClass());
        }
    }

    private static InvalidObjectException wrongType(final ClassCastException e, final Class<?> expected, final Class<?> actual) {
        final InvalidObjectException ioe = new InvalidObjectException("Object is of the wrong type (expected " + expected + ", got " + actual + ")");
        ioe.initCause(e);
        return ioe;
    }
}
