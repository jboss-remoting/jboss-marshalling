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

import java.io.ObjectInput;
import java.io.IOException;
import java.io.ObjectOutput;

/**
 * An externalizer base class which handles object creation in a default fashion.
 */
public abstract class AbstractExternalizer implements Externalizer {
    private static final long serialVersionUID = -584504194617263431L;

    /**
     * Create an instance of a type using the provided creator.
     *
     * @param subjectType the type to create
     * @param input the object input
     * @param defaultCreator the creator
     * @return a new instance
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class could not be located
     */
    public Object createExternal(final Class<?> subjectType, final ObjectInput input, final Creator defaultCreator) throws IOException, ClassNotFoundException {
        return defaultCreator.create(subjectType);
    }

    /** {@inheritDoc}  This default implementation does nothing. */
    public void writeExternal(final Object subject, final ObjectOutput output) throws IOException {
    }

    /** {@inheritDoc}  This default implementation does nothing. */
    public void readExternal(final Object subject, final ObjectInput input) throws IOException, ClassNotFoundException {
    }
}
