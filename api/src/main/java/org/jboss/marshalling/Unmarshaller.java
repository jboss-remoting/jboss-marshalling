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
