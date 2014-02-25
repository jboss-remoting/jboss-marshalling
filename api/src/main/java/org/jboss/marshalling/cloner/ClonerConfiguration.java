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

package org.jboss.marshalling.cloner;

import org.jboss.marshalling.Creator;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.SerializabilityChecker;
import org.jboss.marshalling.reflect.PublicReflectiveCreator;
import org.jboss.marshalling.reflect.SunReflectiveCreator;

/**
 * The configuration for a cloner instance.
 */
public final class ClonerConfiguration implements Cloneable {

    private CloneTable cloneTable;
    private ObjectResolver objectResolver;
    private ObjectResolver objectPreResolver;
    private ClassCloner classCloner;
    private SerializabilityChecker serializabilityChecker;
    private Creator externalizedCreator;
    private Creator serializedCreator;
    private int bufferSize;

    /**
     * Create a copy of this configuration.
     *
     * @return the copy
     */
    public ClonerConfiguration clone() {
        try {
            return (ClonerConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the clone table.  This can be used to intercede in the cloning process to choose specific existing instances
     * or a cloning strategy depending on the source object.
     *
     * @return the clone table
     */
    public CloneTable getCloneTable() {
        return cloneTable;
    }

    /**
     * Set the clone table.
     *
     * @param cloneTable the clone table
     * @see #getCloneTable()
     */
    public void setCloneTable(final CloneTable cloneTable) {
        this.cloneTable = cloneTable;
    }

    /**
     * Get the object resolver.  The object resolver can be used to perform object substitution before and after cloning.
     *
     * @return the object resolver
     */
    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    /**
     * Set the object resolver.
     *
     * @param objectResolver the object resolver
     * @see #getObjectResolver()
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
     * @param objectResolver the object resolver
     */
    public void setObjectPreResolver(final ObjectResolver objectPreResolver) {
        this.objectPreResolver = objectPreResolver;
    }

    /**
     * Get the class cloner.  This is used by the serializing cloner to implement a strategy for mapping classes from
     * one "side" of the cloner to the other.
     *
     * @return the class cloner
     */
    public ClassCloner getClassCloner() {
        return classCloner;
    }

    /**
     * Set the class cloner.
     *
     * @param classCloner the class cloner
     * @see #getClassCloner()
     */
    public void setClassCloner(final ClassCloner classCloner) {
        this.classCloner = classCloner;
    }

    /**
     * Get the serializability checker.  This is used by the serializing cloner to determine whether a class may be
     * considered serializable (and thus cloneable).  By default, the standard serializability checker is used.
     *
     * @return the serializability checker
     * @see SerializabilityChecker#DEFAULT
     */
    public SerializabilityChecker getSerializabilityChecker() {
        return serializabilityChecker;
    }

    /**
     * Set the serializability checker.
     *
     * @param serializabilityChecker the serializability checker
     * @see #getSerializabilityChecker()
     */
    public void setSerializabilityChecker(final SerializabilityChecker serializabilityChecker) {
        this.serializabilityChecker = serializabilityChecker;
    }

    /**
     * Get the creator to use for externalizable objects.  This is used by the serializing cloner.
     *
     * @return the creator to use for externalizable objects
     * @see PublicReflectiveCreator
     */
    public Creator getExternalizedCreator() {
        return externalizedCreator;
    }

    /**
     * Set the creator to use for externalizable objects.
     *
     * @param externalizedCreator the creator to use for externalizable objects
     * @see #getExternalizedCreator()
     */
    public void setExternalizedCreator(final Creator externalizedCreator) {
        this.externalizedCreator = externalizedCreator;
    }

    /**
     * Get the creator to use for serialized objects.  This is used by the serializing cloner.
     *
     * @return the creator to use for serialized objects
     * @see SunReflectiveCreator
     */
    public Creator getSerializedCreator() {
        return serializedCreator;
    }

    /**
     * Set the creator to use for serialized objects.
     *
     * @param serializedCreator the creator to use for serialized objects
     */
    public void setSerializedCreator(final Creator serializedCreator) {
        this.serializedCreator = serializedCreator;
    }

    /**
     * Get the buffer size to use.  This is used by the serializing cloner.
     *
     * @return the buffer size to use
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the buffer size to use.
     *
     * @param bufferSize the buffer size to use
     * @see #getBufferSize()
     */
    public void setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
