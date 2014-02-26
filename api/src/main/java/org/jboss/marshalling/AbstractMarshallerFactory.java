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
