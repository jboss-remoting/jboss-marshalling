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

import java.io.ObjectInputFilter;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A Marshaller configuration.
 * @apiviz.landmark
 */
public final class MarshallingConfiguration implements Cloneable {

    private static final String IGNORE_STATIC_JVM_SERIAL_FILTER_PROPERTY = "org.jboss.marshalling.ignore-static-jvm-serial-filter";

    private ClassExternalizerFactory classExternalizerFactory;
    private StreamHeader streamHeader;
    private ClassNameTransformer classNameTransformer;
    private ClassResolver classResolver;
    private ObjectResolver objectResolver;
    private ClassTable classTable;
    private ObjectTable objectTable;
    private ExceptionListener exceptionListener;
    private SerializabilityChecker serializabilityChecker;
    private UnmarshallingObjectInputFilter unmarshallingFilter;

    private int instanceCount = 256;
    private int classCount = 64;
    private int bufferSize = 512;
    private int version = -1;
    private ObjectResolver objectPreResolver;

    /**
     * Construct a new instance.
     */
    public MarshallingConfiguration() {
        unmarshallingFilter = createDefaultUnmarshallingFilter();
    }

    /**
     * Get the class externalizer factory, or {@code null} if none is specified.
     *
     * @return the class externalizer factory
     */
    public ClassExternalizerFactory getClassExternalizerFactory() {
        return classExternalizerFactory;
    }

    /**
     * Set the class externalizer factory.  Specify {@code null} to use none.
     *
     * @param classExternalizerFactory the class externalizer factory
     */
    public void setClassExternalizerFactory(final ClassExternalizerFactory classExternalizerFactory) {
        this.classExternalizerFactory = classExternalizerFactory;
    }

    /**
     * Get the stream header, or {@code null} if none is specified.
     *
     * @return the stream header
     */
    public StreamHeader getStreamHeader() {
        return streamHeader;
    }

    /**
     * Set the stream header.  Specify {@code null} to use none.
     *
     * @param streamHeader the stream header
     */
    public void setStreamHeader(final StreamHeader streamHeader) {
        this.streamHeader = streamHeader;
    }

    /**
     * Get the class resolver, or {@code null} if none is specified.
     *
     * @return the class resolver
     */
    public ClassResolver getClassResolver() {
        return classResolver;
    }

    /**
     * Set the class resolver, or {@code null} to use the default.
     *
     * @param classResolver the class resolver
     */
    public void setClassResolver(final ClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    /**
     * Get the object resolver, or {@code null} if none is specified.
     *
     * @return the object resolver
     */
    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    /**
     * Set the object resolver, or {@code null} to use none.
     *
     * @param objectResolver the object resolver
     */
    public void setObjectResolver(final ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    /**
     * Get the class name transformer, or {@code null} if none is specified.
     *
     * @return the class name transformer
     */
    public ClassNameTransformer getClassNameTransformer() {
        return classNameTransformer;
    }

    /**
     * Set the class name transformer, or {@code null} to use none.
     *
     * @param classNameTransformer class name transformer
     */
    public void setClassNameTransformer(final ClassNameTransformer classNameTransformer) {
        this.classNameTransformer = classNameTransformer;
    }

    /**
     * Get the object pre resolver, or {@code null} if none is specified.
     *
     * @return the object resolver
     */
    public ObjectResolver getObjectPreResolver() {
        return objectPreResolver;
    }

    /**
     * Set the object pre resolver, or {@code null} to use none.
     * Invoked before user replacement and global object resolver
     *
     * @param objectPreResolver the object resolver
     */
    public void setObjectPreResolver(final ObjectResolver objectPreResolver) {
        this.objectPreResolver = objectPreResolver;
    }

    
    /**
     * Get the class table, or {@code null} if none is specified.
     *
     * @return the class table
     */
    public ClassTable getClassTable() {
        return classTable;
    }

    /**
     * Set the class table, or {@code null} to use none.
     *
     * @param classTable the class table
     */
    public void setClassTable(final ClassTable classTable) {
        this.classTable = classTable;
    }

    /**
     * Get the object table, or {@code null} if none is specified.
     *
     * @return the object table
     */
    public ObjectTable getObjectTable() {
        return objectTable;
    }

    /**
     * Set the object table, or {@code null} to use none.
     *
     * @param objectTable the object table
     */
    public void setObjectTable(final ObjectTable objectTable) {
        this.objectTable = objectTable;
    }

    /**
     * Get the estimated instance count for this configuration.
     *
     * @return the instance count
     */
    public int getInstanceCount() {
        return instanceCount;
    }

    /**
     * Set the estimated instance count for this configuration.  The given value is used to pre-size certain internal
     * tables in some implementations.
     *
     * @param instanceCount the instance count
     */
    public void setInstanceCount(final int instanceCount) {
        this.instanceCount = instanceCount;
    }

    /**
     * Get the estimated class count for this configuration.
     *
     * @return the class count
     */
    public int getClassCount() {
        return classCount;
    }

    /**
     * Set the estimated class count for this configuration.  The given value is used to pre-size certain internal tables
     * in some implementations.
     *
     * @param classCount the class count
     */
    public void setClassCount(final int classCount) {
        this.classCount = classCount;
    }

    /**
     * Get the configured buffer size.
     *
     * @return the buffer size, in bytes
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the configured buffer size.  Some implementations will use this value to set the size of internal read/write
     * buffers.
     *
     * @param bufferSize the buffer size, in bytes
     */
    public void setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Get the version to use, for protocols with multiple versions.
     *
     * @return the version to use
     */
    public int getVersion() {
        return version;
    }

    /**
     * Set the version to use, for protocols with multiple versions.
     *
     * @param version the version to use
     */
    public void setVersion(final int version) {
        this.version = version;
    }

    /**
     * Get the exception listener to use.
     *
     * @return the exception listener
     */
    public ExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    /**
     * Set the exception listener to use.
     *
     * @param exceptionListener the exception listener
     */
    public void setExceptionListener(final ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    /**
     * Get the serializability checker.
     *
     * @return the serializability checker
     */
    public SerializabilityChecker getSerializabilityChecker() {
        return serializabilityChecker;
    }

    /**
     * Set the serializability checker.
     *
     * @param serializabilityChecker the new serializability checker
     */
    public void setSerializabilityChecker(final SerializabilityChecker serializabilityChecker) {
        this.serializabilityChecker = serializabilityChecker;
    }

    /**
     * Get the unmarshalling filter.
     *
     * @return the unmarshalling filter
     */
    public UnmarshallingObjectInputFilter getUnmarshallingFilter() {
        return unmarshallingFilter;
    }

    /**
     * Set the unmarshalling filter.
     *
     * @param unmarshallingFilter the new unmarshalling filter
     */
    public void setUnmarshallingFilter(UnmarshallingObjectInputFilter unmarshallingFilter) {
        this.unmarshallingFilter = unmarshallingFilter;
    }

    private UnmarshallingObjectInputFilter createDefaultUnmarshallingFilter() {
        String property;
        if (System.getSecurityManager() == null) {
            property = System.getProperty(IGNORE_STATIC_JVM_SERIAL_FILTER_PROPERTY);
        } else {
            property = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(IGNORE_STATIC_JVM_SERIAL_FILTER_PROPERTY));
        }

        if (!Boolean.parseBoolean(property)) {
            return getJEPS290ProcessWideFilter();
        }
        return null;
    }

    /**
     * Returns an adapter instance for the static JVM-wide deserialization filter (-DserialFilter=...) or null.
     */
    static UnmarshallingObjectInputFilter getJEPS290ProcessWideFilter() {
        ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
        if (serialFilter != null) {
            return new UnmarshallingObjectInputFilterAdapter(serialFilter);
        }
        return null;
    }

    /**
     * Create a shallow clone.
     *
     * @return a clone
     */
    public MarshallingConfiguration clone() {
        try {
            return (MarshallingConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            // not possible
            throw new IllegalStateException();
        }
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append(": ");
        if (classExternalizerFactory != null) {
            builder.append("classExternalizerFactory=<").append(classExternalizerFactory.toString()).append("> ");
        }
        if (streamHeader != null) {
            builder.append("streamHeader=<").append(streamHeader).append("> ");
        }
        if (classResolver != null) {
            builder.append("classResolver=<").append(classResolver).append("> ");
        }
        if (objectResolver != null) {
            builder.append("objectResolver=<").append(objectResolver).append("> ");
        }
        if (classTable != null) {
            builder.append("classTable=<").append(classTable).append("> ");
        }
        if (objectTable != null) {
            builder.append("objectTable=<").append(objectTable).append("> ");
        }
        if (exceptionListener != null) {
            builder.append("exceptionListener=<").append(exceptionListener).append("> ");
        }
        if (serializabilityChecker != null) {
            builder.append("serializabilityChecker=<").append(serializabilityChecker).append("> ");
        }
        if (unmarshallingFilter != null) {
            builder.append("unmarshallingFilter=<").append(unmarshallingFilter).append("> ");
        }
        builder.append("instanceCount=").append(instanceCount);
        builder.append(" classCount=").append(classCount);
        builder.append(" bufferSize=").append(bufferSize);
        builder.append(" version=").append(version);
        return builder.toString();
    }
}
