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

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;

/**
 * A marshaller's object output stream.  Used by marshallers for compatibility with Java serialization.  Instances of
 * this class may be passed in to the overridden serialization methods for a class implementing {@link java.io.Serializable}.
 * <p>
 * This class is not part of the marshalling API; rather it is intended for marshaller implementers to make it easier
 * to develop Java serialization-compatible marshallers.
 */
public abstract class MarshallerObjectOutputStream extends ObjectOutputStream implements ByteOutput {
    private final Marshaller output;

    /**
     * Construct a new instance that delegates to the given marshaller.
     *
     * @param output the delegate marshaller
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if the caller does not have permission to construct an instance of this class
     */
    protected MarshallerObjectOutputStream(final Marshaller output) throws IOException, SecurityException {
        this.output = output;
    }

    // Delegated methods

    /** {@inheritDoc} */
    protected void writeObjectOverride(final Object obj) throws IOException {
        output.writeObject(obj);
    }

    /** {@inheritDoc} */
    public void writeUnshared(final Object obj) throws IOException {
        output.writeObjectUnshared(obj);
    }

    /** {@inheritDoc} */
    public void write(final int val) throws IOException {
        output.write(val);
    }

    /** {@inheritDoc} */
    public void write(final byte[] buf) throws IOException {
        output.write(buf, 0, buf.length);
    }

    /** {@inheritDoc} */
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        output.write(buf, off, len);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException {
        output.flush();
    }

    /** {@inheritDoc} */
    public void writeBoolean(final boolean val) throws IOException {
        output.writeBoolean(val);
    }

    /** {@inheritDoc} */
    public void writeByte(final int val) throws IOException {
        output.writeByte(val);
    }

    /** {@inheritDoc} */
    public void writeShort(final int val) throws IOException {
        output.writeShort(val);
    }

    /** {@inheritDoc} */
    public void writeChar(final int val) throws IOException {
        output.writeChar(val);
    }

    /** {@inheritDoc} */
    public void writeInt(final int val) throws IOException {
        output.writeInt(val);
    }

    /** {@inheritDoc} */
    public void writeLong(final long val) throws IOException {
        output.writeLong(val);
    }

    /** {@inheritDoc} */
    public void writeFloat(final float val) throws IOException {
        output.writeFloat(val);
    }

    /** {@inheritDoc} */
    public void writeDouble(final double val) throws IOException {
        output.writeDouble(val);
    }

    /** {@inheritDoc} */
    public void writeBytes(final String str) throws IOException {
        output.writeBytes(str);
    }

    /** {@inheritDoc} */
    public void writeChars(final String str) throws IOException {
        output.writeChars(str);
    }

    /** {@inheritDoc} */
    public void writeUTF(final String str) throws IOException {
        output.writeUTF(str);
    }

    // Unsupported methods

    /** {@inheritDoc} */
    public final void reset() throws IOException {
        throw new IOException("reset() may not be invoked on this stream");
    }

    /** {@inheritDoc} */
    public final void close() throws IOException {
        throw new IllegalStateException("Stream may not be closed in this context");
    }

    /** {@inheritDoc} */
    public final void useProtocolVersion(final int version) throws IOException {
        throw new IllegalStateException("Protocol version may not be changed");
    }

    /** {@inheritDoc} */
    protected final void annotateClass(final Class<?> cl) throws IOException {
        throw new IllegalStateException("Class may not be annotated in this context");
    }

    /** {@inheritDoc} */
    protected final void annotateProxyClass(final Class<?> cl) throws IOException {
        throw new IllegalStateException("Class may not be annotated in this context");
    }

    /** {@inheritDoc} */
    protected final Object replaceObject(final Object obj) throws IOException {
        throw new IllegalStateException("Object may not be replaced in this context");
    }

    /** {@inheritDoc} */
    protected final boolean enableReplaceObject(final boolean enable) throws SecurityException {
        throw new SecurityException("Object replacement may not be controlled in this context");
    }

    /** {@inheritDoc} */
    protected final void writeStreamHeader() throws IOException {
        throw new IllegalStateException("Stream header may not be written in this context");
    }

    /** {@inheritDoc} */
    protected final void writeClassDescriptor(final ObjectStreamClass desc) throws IOException {
        throw new IllegalStateException("Class descriptor may not be written in this context");
    }

    /** {@inheritDoc} */
    protected final void drain() throws IOException {
        throw new IllegalStateException("Output may not be drained in this context");
    }

    // User-implementation methods

    /** {@inheritDoc} */
    public abstract void writeFields() throws IOException;

    /** {@inheritDoc} */
    public abstract PutField putFields() throws IOException;

    /** {@inheritDoc} */
    public abstract void defaultWriteObject() throws IOException;
}
