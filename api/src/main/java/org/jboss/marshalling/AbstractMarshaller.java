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

/**
 * An abstract implementation of the {@code Marshaller} interface.  Most of the
 * write methods delegate directly to the current data output.
 */
public abstract class AbstractMarshaller extends AbstractObjectOutput implements Marshaller {

    /** The configured class externalizer factory. */
    protected final ClassExternalizerFactory classExternalizerFactory;
    /** The configured stream header. */
    protected final StreamHeader streamHeader;
    /** The configured class resolver. */
    protected final ClassResolver classResolver;
    /** The configured object resolver. */
    protected final ObjectResolver objectResolver;
    /** The configured class table. */
    protected final ClassTable classTable;
    /** The configured object table. */
    protected final ObjectTable objectTable;
    /** The configured exception listener. */
    protected final ExceptionListener exceptionListener;
    /** The configured serializability checker. */
    protected final SerializabilityChecker serializabilityChecker;
    /** The configured version to write. */
    protected final int configuredVersion;

    /**
     * Construct a new marshaller instance.
     *
     * @param marshallerFactory the marshaller factory
     * @param configuration
     */
    protected AbstractMarshaller(final AbstractMarshallerFactory marshallerFactory, final MarshallingConfiguration configuration) {
        super(calcBufferSize(marshallerFactory, configuration));
        final ClassExternalizerFactory classExternalizerFactory = configuration.getClassExternalizerFactory();
        this.classExternalizerFactory = classExternalizerFactory == null ? marshallerFactory.getDefaultClassExternalizerFactory() : classExternalizerFactory;
        final StreamHeader streamHeader = configuration.getStreamHeader();
        this.streamHeader = streamHeader == null ? marshallerFactory.getDefaultStreamHeader() : streamHeader;
        final ClassResolver classResolver = configuration.getClassResolver();
        this.classResolver = classResolver == null ? marshallerFactory.getDefaultClassResolver() : classResolver;
        final ObjectResolver objectResolver = configuration.getObjectResolver();
        this.objectResolver = objectResolver == null ? marshallerFactory.getDefaultObjectResolver() : objectResolver;
        final ClassTable classTable = configuration.getClassTable();
        this.classTable = classTable == null ? marshallerFactory.getDefaultClassTable() : classTable;
        final ObjectTable objectTable = configuration.getObjectTable();
        this.objectTable = objectTable == null ? marshallerFactory.getDefaultObjectTable() : objectTable;
        final ExceptionListener exceptionListener = configuration.getExceptionListener();
        this.exceptionListener = exceptionListener == null ? ExceptionListener.NO_OP : exceptionListener;
        final SerializabilityChecker serializabilityChecker = configuration.getSerializabilityChecker();
        this.serializabilityChecker = serializabilityChecker == null ? SerializabilityChecker.DEFAULT : serializabilityChecker;
        final int configuredVersion = configuration.getVersion();
        this.configuredVersion = configuredVersion == -1 ? marshallerFactory.getDefaultVersion() : configuredVersion;
    }

    private static int calcBufferSize(final AbstractMarshallerFactory marshallerFactory, final MarshallingConfiguration configuration) {
        final int minBufSize = marshallerFactory.getMinimumBufferSize();
        final int bufferSize = configuration.getBufferSize();
        return bufferSize == -1 ? marshallerFactory.getDefaultBufferSize() : bufferSize < minBufSize ? minBufSize : bufferSize;
    }

    /** {@inheritDoc} */
    public void start(final ByteOutput byteOutput) throws IOException {
        this.byteOutput = byteOutput;
        buffer = new byte[bufferSize];
        streamHeader.writeHeader(this);
    }

    /** {@inheritDoc} */
    public final void writeObjectUnshared(final Object obj) throws IOException {
        try {
            super.writeObjectUnshared(obj);
        } catch (IOException e) {
            TraceInformation.addObjectInformation(e, obj);
            exceptionListener.handleMarshallingException(e, obj);
            throw e;
        } catch (RuntimeException e) {
            TraceInformation.addObjectInformation(e, obj);
            exceptionListener.handleMarshallingException(e, obj);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public final void writeObject(final Object obj) throws IOException {
        try {
            super.writeObject(obj);
        } catch (IOException e) {
            TraceInformation.addObjectInformation(e, obj);
            exceptionListener.handleMarshallingException(e, obj);
            throw e;
        } catch (RuntimeException e) {
            TraceInformation.addObjectInformation(e, obj);
            exceptionListener.handleMarshallingException(e, obj);
            throw e;
        }
    }

    /** {@inheritDoc} */
    public void finish() throws IOException {
        try {
            super.finish();
        } finally {
            clearClassCache();
        }
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        finish();
    }
}
