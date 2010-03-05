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
import java.io.ObjectInput;

public abstract class AbstractObjectInput extends SimpleDataInput implements ObjectInput {

    protected AbstractObjectInput(int bufferSize) {
        super(bufferSize);
    }

    /** {@inheritDoc} */
    public final Object readObject() throws ClassNotFoundException, IOException {
        return doReadObject(false);
    }

    /** {@inheritDoc} */
    public final Object readObjectUnshared() throws ClassNotFoundException, IOException {
        return doReadObject(true);
    }

    /**
     * Implementation of the actual object-reading method.
     *
     * @param unshared {@code true} if the instance should be unshared, {@code false} if it is shared
     * @return the object to read
     * @throws ClassNotFoundException if the class for the object could not be loaded
     * @throws java.io.IOException if an I/O error occurs
     */
    protected abstract Object doReadObject(boolean unshared) throws ClassNotFoundException, IOException;
}
