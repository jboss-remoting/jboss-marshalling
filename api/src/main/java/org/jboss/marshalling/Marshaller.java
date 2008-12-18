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
