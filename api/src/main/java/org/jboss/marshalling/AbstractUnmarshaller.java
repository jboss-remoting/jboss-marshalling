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
import java.io.ObjectInputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    /** The configured unmarshalling filter */
    protected final UnmarshallingObjectInputFilter unmarshallingFilter;
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
        final ClassNameTransformer classNameTransformer = configuration.getClassNameTransformer();
        if (classNameTransformer != null) {
            this.classResolver = new TransformingClassResolver(classResolver == null ? marshallerFactory.getDefaultClassResolver() : classResolver, classNameTransformer, true);
        } else {
            this.classResolver = classResolver == null ? marshallerFactory.getDefaultClassResolver() : classResolver;
        }
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
        final UnmarshallingObjectInputFilter unmarshallingFilter = configuration.getUnmarshallingFilter();
        this.unmarshallingFilter = unmarshallingFilter == null ? UnmarshallingObjectInputFilter.ACCEPTING : unmarshallingFilter;
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

    protected final void filterCheck(final Class<?> unmarshallClass, final long arrayLength, final long depth,
                                final long references, final long streamBytes) throws InvalidClassException {
        if (unmarshallingFilter != UnmarshallingObjectInputFilter.ACCEPTING) {
            UnmarshallingObjectInputFilter.FilterInfo filterInfo = new FilterInputImpl(unmarshallClass, arrayLength, depth, references, streamBytes);
            UnmarshallingObjectInputFilter.Status status = null;
            RuntimeException ex = null;
            try {
                status = unmarshallingFilter.checkInput(filterInfo);
            } catch (RuntimeException re) {
                ex = re;
                InvalidClassException ice = new InvalidClassException(String.format("Filtering failed for %s", filterInfo));
                ice.initCause(re);
                throw ice;
            } finally {
                Logging.logFilterResponse(filterInfo, status, ex);
            }

            if (status == UnmarshallingObjectInputFilter.Status.REJECTED) {
                throw new InvalidClassException(String.format("Filtering rejected %s", status));
            }
        }
    }

    protected static void setObjectInputStreamFilter(ObjectInputStream ois, UnmarshallingObjectInputFilter filter) {
        JDKSpecific.setObjectInputStreamFilter(ois, filter);
    }

    private static class FilterInputImpl implements UnmarshallingObjectInputFilter.FilterInfo {

        private final Class<?> unmarshallClass;
        private final long arrayLength;
        private final long depth;
        private final long references;
        private final long streamBytes;

        private FilterInputImpl(final Class<?> unmarshallClass, final long arrayLength, final long depth,
                                final long references, final long streamBytes) {
            this.unmarshallClass = unmarshallClass;
            this.arrayLength = arrayLength;
            this.depth = depth;
            this.references = references;
            this.streamBytes = streamBytes;
        }

        @Override
        public Class<?> getUnmarshalledClass() {
            return unmarshallClass;
        }

        @Override
        public long getArrayLength() {
            return arrayLength;
        }

        @Override
        public long getDepth() {
            return depth;
        }

        @Override
        public long getReferences() {
            return references;
        }

        @Override
        public long getStreamBytes() {
            return streamBytes;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString()).append(": ");
            if (unmarshallClass != null) {
                builder.append("unmarshallClass=<").append(unmarshallClass.toString()).append("> ");
            }
            builder.append("arrayLength=<").append(arrayLength).append("> ");
            builder.append("depth=<").append(depth).append("> ");
            builder.append("references=<").append(references).append("> ");
            builder.append("streamBytes=<").append(streamBytes).append("> ");
            return builder.toString();
        }
    }

    private static class Logging {
        static final Logger marshallingLogger = Logger.getLogger("org.jboss.marshalling");

        private static void logFilterResponse(final UnmarshallingObjectInputFilter.FilterInfo filterInfo,
                                              final UnmarshallingObjectInputFilter.Status status,
                                              final RuntimeException ex) {
            // Replicate logging message format from native deserialization filtering mechanism, except that the
            // logger name is "org.jboss.marshalling" instead of "java.io.serialization".
            if (status == null || status == UnmarshallingObjectInputFilter.Status.REJECTED) {
                logFilterResponse(filterInfo, status, ex, Level.FINER);
            } else {
                logFilterResponse(filterInfo, status, ex, Level.FINEST);
            }
        }

        private static void logFilterResponse(final UnmarshallingObjectInputFilter.FilterInfo filterInfo,
                                              final UnmarshallingObjectInputFilter.Status status,
                                              final RuntimeException ex,
                                              final Level level) {
            if (marshallingLogger.isLoggable(level)) {
                String message = String.format("UnmarshallingFilter %s: %s, array length: %d, nRefs: %d, depth: %d, bytes: %d, ex: %s",
                        status, filterInfo.getUnmarshalledClass(), filterInfo.getArrayLength(), filterInfo.getReferences(),
                        filterInfo.getDepth(), filterInfo.getStreamBytes(), Objects.toString(ex, "n/a"));
                if (Level.FINEST.equals(level)) {
                    marshallingLogger.log(level, message, ex);
                } else {
                    marshallingLogger.log(level, message);
                }
            }
        }
    }
}
