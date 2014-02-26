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

import java.io.IOException;

/**
 * An abstract implementation of the {@code Unmarshaller} interface.  Most of the
 * write methods delegate directly to the current data output.
 */
public abstract class AbstractUnmarshaller extends AbstractObjectInput implements Unmarshaller {

    /** The configured class externalizer factory. */
    protected final ClassExternalizerFactory classExternalizerFactory;
    /** The configured stream header. */
    protected final StreamHeader streamHeader;
    /** The configured class resolver. */
    protected final ClassResolver classResolver;
    /** The configured object resolver. */
    protected final ObjectResolver objectResolver;
    /** The configured object pre resolver. */
    protected final ObjectResolver objectPreResolver;
    /** The configured class table. */
    protected final ClassTable classTable;
    /** The configured object table. */
    protected final ObjectTable objectTable;
    /** The configured exception listener. */
    protected final ExceptionListener exceptionListener;
    /** The configured serializability checker. */
    protected final SerializabilityChecker serializabilityChecker;
    /** The configured version. */
    protected final int configuredVersion;

    /**
     * Construct a new unmarshaller instance.
     *
     * @param marshallerFactory the marshaller factory
     * @param configuration
     */
    protected AbstractUnmarshaller(final AbstractMarshallerFactory marshallerFactory, final MarshallingConfiguration configuration) {
        super(configuration.getBufferSize());
        final ClassExternalizerFactory classExternalizerFactory = configuration.getClassExternalizerFactory();
        this.classExternalizerFactory = classExternalizerFactory == null ? marshallerFactory.getDefaultClassExternalizerFactory() : classExternalizerFactory;
        final StreamHeader streamHeader = configuration.getStreamHeader();
        this.streamHeader = streamHeader == null ? marshallerFactory.getDefaultStreamHeader() : streamHeader;
        final ClassResolver classResolver = configuration.getClassResolver();
        this.classResolver = classResolver == null ? marshallerFactory.getDefaultClassResolver() : classResolver;
        final ObjectResolver objectResolver = configuration.getObjectResolver();
        this.objectResolver = objectResolver == null ? marshallerFactory.getDefaultObjectResolver() : objectResolver;
        final ObjectResolver objectPreResolver = configuration.getObjectPreResolver();
        this.objectPreResolver = objectPreResolver == null ? marshallerFactory.getDefaultObjectResolver() : objectPreResolver;
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

    /** {@inheritDoc} */
    public void start(final ByteInput byteInput) throws IOException {
        this.byteInput = byteInput;
        position = limit = 0;
        streamHeader.readHeader(this);
    }

    /** {@inheritDoc} */
    public void finish() throws IOException {
        limit = -1;
        position = 0;
        byteInput = null;
        clearClassCache();
    }
}
