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

public final class ClonerConfiguration implements Cloneable {

    private CloneTable cloneTable;
    private ObjectResolver objectResolver;
    private ClassCloner classCloner;
    private SerializabilityChecker serializabilityChecker;
    private Creator externalizedCreator;
    private Creator serializedCreator;
    private int bufferSize;

    public ClonerConfiguration clone() {
        try {
            return (ClonerConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public CloneTable getCloneTable() {
        return cloneTable;
    }

    public void setCloneTable(final CloneTable cloneTable) {
        this.cloneTable = cloneTable;
    }

    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public void setObjectResolver(final ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    public ClassCloner getClassCloner() {
        return classCloner;
    }

    public void setClassCloner(final ClassCloner classCloner) {
        this.classCloner = classCloner;
    }

    public SerializabilityChecker getSerializabilityChecker() {
        return serializabilityChecker;
    }

    public void setSerializabilityChecker(final SerializabilityChecker serializabilityChecker) {
        this.serializabilityChecker = serializabilityChecker;
    }

    public Creator getExternalizedCreator() {
        return externalizedCreator;
    }

    public void setExternalizedCreator(final Creator externalizedCreator) {
        this.externalizedCreator = externalizedCreator;
    }

    public Creator getSerializedCreator() {
        return serializedCreator;
    }

    public void setSerializedCreator(final Creator serializedCreator) {
        this.serializedCreator = serializedCreator;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
