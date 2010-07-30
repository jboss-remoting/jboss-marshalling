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

package org.jboss.marshalling;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;

public abstract class AbstractObjectInput extends SimpleDataInput implements ObjectInput {

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
