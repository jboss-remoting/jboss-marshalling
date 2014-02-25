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
