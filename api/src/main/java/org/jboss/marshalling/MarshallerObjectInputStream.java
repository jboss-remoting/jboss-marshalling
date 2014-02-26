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

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectInputValidation;
import java.io.NotActiveException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamClass;

/**
 * A marshaller's object input stream.  Used by marshallers for compatibility with Java serialization.  Instances of
 * this class may be passed in to the overridden serialization methods for a class implementing {@link java.io.Serializable}.
 * <p>
 * This class is not part of the marshalling API; rather it is intended for marshaller implementers to make it easier
 * to develop Java serialization-compatible marshallers.
 */
public abstract class MarshallerObjectInputStream extends ObjectInputStream implements ByteInput {
    private final Unmarshaller input;

    /**
     * Construct a new instance which delegates to the given unmarshaller.
     *
     * @param input the delegate unmarshaller
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if the caller does not have permission to construct an instance of this class
     */
    protected MarshallerObjectInputStream(final Unmarshaller input) throws IOException, SecurityException {
        this.input = input;
    }

    // Delegated methods

    /** {@inheritDoc} */
    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return input.readObject();
    }

    /** {@inheritDoc} */
    public Object readUnshared() throws IOException, ClassNotFoundException {
        return input.readObjectUnshared();
    }

    /** {@inheritDoc} */
    public int read() throws IOException {
        return input.read();
    }

    /** {@inheritDoc} */
    public int read(final byte[] buf) throws IOException {
        return input.read(buf, 0, buf.length);
    }

    /** {@inheritDoc} */
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        return input.read(buf, off, len);
    }

    /** {@inheritDoc} */
    public int available() throws IOException {
        return input.available();
    }

    /** {@inheritDoc} */
    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    /** {@inheritDoc} */
    public byte readByte() throws IOException {
        return input.readByte();
    }

    /** {@inheritDoc} */
    public int readUnsignedByte() throws IOException {
        return input.readUnsignedByte();
    }

    /** {@inheritDoc} */
    public char readChar() throws IOException {
        return input.readChar();
    }

    /** {@inheritDoc} */
    public short readShort() throws IOException {
        return input.readShort();
    }

    /** {@inheritDoc} */
    public int readUnsignedShort() throws IOException {
        return input.readUnsignedShort();
    }

    /** {@inheritDoc} */
    public int readInt() throws IOException {
        return input.readInt();
    }

    /** {@inheritDoc} */
    public long readLong() throws IOException {
        return input.readLong();
    }

    /** {@inheritDoc} */
    public float readFloat() throws IOException {
        return input.readFloat();
    }

    /** {@inheritDoc} */
    public double readDouble() throws IOException {
        return input.readDouble();
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] buf) throws IOException {
        input.readFully(buf);
    }

    /** {@inheritDoc} */
    public void readFully(final byte[] buf, final int off, final int len) throws IOException {
        input.readFully(buf, off, len);
    }

    /** {@inheritDoc} */
    public int skipBytes(final int len) throws IOException {
        return input.skipBytes(len);
    }

    /** {@inheritDoc} */
    @Deprecated
    public String readLine() throws IOException {
        return input.readLine();
    }

    /** {@inheritDoc} */
    public String readUTF() throws IOException {
        return input.readUTF();
    }

    /** {@inheritDoc} */
    public long skip(final long n) throws IOException {
        return input.skip(n);
    }

    // Unsupported methods

    /** {@inheritDoc} */
    public final void mark(final int readlimit) {
    }

    /** {@inheritDoc} */
    public final void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    /** {@inheritDoc} */
    public final boolean markSupported() {
        return false;
    }

    /** {@inheritDoc} */
    public final void close() throws IllegalStateException {
        throw new IllegalStateException("Stream may not be closed in this context");
    }

    /** {@inheritDoc} */
    protected final Class<?> resolveClass(final ObjectStreamClass desc) throws IllegalStateException {
        throw new IllegalStateException("Class may not be resolved in this context");
    }

    /** {@inheritDoc} */
    protected final Class<?> resolveProxyClass(final String[] interfaces) throws IllegalStateException {
        throw new IllegalStateException("Class may not be resolved in this context");
    }

    /** {@inheritDoc} */
    protected final Object resolveObject(final Object obj) throws IllegalStateException {
        throw new IllegalStateException("Object may not be resolved in this context");
    }

    /** {@inheritDoc} */
    protected final boolean enableResolveObject(final boolean enable) throws IllegalStateException {
        throw new IllegalStateException("Object resolution may not be enabled in this context");
    }

    /** {@inheritDoc} */
    protected final void readStreamHeader() throws IllegalStateException {
        throw new IllegalStateException("Stream header may not be read in this context");
    }

    /** {@inheritDoc} */
    protected final ObjectStreamClass readClassDescriptor() throws IllegalStateException {
        throw new IllegalStateException("Class descriptor may not be read in this context");
    }

    // User-implementation methods

    /** {@inheritDoc} */
    public abstract void defaultReadObject() throws IOException, ClassNotFoundException;

    /** {@inheritDoc} */
    public abstract GetField readFields() throws IOException, ClassNotFoundException;

    /** {@inheritDoc} */
    public abstract void registerValidation(final ObjectInputValidation obj, final int prio) throws NotActiveException, InvalidObjectException;
}
