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

/**
 * A Marshaller configuration.
 * @apiviz.landmark
 */
public final class MarshallingConfiguration implements Cloneable {

    private ClassExternalizerFactory classExternalizerFactory;
    private StreamHeader streamHeader;
    private ClassResolver classResolver;
    private ObjectResolver objectResolver;
    private Creator creator;
    private Creator externalizerCreator;
    private Creator serializedCreator;
    private ClassTable classTable;
    private ObjectTable objectTable;
    private ExceptionListener exceptionListener;
    private SerializabilityChecker serializabilityChecker;
    private int instanceCount = 256;
    private int classCount = 64;
    private int bufferSize = 512;
    private int version = -1;
    private ObjectResolver objectPreResolver;

    /**
     * Construct a new instance.
     */
    public MarshallingConfiguration() {
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
     * Get the object creator, or {@code null} if none is specified.  Used only if a more specific creator type
     * was not specified.
     *
     * @return the object creator
     *
     * @deprecated no longer used in 1.4, will be removed in a future version
     */
    @Deprecated
    public Creator getCreator() {
        return creator;
    }

    /**
     * Set the object creator, or {@code null} to use the default.  Used only if a more specific creator type
     * was not specified.
     *
     * @param creator the object creator
     *
     * @deprecated no longer used in 1.4, will be removed in a future version
     */
    @Deprecated
    public void setCreator(final Creator creator) {
        this.creator = creator;
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
     * Get the creator to use for externalized objects.  If none was configured, returns the result of
     * {@link #getCreator()}.
     *
     * @return the creator
     *
     * @deprecated no longer used in 1.4, will be removed in 1.4
     */
    @Deprecated
    public Creator getExternalizerCreator() {
        final Creator result = externalizerCreator;
        return result == null ? creator : result;
    }

    /**
     * Set the creator to use for externalized objects.
     *
     * @param externalizerCreator the creator
     *
     * @deprecated no longer used in 1.4, will be removed in 1.4
     */
    @Deprecated
    public void setExternalizerCreator(final Creator externalizerCreator) {
        this.externalizerCreator = externalizerCreator;
    }

    /**
     * Get the creator to use for serialized objects.  If none was configured, returns the result of
     * {@link #getCreator()}.
     *
     * @return the creator
     *
     * @deprecated no longer used in 1.4, will be removed in 1.4
     */
    @Deprecated
    public Creator getSerializedCreator() {
        final Creator result = serializedCreator;
        return result == null ? creator : result;
    }

    /**
     * Set the creator to use for serialized objects.
     *
     * @param serializedCreator the creator
     *
     * @deprecated no longer used in 1.4, will be removed in 1.4
     */
    @Deprecated
    public void setSerializedCreator(final Creator serializedCreator) {
        this.serializedCreator = serializedCreator;
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
        builder.append(super.toString()).append(": ");
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
            builder.append("exceptionListener=<").append(objectTable).append("> ");
        }
        builder.append("instanceCount=").append(instanceCount);
        builder.append(" classCount=").append(classCount);
        builder.append(" bufferSize=").append(bufferSize);
        builder.append(" version=").append(version);
        return builder.toString();
    }
}
