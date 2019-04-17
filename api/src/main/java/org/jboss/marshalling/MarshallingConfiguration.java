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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Objects;

/**
 * A Marshaller configuration.
 * @apiviz.landmark
 */
public final class MarshallingConfiguration implements Cloneable {

    private ClassExternalizerFactory classExternalizerFactory;
    private StreamHeader streamHeader;
    private ClassResolver classResolver;
    private ObjectResolver objectResolver;
    private ClassTable classTable;
    private ObjectTable objectTable;
    private ExceptionListener exceptionListener;
    private SerializabilityChecker serializabilityChecker;
    private int instanceCount = 256;
    private int classCount = 64;
    private int bufferSize = 512;
    private int version = -1;
    private ObjectResolver objectPreResolver;
    private InputFilter configuredFilter;
    private InputFilter serialFilter;

    /**
     * Construct a new instance.
     */
    public MarshallingConfiguration() {
        initFilter();
    }

    private void initFilter() {
        configuredFilter = AccessController.doPrivileged((PrivilegedAction<InputFilter>) () -> {
            String props = System.getProperty(SERIAL_FILTER_PROPNAME);
            if (props == null) {
                props = Security.getProperty(SERIAL_FILTER_PROPNAME);
            }
            if (props != null) {
                System.out.println("Creating serialization filter from " + props);
                try {
                    return createFilter(props);
                } catch (RuntimeException re) {
                    System.out.println("Error configuring filter: " + re);
                }
            }
            return null;
        });
        serialFilter = configuredFilter;
    }

    /**
     * Lock object for process-wide filter.
     */
    private Object serialFilterLock = new Object();

    /**
     * The name for the process-wide deserialization filter. Used as a system property and a java.security.Security property.
     */
    private final static String SERIAL_FILTER_PROPNAME = "jdk.serialFilter";

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
     * Get the serialFilter.
     *
     * @return the serialFilter
     */
    public InputFilter getSerialFilter() {
        synchronized (serialFilterLock) {
            return this.serialFilter;
        }
    }

    /**
     * Set the serialFilter.
     *
     * @param serialFilter the new serialFilter
     */
    public void setSerialFilter(InputFilter inputFilter) {
        Objects.requireNonNull(inputFilter, "inputFilter");
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ExtendedObjectStreamConstants.SERIAL_FILTER_PERMISSION);
        }
        synchronized (serialFilterLock) {
            if (serialFilter != null) {
                throw new IllegalStateException("Serial filter can only be set once");
            }
            this.serialFilter = inputFilter;
        }
    }

    /**
     * Returns an InputFilterFilter from a string of patterns.
     * <p>
     * Patterns are separated by ";" (semicolon). Whitespace is significant and
     * is considered part of the pattern.
     * If a pattern includes an equals assignment, "{@code =}" it sets a limit.
     * If a limit appears more than once the last value is used.
     * <ul>
     *     <li>maxdepth={@code value} - the maximum depth of a graph</li>
     *     <li>maxrefs={@code value}  - the maximum number of internal references</li>
     *     <li>maxbytes={@code value} - the maximum number of bytes in the input stream</li>
     *     <li>maxarray={@code value} - the maximum array length allowed</li>
     * </ul>
     * <p>
     * Other patterns match or reject class or package name
     * as returned from {@link Class#getName() Class.getName()} and
     * if an optional module name is present
     * {@link java.lang.reflect.Module#getName() class.getModule().getName()}.
     * Note that for arrays the element type is used in the pattern,
     * not the array type.
     * <ul>
     * <li>If the pattern starts with "!", the class is rejected if the remaining pattern is matched;
     *     otherwise the class is allowed if the pattern matches.
     * <li>If the pattern contains "/", the non-empty prefix up to the "/" is the module name;
     *     if the module name matches the module name of the class then
     *     the remaining pattern is matched with the class name.
     *     If there is no "/", the module name is not compared.
     * <li>If the pattern ends with ".**" it matches any class in the package and all subpackages.
     * <li>If the pattern ends with ".*" it matches any class in the package.
     * <li>If the pattern ends with "*", it matches any class with the pattern as a prefix.
     * <li>If the pattern is equal to the class name, it matches.
     * <li>Otherwise, the pattern is not matched.
     * </ul>
     * <p>
     * The resulting filter performs the limit checks and then
     * tries to match the class, if any. If any of the limits are exceeded,
     * the filter returns {@link Status#REJECTED Status.REJECTED}.
     * If the class is an array type, the class to be matched is the element type.
     * Arrays of any number of dimensions are treated the same as the element type.
     * For example, a pattern of "{@code !example.Foo}",
     * rejects creation of any instance or array of {@code example.Foo}.
     * The first pattern that matches, working from left to right, determines
     * the {@link Status#ALLOWED Status.ALLOWED}
     * or {@link Status#REJECTED Status.REJECTED} result.
     * If nothing matches, the result is {@link Status#UNDECIDED Status.UNDECIDED}.
     *
     * @param pattern the pattern string to parse; not null
     * @return a filter to check a class being deserialized; may be null;
     *          {@code null} if no patterns
     * @throws IllegalArgumentException
     *                if a limit is missing the name, or the long value
     *                is not a number or is negative,
     *                or the module name is missing if the pattern contains "/"
     *                or if the package is missing for ".*" and ".**"
     */
    public static InputFilter createFilter(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return InputFilter.Global.createFilter(pattern);
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
