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

import java.io.ObjectOutput;
import java.io.IOException;

/**
 * An object marshaller for writing objects to byte streams.
 */
public interface Marshaller extends ObjectOutput, ByteOutput {
    /**
     * Write an object to the underlying storage or stream as a new instance. The class that implements this interface
     * defines how the object is written.
     *
     * @param obj the object to be written
     * @throws IOException if an error occurs
     */
    void writeObjectUnshared(Object obj) throws IOException;

    /**
     * Begin marshalling to a stream.
     *
     * @param newOutput the new stream
     * @throws IOException if an error occurs during setup, such as an error writing the header
     */
    void start(ByteOutput newOutput) throws IOException;

    /**
     * Discard the instance cache.  May also discard the class cache in implementations that do not support separated
     * class and instance caches.
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
     * Finish marshalling to a stream.  Any transient class or instance cache is discarded.  The stream is released.
     * No further marshalling may be done until the {@link #start(ByteOutput)} method is again invoked.
     *
     * @throws IOException if an error occurs
     */
    void finish() throws IOException;
}
