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
import java.io.ObjectOutput;

/**
 * An abstract object output implementation.
 */
public abstract class AbstractObjectOutput extends SimpleDataOutput implements ObjectOutput {

    /**
     * Construct a new instance.
     *
     * @param bufferSize the buffer size
     */
    protected AbstractObjectOutput(final int bufferSize) {
        super(bufferSize);
    }

    /**
     * Implementation of the actual object-writing method.
     *
     * @param obj the object to write
     * @param unshared {@code true} if the instance is unshared, {@code false} if it is shared
     * @throws IOException if an I/O error occurs
     */
    protected abstract void doWriteObject(Object obj, boolean unshared) throws IOException;

    /**
     * {@inheritDoc}
     */
    public void writeObjectUnshared(Object obj) throws IOException {
        doWriteObject(obj, true);
    }

    /**
     * {@inheritDoc}
     */
    public void writeObject(Object obj) throws IOException {
        doWriteObject(obj, false);
    }
}
