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
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.io.Serializable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.security.PrivilegedAction;
import java.security.AccessController;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.ServiceLoader;

/**
 * Static utility methods for simplfying use of marshallers.
 */
public final class Marshalling {

    private Marshalling() {
    }

    /**
     * Get a marshaller factory, by name.  Uses the thread's current context classloader, if available, to locate
     * the factory.
     *
     * @param name the name of the protocol to acquire
     * @return the marshaller factory, or {@code null} if no matching factory was found
     *
     * @see ServiceLoader
     */
    public static MarshallerFactory getMarshallerFactory(String name) {
        return loadMarshallerFactory(ServiceLoader.load(ProviderDescriptor.class), name);
    }

    /**
     * Get a marshaller factory, by name.  Uses the given classloader to locate
     * the factory.
     *
     * @param name the name of the protocol to acquire
     * @param classLoader the class loader to use
     * @return the marshaller factory, or {@code null} if no matching factory was found
     *
     * @see ServiceLoader
     */
    public static MarshallerFactory getMarshallerFactory(String name, ClassLoader classLoader) {
        return loadMarshallerFactory(ServiceLoader.load(ProviderDescriptor.class, classLoader), name);
    }

    private static MarshallerFactory loadMarshallerFactory(ServiceLoader<ProviderDescriptor> loader, String name) {
        for (ProviderDescriptor descriptor : loader) {
            if (name.equals(descriptor.getName())) {
                return descriptor.getMarshallerFactory();
            }
        }
        return null;
    }

    private static final StreamHeader NULL_STREAM_HEADER = new StreamHeader() {
        public void readHeader(final ByteInput input) throws IOException {
        }

        public void writeHeader(final ByteOutput output) throws IOException {
        }

        public String toString() {
            return "null StreamHeader";
        }
    };

    /**
     * Get the default stream header producer, which reads and writes no header at all.
     *
     * @return the default stream header producer
     */
    public static StreamHeader nullStreamHeader() {
        return NULL_STREAM_HEADER;
    }

    /**
     * Create a stream header that uses the given bytes.
     *
     * @param headerBytes the header bytes
     * @return the stream header object
     */
    public static StreamHeader streamHeader(final byte[] headerBytes) {
        return new StaticStreamHeader(headerBytes);
    }

    private static final class StaticStreamHeader implements StreamHeader, Serializable {
        private final byte[] headerBytes;

        private static final long serialVersionUID = 8465784729867667872L;

        public StaticStreamHeader(final byte[] bytes) {
            headerBytes = bytes;
        }

        public void readHeader(final ByteInput input) throws IOException {
            final byte[] buf = new byte[headerBytes.length];
            readFully(input, buf);
            if (! Arrays.equals(buf, headerBytes)) {
                throw new StreamCorruptedException("Header is incorrect (expected " + Arrays.toString(headerBytes) + ", got " + Arrays.toString(buf) + ")");
            }
        }

        public void writeHeader(final ByteOutput output) throws IOException {
            output.write(headerBytes);
        }

        public String toString() {
            return "static StreamHeader@" + Integer.toHexString(hashCode()) + " (" + headerBytes.length + " bytes)";
        }
    }

    /**
     * Read bytes from a {@code ByteInput}.  Fully fills in the array.
     *
     * @param input the input
     * @param dest the destination
     * @throws EOFException if the end of file is reached before the array is filled
     * @throws IOException if an I/O error occurs
     */
    public static void readFully(ByteInput input, byte[] dest) throws IOException {
        readFully(input, dest, 0, dest.length);
    }

    /**
     * Read bytes from a {@code ByteInput}.  Fully fills in {@code len} bytes in the array.
     *
     * @param input the input
     * @param dest the destination
     * @param offs the offset into the array
     * @param len the number of bytes
     * @throws EOFException if the end of file is reached before the array is filled
     * @throws IOException if an I/O error occurs
     */
    public static void readFully(ByteInput input, byte[] dest, int offs, int len) throws IOException {
        while (len > 0) {
            final int r = input.read(dest, offs, len);
            if (r == -1) {
                throw new EOFException();
            }
            len -= r;
            offs += r;
        }
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

    /**
     * Create an {@code InputStream} wrapper for a {@code ByteInput}.
     *
     * @param byteInput the byte input
     * @return the input stream wrapper
     */
    public static InputStream createInputStream(final ByteInput byteInput) {
        return new InputStream() {
            public int read() throws IOException {
                return byteInput.read();
            }

            public int read(final byte[] b) throws IOException {
                return byteInput.read(b);
            }

            public int read(final byte[] b, final int off, final int len) throws IOException {
                return byteInput.read(b, off, len);
            }

            public long skip(final long n) throws IOException {
                return byteInput.skip(n);
            }

            public int available() throws IOException {
                return byteInput.available();
            }

            public void close() throws IOException {
                byteInput.close();
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

    /**
     * Create a {@code OutputStream} wrapper for a {@code ByteOutput}.
     *
     * @param byteOutput the byte output
     * @return the output stream wrapper
     */
    public static OutputStream createOutputStream(final ByteOutput byteOutput) {
        return new OutputStream() {
            public void write(final int b) throws IOException {
                byteOutput.write(b);
            }

            public void write(final byte[] b) throws IOException {
                byteOutput.write(b);
            }

            public void write(final byte[] b, final int off, final int len) throws IOException {
                byteOutput.write(b, off, len);
            }

            public void flush() throws IOException {
                byteOutput.flush();
            }

            public void close() throws IOException {
                byteOutput.close();
            }
        };
    }

    private static final ClassExternalizerFactory NULL_CLASS_EXTERNALIZER_FACTORY = new ClassExternalizerFactory() {
        public Externalizer getExternalizer(final Class<?> type) {
            return null;
        }
    };

    /**
     * Return the null class externalizer factory.  This instance does not externalize any classes.
     *
     * @return the null class externalizer factory
     */
    public static ClassExternalizerFactory nullClassExternalizerFactory() {
        return NULL_CLASS_EXTERNALIZER_FACTORY;
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

    /**
     * Construct a new {@link java.io.OptionalDataException}.  This method is necssary because there are no
     * public constructors in the API.
     *
     * @param eof {@code true} if there is no more data in the buffered part of the stream
     * @return a new OptionalDataException
     */
    public static OptionalDataException createOptionalDataException(boolean eof) {
        final OptionalDataException optionalDataException = createOptionalDataException();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final StackTraceElement[] copyStackTrace = new StackTraceElement[stackTrace.length - 1];
        System.arraycopy(stackTrace, 1, copyStackTrace, 0, copyStackTrace.length);
        optionalDataException.setStackTrace(copyStackTrace);
        optionalDataException.eof = eof;
        return optionalDataException;
    }

    /**
     * Construct a new {@link java.io.OptionalDataException}.  This method is necssary because there are no
     * public constructors in the API.
     *
     * @param length the number of bytes of primitive data available to be read in the current buffer
     * @return a new OptionalDataException
     */
    public static OptionalDataException createOptionalDataException(int length) {
        final OptionalDataException optionalDataException = createOptionalDataException();
        optionalDataException.length = length;
        return optionalDataException;
    }

    private static OptionalDataException createOptionalDataException() {
        return AccessController.doPrivileged(OPTIONAL_DATA_EXCEPTION_CREATE_ACTION);
    }

    private static final OptionalDataExceptionCreateAction OPTIONAL_DATA_EXCEPTION_CREATE_ACTION = new OptionalDataExceptionCreateAction();

    private static final class OptionalDataExceptionCreateAction implements PrivilegedAction<OptionalDataException> {

        private final Constructor<OptionalDataException> constructor;

        private OptionalDataExceptionCreateAction() {
            constructor = AccessController.doPrivileged(new PrivilegedAction<Constructor<OptionalDataException>>() {
                public Constructor<OptionalDataException> run() {
                    try {
                        final Constructor<OptionalDataException> constructor = OptionalDataException.class.getDeclaredConstructor(boolean.class);
                        constructor.setAccessible(true);
                        return constructor;
                    } catch (NoSuchMethodException e) {
                        throw new NoSuchMethodError(e.getMessage());
                    }
                }
            });
        }

        public OptionalDataException run() {
            try {
                return constructor.newInstance(Boolean.FALSE);
            } catch (InstantiationException e) {
                throw new InstantiationError(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Error invoking constructor", e);
            }
        }
    }

    private static final Externalizer NULL_EXTERNALIZER = new AbstractExternalizer() {
        private static final long serialVersionUID = 1L;

        public void writeExternal(final Object subject, final ObjectOutput output) throws IOException {
        }

        public void readExternal(final Object subject, final ObjectInput input) throws IOException, ClassNotFoundException {
        }
    };

    /**
     * Get a null externalizer.  Useful in conjunction with {@link org.jboss.marshalling.ObjectTable} entries.
     * This externalizer reads and writes no data.
     *
     * @return the null externalizer
     */
    public static Externalizer nullExternalizer() {
        return NULL_EXTERNALIZER;
    }
}
