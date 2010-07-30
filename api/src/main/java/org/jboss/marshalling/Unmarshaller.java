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

import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.IOException;

/**
 * An unmarshaller which reads objects from a stream.
 */
public interface Unmarshaller extends ObjectInput, ByteInput {
    /**
     * Read and return an unshared object.
     *
     * @return an unshared object
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws IOException if an error occurs
     */
    Object readObjectUnshared() throws ClassNotFoundException, IOException;

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
    <T> T readObject(Class<T> type) throws ClassNotFoundException, IOException;

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
    <T> T readObjectUnshared(Class<T> type) throws ClassNotFoundException, IOException;

    /**
     * Begin unmarshalling from a stream.
     *
     * @param newInput the new stream
     * @throws IOException if an error occurs during setup, such as an invalid header
     */
    void start(ByteInput newInput) throws IOException;

    /**
     * Discard the instance cache.
     *
     * @throws IOException if an error occurs
     */
    void clearInstanceCache() throws IOException;

    /**
     * Discard the class cache.  Implicitly also discards the instance cache.
     *
     * @throws IOException if an error occurs
     */
    void clearClassCache() throws IOException;

    /**
     * Finish unmarshalling from a stream.  Any transient class or instance cache is discarded.
     *
     * @throws IOException if an error occurs
     */
    void finish() throws IOException;
}
