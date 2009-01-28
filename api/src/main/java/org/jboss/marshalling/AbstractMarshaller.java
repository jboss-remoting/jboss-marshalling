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
import java.io.NotActiveException;

/**
 * An abstract implementation of the {@code Marshaller} interface.  Most of the
 * write methods delegate directly to the current data output.
 */
public abstract class AbstractMarshaller implements Marshaller {
    /** The configured externalizer factory. */
    protected final ExternalizerFactory externalizerFactory;
    /** The configured class externalizer factory. */
    protected final ClassExternalizerFactory classExternalizerFactory;
    /** The configured stream header. */
    protected final StreamHeader streamHeader;
    /** The configured class resolver. */
    protected final ClassResolver classResolver;
    /** The configured object resolver. */
    protected final ObjectResolver objectResolver;
    /** The configured object creator. */
    protected final Creator creator;
    /** The configured class table. */
    protected final ClassTable classTable;
    /** The configured object table. */
    protected final ObjectTable objectTable;
    /** The current byte output. */
    protected ByteOutput byteOutput;

    /**
     * Construct a new marshaller instance.
     *
     * @param marshallerFactory the marshaller factory
     * @param configuration
     */
    protected AbstractMarshaller(final AbstractMarshallerFactory marshallerFactory, final MarshallingConfiguration configuration) {
        final ExternalizerFactory externalizerFactory = configuration.getExternalizerFactory();
        this.externalizerFactory = externalizerFactory == null ? marshallerFactory.getDefaultExternalizerFactory() : externalizerFactory;
        final ClassExternalizerFactory classExternalizerFactory = configuration.getClassExternalizerFactory();
        this.classExternalizerFactory = classExternalizerFactory == null ? marshallerFactory.getDefaultClassExternalizerFactory() : classExternalizerFactory;
        final StreamHeader streamHeader = configuration.getStreamHeader();
        this.streamHeader = streamHeader == null ? marshallerFactory.getDefaultStreamHeader() : streamHeader;
        final ClassResolver classResolver = configuration.getClassResolver();
        this.classResolver = classResolver == null ? marshallerFactory.getDefaultClassResolver() : classResolver;
        final ObjectResolver objectResolver = configuration.getObjectResolver();
        this.objectResolver = objectResolver == null ? marshallerFactory.getDefaultObjectResolver() : objectResolver;
        final Creator creator = configuration.getCreator();
        this.creator = creator == null ? marshallerFactory.getDefaultCreator() : creator;
        final ClassTable classTable = configuration.getClassTable();
        this.classTable = classTable == null ? marshallerFactory.getDefaultClassTable() : classTable;
        final ObjectTable objectTable = configuration.getObjectTable();
        this.objectTable = objectTable == null ? marshallerFactory.getDefaultObjectTable() : objectTable;
        bufsize = configuration.getBufferSize();
    }

    private NotActiveException notActiveException() {
        return new NotActiveException("Output not started");
    }

    private byte[] buffer;
    private final int bufsize;
    private int position;

    /** {@inheritDoc} */
    public void write(final int v) throws IOException {
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining == 0) {
                flush();
                buffer[0] = (byte) v;
                position = 1;
            } else {
                buffer[position++] = (byte) v;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void write(final byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    /** {@inheritDoc} */
    public void write(final byte[] bytes, final int off, int len) throws IOException {
        final int bl = buffer.length;
        final int position = this.position;
        if (len > bl - position || len > bl >> 3) {
            flush();
            byteOutput.write(bytes, off, len);
        } else {
            System.arraycopy(bytes, off, buffer, position, len);
            this.position = position + len;
        }
    }

    /** {@inheritDoc} */
    public void writeBoolean(final boolean v) throws IOException {
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining == 0) {
                flush();
                buffer[0] = (byte) (v ? 1 : 0);
                position = 1;
            } else {
                buffer[position++] = (byte) (v ? 1 : 0); 
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeByte(final int v) throws IOException {
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining == 0) {
                flush();
                buffer[0] = (byte) v;
                position = 1;
            } else {
                buffer[position++] = (byte) v;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeShort(final int v) throws IOException {
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining < 2) {
                flush();
                buffer[0] = (byte) (v >> 8);
                buffer[1] = (byte) v;
                position = 2;
            } else {
                final int s = position;
                position = s + 2;
                buffer[s]   = (byte) (v >> 8);
                buffer[s+1] = (byte) v;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeChar(final int v) throws IOException {
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining < 2) {
                flush();
                buffer[0] = (byte) (v >> 8);
                buffer[1] = (byte) v;
                position = 2;
            } else {
                final int s = position;
                position = s + 2;
                buffer[s]   = (byte) (v >> 8);
                buffer[s+1] = (byte) v;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeInt(final int v) throws IOException {
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining < 4) {
                flush();
                buffer[0] = (byte) (v >> 24);
                buffer[1] = (byte) (v >> 16);
                buffer[2] = (byte) (v >> 8);
                buffer[3] = (byte) v;
                position = 4;
            } else {
                final int s = position;
                position = s + 4;
                buffer[s]   = (byte) (v >> 24);
                buffer[s+1] = (byte) (v >> 16);
                buffer[s+2] = (byte) (v >> 8);
                buffer[s+3] = (byte) v;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeLong(final long v) throws IOException {
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining < 8) {
                flush();
                buffer[0] = (byte) (v >> 56L);
                buffer[1] = (byte) (v >> 48L);
                buffer[2] = (byte) (v >> 40L);
                buffer[3] = (byte) (v >> 32L);
                buffer[4] = (byte) (v >> 24L);
                buffer[5] = (byte) (v >> 16L);
                buffer[6] = (byte) (v >> 8L);
                buffer[7] = (byte) v;
                position = 8;
            } else {
                final int s = position;
                position = s + 8;
                buffer[s]   = (byte) (v >> 56L);
                buffer[s+1] = (byte) (v >> 48L);
                buffer[s+2] = (byte) (v >> 40L);
                buffer[s+3] = (byte) (v >> 32L);
                buffer[s+4] = (byte) (v >> 24L);
                buffer[s+5] = (byte) (v >> 16L);
                buffer[s+6] = (byte) (v >> 8L);
                buffer[s+7] = (byte) v;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeFloat(final float v) throws IOException {
        final int bits = Float.floatToIntBits(v);
        try {
            final byte[] buffer = this.buffer;
            final int remaining = buffer.length - position;
            if (remaining < 4) {
                flush();
                buffer[0] = (byte) (bits >> 24);
                buffer[1] = (byte) (bits >> 16);
                buffer[2] = (byte) (bits >> 8);
                buffer[3] = (byte) bits;
                position = 4;
            } else {
                final int s = position;
                position = s + 4;
                buffer[s]   = (byte) (bits >> 24);
                buffer[s+1] = (byte) (bits >> 16);
                buffer[s+2] = (byte) (bits >> 8);
                buffer[s+3] = (byte) bits;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeDouble(final double v) throws IOException {
        final long bits = Double.doubleToLongBits(v);
        try {
            final int remaining = buffer.length - position;
            if (remaining < 8) {
                flush();
                buffer[0] = (byte) (bits >> 56L);
                buffer[1] = (byte) (bits >> 48L);
                buffer[2] = (byte) (bits >> 40L);
                buffer[3] = (byte) (bits >> 32L);
                buffer[4] = (byte) (bits >> 24L);
                buffer[5] = (byte) (bits >> 16L);
                buffer[6] = (byte) (bits >> 8L);
                buffer[7] = (byte) bits;
                position = 8;
            } else {
                final int s = position;
                position = s + 8;
                buffer[s]   = (byte) (bits >> 56L);
                buffer[s+1] = (byte) (bits >> 48L);
                buffer[s+2] = (byte) (bits >> 40L);
                buffer[s+3] = (byte) (bits >> 32L);
                buffer[s+4] = (byte) (bits >> 24L);
                buffer[s+5] = (byte) (bits >> 16L);
                buffer[s+6] = (byte) (bits >> 8L);
                buffer[s+7] = (byte) bits;
            }
        } catch (NullPointerException e) {
            throw notActiveException();
        }
    }

    /** {@inheritDoc} */
    public void writeBytes(final String s) throws IOException {
        final int len = s.length();
        for (int i = 0; i < len; i ++) {
            write(s.charAt(i));
        }
    }

    /** {@inheritDoc} */
    public void writeChars(final String s) throws IOException {
        final int len = s.length();
        for (int i = 0; i < len; i ++) {
            writeChar(s.charAt(i));
        }
    }

    /** {@inheritDoc} */
    public void writeUTF(final String s) throws IOException {
        flush();
        writeShort(UTFUtils.getShortUTFLength(s));
        UTFUtils.writeUTFBytes(byteOutput, s);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException {
        final int pos = position;
        final ByteOutput byteOutput = this.byteOutput;
        if (byteOutput != null) {
            if (pos > 0) {
                byteOutput.write(buffer, 0, pos);
            }
            position = 0;
            byteOutput.flush();
        }
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        finish();
    }

    /** {@inheritDoc} */
    public void start(final ByteOutput byteOutput) throws IOException {
        this.byteOutput = byteOutput;
        buffer = new byte[bufsize];
        doStart();
    }

    /** {@inheritDoc} */
    public void finish() throws IOException {
        try {
            flush();
        } finally {
            buffer = null;
            byteOutput = null;
            clearClassCache();
        }
    }

    /**
     * Implementation of the actual object-writing method.
     *
     * @param obj the object to write
     * @param unshared {@code true} if the instance is unshared, {@code false} if it is shared
     * @throws IOException if an I/O error occurs
     */
    protected abstract void doWriteObject(Object obj, boolean unshared) throws IOException;

    /** {@inheritDoc} */
    public final void writeObjectUnshared(final Object obj) throws IOException {
        doWriteObject(obj, true);
    }

    /** {@inheritDoc} */
    public final void writeObject(final Object obj) throws IOException {
        doWriteObject(obj, false);
    }

    /**
     * Perform any marshaller-specific start activity.  This implementation simply writes the stream header.
     *
     * @throws IOException if I/O exception occurs
     */
    protected void doStart() throws IOException {
        streamHeader.writeHeader(this);
    }
}
