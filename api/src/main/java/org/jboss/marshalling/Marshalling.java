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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;

/**
 * Static utility methods for simplfying use of marshallers.
 */
public final class Marshalling {
    private Marshalling() {
    }

    private static final StreamHeader DEFAULT_STREAM_HEADER = new StreamHeader() {
        public void readHeader(final Unmarshaller input) throws IOException {
        }

        public void writeHeader(final Marshaller output) throws IOException {
        }
    };

    /**
     * Get the default stream header producer, which reads and writes no header at all.
     *
     * @return the default stream header producer
     */
    public static StreamHeader nullStreamHeader() {
        return DEFAULT_STREAM_HEADER;
    }

    /**
     * Create a {@code ByteInput} wrapper for a {@code ByteBuffer}.
     *
     * @param buffer the byte buffer
     * @return the byte input wrapper
     */
    public static ByteInput createByteInput(final ByteBuffer buffer) {
        return new ByteInput() {
            public int read() throws IOException {
                try {
                    return buffer.get() & 0xff;
                } catch (BufferUnderflowException e) {
                    return -1;
                }
            }

            public int read(final byte[] b) throws IOException {
                return read(b, 0, b.length);
            }

            public int read(final byte[] b, final int off, final int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                final int rem = buffer.remaining();
                if (rem == 0) {
                    return -1;
                }
                final int c = Math.min(len, rem);
                buffer.get(b, 0, c);
                return c;
            }

            public int available() throws IOException {
                return buffer.remaining();
            }

            public long skip(final long n) throws IOException {
                if (n > 0L) {
                    final long c = Math.min((long) buffer.remaining(), n);
                    buffer.position(buffer.position() + (int) c);
                    return c;
                } else {
                    return 0L;
                }
            }

            public void close() throws IOException {
                buffer.clear();
            }
        };
    }

    /**
     * Create a {@code ByteInput} wrapper for an {@code InputStream}.
     *
     * @param inputStream the input stream
     * @return the byte input wrapper
     */
    public static ByteInput createByteInput(final InputStream inputStream) {
        return new ByteInput() {
            public int read() throws IOException {
                return inputStream.read();
            }

            public int read(final byte[] b) throws IOException {
                return inputStream.read(b);
            }

            public int read(final byte[] b, final int off, final int len) throws IOException {
                return inputStream.read(b, off, len);
            }

            public int available() throws IOException {
                return inputStream.available();
            }

            public long skip(final long n) throws IOException {
                return inputStream.skip(n);
            }

            public void close() throws IOException {
                inputStream.close();
            }
        };
    }

    private static EOFException writePastEnd() {
        return new EOFException("Write past end of buffer");
    }

    private static IOException readOnlyBuffer() {
        return new IOException("Read only buffer");
    }

    /**
     * Create a {@code ByteOutput} wrapper for a {@code ByteBuffer}.
     *
     * @param buffer the byte buffer
     * @return the byte output wrapper
     */
    public static ByteOutput createByteOutput(final ByteBuffer buffer) {
        return new ByteOutput() {
            public void write(final int b) throws IOException {
                try {
                    buffer.put((byte)b);
                } catch (BufferOverflowException e) {
                    throw writePastEnd();
                } catch (ReadOnlyBufferException e) {
                    throw readOnlyBuffer();
                }
            }

            public void write(final byte[] b) throws IOException {
                try {
                    buffer.put(b);
                } catch (BufferOverflowException e) {
                    throw writePastEnd();
                } catch (ReadOnlyBufferException e) {
                    throw readOnlyBuffer();
                }
            }

            public void write(final byte[] b, final int off, final int len) throws IOException {
                try {
                    buffer.put(b, off, len);
                } catch (BufferOverflowException e) {
                    throw writePastEnd();
                } catch (ReadOnlyBufferException e) {
                    throw readOnlyBuffer();
                }
            }

            public void close() throws IOException {
                buffer.clear();
            }

            public void flush() throws IOException {
            }
        };
    }

    /**
     * Create a {@code ByteOutput} wrapper for an {@code OutputStream}.
     *
     * @param outputStream the output stream
     * @return the byte output wrapper
     */
    public static ByteOutput createByteOutput(final OutputStream outputStream) {
        return new ByteOutput() {
            public void write(final int b) throws IOException {
                outputStream.write(b);
            }

            public void write(final byte[] b) throws IOException {
                outputStream.write(b);
            }

            public void write(final byte[] b, final int off, final int len) throws IOException {
                outputStream.write(b, off, len);
            }

            public void close() throws IOException {
                outputStream.close();
            }

            public void flush() throws IOException {
                outputStream.flush();
            }
        };
    }

    private static final ExternalizerFactory NULL_EXTERNALIZER_FACTORY = new ExternalizerFactory() {
        public Externalizer getExternalizer(final Object instance) {
            return null;
        }
    };

    /**
     * Return the null externalizer factory.  This instance does not externalize any classes.
     *
     * @return the null externalizer
     */
    public static ExternalizerFactory nullExternalizerFactory() {
        return NULL_EXTERNALIZER_FACTORY;
    }

    private static final ObjectResolver NULL_OBJECT_RESOLVER = new ObjectResolver() {
        public Object readResolve(final Object replacement) {
            return replacement;
        }

        public Object writeReplace(final Object original) {
            return original;
        }
    };

    /**
     * Return the null object resolver.  This instance does not translate objects in any way.
     *
     * @return the null object resolver
     */
    public static ObjectResolver nullObjectResolver() {
        return NULL_OBJECT_RESOLVER;
    }

    private static final ObjectTable NULL_OBJECT_TABLE = new ObjectTable() {
        public Writer getObjectWriter(final Object object) {
            return null;
        }

        public Object readObject(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
            return null;
        }
    };

    /**
     * Return the null object instance table.  This instance contains no predefined instances.
     *
     * @return the null instance table
     */
    public static ObjectTable nullObjectTable() {
        return NULL_OBJECT_TABLE;
    }

    private static final ClassTable NULL_CLASS_TABLE = new ClassTable() {
        public Writer getClassWriter(final Class<?> clazz) {
            return null;
        }

        public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
            return null;
        }
    };

    /**
     * Return the null class table instance.  This instance contains no predefined classes.
     *
     * @return the null class table
     */
    public static ClassTable nullClassTable() {
        return NULL_CLASS_TABLE;
    }
}
