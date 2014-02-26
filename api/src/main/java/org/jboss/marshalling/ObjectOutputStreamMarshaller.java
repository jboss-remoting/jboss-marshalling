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
