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
import java.io.InvalidClassException;

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
    /** The input filter to stop deserialization process if needed. */
    protected final InputFilter inputFilter;

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
        final InputFilter inputFilter = configuration.getInputFilter();
        this.inputFilter = inputFilter == null ? info -> InputFilter.Status.UNDECIDED : inputFilter;
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

    protected void checkInput(InputFilter.FilterInfo info) throws InvalidClassException {
        if (InputFilter.Status.REJECTED == inputFilter.checkInput(info)) {
            throw new InvalidClassException("Rejected by input filter: " + info);
        }
    }

    protected final class FilterInfo implements InputFilter.FilterInfo {
        final Class<?> serialClass;
        final long arrayLength;
        final long depth;
        final long references;
        final long streamBytes;

        public FilterInfo(Class<?> serialClass, long arrayLength, long depth, long references, long streamBytes) {
            this.serialClass = serialClass;
            this.arrayLength = arrayLength;
            this.depth = depth;
            this.references = references;
            this.streamBytes = streamBytes;
        }

        @Override
        public Class<?> serialClass() {
            return serialClass;
        }

        @Override
        public long arrayLength() {
            return arrayLength;
        }

        @Override
        public long depth() {
            return depth;
        }

        @Override
        public long references() {
            return references;
        }

        @Override
        public long streamBytes() {
            return streamBytes;
        }

        @Override
        public String toString() {
            return String.format("serialClass: %s, arrayLength: %s, depth: %s, references: %s, streamBytes: %s", serialClass, arrayLength, depth, references, streamBytes);
        }
    }
}
