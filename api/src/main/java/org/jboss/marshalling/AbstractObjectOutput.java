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
     * @throws java.io.IOException if an I/O error occurs
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
