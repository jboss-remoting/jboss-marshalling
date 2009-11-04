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

import org.jboss.marshalling.reflect.SunReflectiveCreator;

/**
 * An abstract implementation of the {@code MarshallerFactory} interface.  This
 * instance can be configured at any time; however any marshallers and unmarshallers
 * produced will have a configuration based on a snapshot of the values of this instance's
 * configuration.
 */
public abstract class AbstractMarshallerFactory implements MarshallerFactory {
    /**
     * Construct a new marshaller factory instance.
     */
    protected AbstractMarshallerFactory() {
    }

    /**
     * Get the default class externalizer factory, which is used if none was configured.  This base implementation returns a
     * no-operation class externalizer factory.
     *
     * @return the class externalizer factory
     */
    protected ClassExternalizerFactory getDefaultClassExternalizerFactory() {
        return Marshalling.nullClassExternalizerFactory();
    }

    /**
     * Get the default stream header, which is used if none was configured.  This base implementation returns a
     * no-operation stream header (writes and reads no bytes).
     *
     * @return the stream header
     */
    protected StreamHeader getDefaultStreamHeader() {
        return Marshalling.nullStreamHeader();
    }

    /**
     * Get the default object resolver, which is used if none was configured.  This base implementation returns an
     * identity object resolver.
     *
     * @return the object resolver
     */
    protected ObjectResolver getDefaultObjectResolver() {
        return Marshalling.nullObjectResolver();
    }

    /**
     * Get the default class resolver, which is used if none was configured.  This base implementation returns a
     * new {@link ContextClassResolver} instance.
     *
     * @return the class resolver
     */
    protected ClassResolver getDefaultClassResolver() {
        return new ContextClassResolver();
    }

    /**
     * Get the default object creator, which is used if none was configured.  This base implementation returns a
     * simple reflective creator.
     *
     * @return the creator
     */
    protected Creator getDefaultCreator() {
        return new SunReflectiveCreator();
    }

    /**
     * Get the default class table, which is used if none was configured.  This base implementation returns a
     * no-operation class table.
     *
     * @return the class table
     */
    protected ClassTable getDefaultClassTable() {
        return Marshalling.nullClassTable();
    }

    /**
     * Get the default object, which is used if none was configured.  This base implementation returns a
     * no-operation object table.
     *
     * @return the object table
     */
    protected ObjectTable getDefaultObjectTable() {
        return Marshalling.nullObjectTable();
    }

    /**
     * Get the default version, which is used if none was configured.  This base implementation returns -1.
     *
     * @return the default version to use
     */
    protected int getDefaultVersion() {
        return -1;
    }

    /**
     * Get the default buffer size, which is used if none was configured.  This base implementation returns 512.
     *
     * @return the default buffer size
     */
    protected int getDefaultBufferSize() {
        return 512;
    }

    /**
     * Get the minimum buffer size.  This base implementation returns 64.
     *
     * @return the minimum buffer size
     */
    protected int getMinimumBufferSize() {
        return 64;
    }
}
