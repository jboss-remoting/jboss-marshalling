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

import java.io.IOException;
import java.io.ObjectInputValidation;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * An object input stream which wraps an {@code Unmarshaller}, which may be used by legacy {@link java.io.ObjectInputStream ObjectInputStream}-based
 * applications that wish to use the marshalling framework.
 */
public final class MarshallingObjectInputStream extends ObjectInputStream {

    private Unmarshaller unmarshaller;

    /**
     * Construct a new instance which delegates to the given unmarshaller, reading from the given input.  The unmarshaller
     * will read from the input stream until it is closed.
     *
     * @param unmarshaller the delegate unmarshaller
     * @param stream the input stream to read from
     *
     * @throws java.io.IOException if an I/O error occurs
     * @throws SecurityException if the caller does not have permission to construct an instance of this class
     */
    public MarshallingObjectInputStream(final Unmarshaller unmarshaller, final InputStream stream) throws IOException, SecurityException {
        this(unmarshaller, Marshalling.createByteInput(stream));
    }

    /**
     * Construct a new instance which delegates to the given unmarshaller, reading from the given input.  The unmarshaller
     * will read from the input stream until it is closed.
     *
     * @param unmarshaller the delegate unmarshaller
     * @param byteInput the input stream to read from
     *
     * @throws java.io.IOException if an I/O error occurs
     * @throws SecurityException if the caller does not have permission to construct an instance of this class
     */
    public MarshallingObjectInputStream(final Unmarshaller unmarshaller, final ByteInput byteInput) throws IOException, SecurityException {
        unmarshaller.start(byteInput);
        this.unmarshaller = unmarshaller;
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        return unmarshaller.readObjectUnshared();
    }

    protected Object readObjectOverride() throws ClassNotFoundException, IOException {
        return unmarshaller.readObject();
    }

    public int read() throws IOException {
        return unmarshaller.read();
    }

    public int read(final byte[] b) throws IOException {
        return unmarshaller.read(b);
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        return unmarshaller.read(b, off, len);
    }

    public long skip(final long n) throws IOException {
        return unmarshaller.skip(n);
    }

    public int available() throws IOException {
        return unmarshaller.available();
    }

    public void close() throws IOException, IllegalStateException {
        unmarshaller.finish();
        unmarshaller = null;
    }

    public void readFully(final byte[] b) throws IOException {
        unmarshaller.readFully(b);
    }

    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        unmarshaller.readFully(b, off, len);
    }

    public int skipBytes(final int n) throws IOException {
        return unmarshaller.skipBytes(n);
    }

    public boolean readBoolean() throws IOException {
        return unmarshaller.readBoolean();
    }

    public byte readByte() throws IOException {
        return unmarshaller.readByte();
    }

    public int readUnsignedByte() throws IOException {
        return unmarshaller.readUnsignedByte();
    }

    public short readShort() throws IOException {
        return unmarshaller.readShort();
    }

    public int readUnsignedShort() throws IOException {
        return unmarshaller.readUnsignedShort();
    }

    public char readChar() throws IOException {
        return unmarshaller.readChar();
    }

    public int readInt() throws IOException {
        return unmarshaller.readInt();
    }

    public long readLong() throws IOException {
        return unmarshaller.readLong();
    }

    public float readFloat() throws IOException {
        return unmarshaller.readFloat();
    }

    public double readDouble() throws IOException {
        return unmarshaller.readDouble();
    }

    public String readLine() throws IOException {
        return unmarshaller.readLine();
    }

    public String readUTF() throws IOException {
        return unmarshaller.readUTF();
    }

    public Object readObjectUnshared() throws ClassNotFoundException, IOException {
        return unmarshaller.readObjectUnshared();
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

    /**
     * May not be invoked in this context.
     *
     * @throws IllegalStateException always
     */
    public void defaultReadObject() throws IllegalStateException {
        throw new IllegalStateException("This method may not be invoked in this context");
    }

    /**
     * May not be invoked in this context.
     *
     * @throws IllegalStateException always
     */
    public GetField readFields() throws IllegalStateException {
        throw new IllegalStateException("Fields may not be read in this context");
    }

    /**
     * May not be invoked in this context.
     *
     * @param obj ignored
     * @param prio ignored
     * @throws IllegalStateException always
     */
    public void registerValidation(final ObjectInputValidation obj, final int prio) throws IllegalStateException {
        throw new IllegalStateException("Validation objects may not be registered in this context");
    }

}
