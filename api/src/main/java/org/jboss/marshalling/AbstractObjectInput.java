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
import java.io.InvalidObjectException;
import java.io.ObjectInput;

/**
 * An abstract object input implementation.
 */
public abstract class AbstractObjectInput extends SimpleDataInput implements ObjectInput {

    /**
     * Construct a new instance.
     *
     * @param bufferSize the buffer size to use
     */
    protected AbstractObjectInput(int bufferSize) {
        super(bufferSize);
    }

    /** {@inheritDoc} */
    public final Object readObject() throws ClassNotFoundException, IOException {
        return doReadObject(false);
    }

    /**
     * Read and return an unshared object.
     *
     * @return an unshared object
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws IOException if an error occurs
     */
    public final Object readObjectUnshared() throws ClassNotFoundException, IOException {
        return doReadObject(true);
    }

    /**
     * Implementation of the actual object-reading method.
     *
     * @param unshared {@code true} if the instance should be unshared, {@code false} if it is shared
     * @return the object to read
     * @throws ClassNotFoundException if the class for the object could not be loaded
     * @throws IOException if an I/O error occurs
     */
    protected abstract Object doReadObject(boolean unshared) throws ClassNotFoundException, IOException;

    /**
     * Read and return an object, cast to a specific type.
     *
     * @param type the object class
     * @param <T> the object type
     * @return the object read from the stream
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws InvalidObjectException if the object is not of the expected type
     * @throws IOException if an error occurs
     */
    public <T> T readObject(final Class<T> type) throws ClassNotFoundException, IOException {
        final Object obj = doReadObject(false);
        try {
            return type.cast(obj);
        } catch (ClassCastException e) {
            throw wrongType(e, type, obj.getClass());
        }
    }

    /**
     * Read and return an unshared object, cast to a specific type.
     *
     * @param type the object class
     * @param <T> the object type
     * @return an unshared object
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws InvalidObjectException if the object is not of the expected type
     * @throws IOException if an error occurs
     */
    public <T> T readObjectUnshared(final Class<T> type) throws ClassNotFoundException, IOException {
        final Object obj = doReadObject(true);
        try {
            return type.cast(obj);
        } catch (ClassCastException e) {
            throw wrongType(e, type, obj.getClass());
        }
    }

    private static InvalidObjectException wrongType(final ClassCastException e, final Class<?> expected, final Class<?> actual) {
        final InvalidObjectException ioe = new InvalidObjectException("Object is of the wrong type (expected " + expected + ", got " + actual + ")");
        ioe.initCause(e);
        return ioe;
    }
}
