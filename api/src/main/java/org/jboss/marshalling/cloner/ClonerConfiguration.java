/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling.cloner;

import org.jboss.marshalling.Creator;
import org.jboss.marshalling.ObjectResolver;

public final class ClonerConfiguration implements Cloneable {

    private ObjectCloner delegate;
    private ObjectResolver objectResolver;
    private ClassCloner classCloner;
    private Creator externalizedCreator;
    private Creator serializedCreator;
    private int bufferSize;

    protected ClonerConfiguration clone() {
        try {
            return (ClonerConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public ObjectCloner getDelegate() {
        return delegate;
    }

    public void setDelegate(final ObjectCloner delegate) {
        this.delegate = delegate;
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
