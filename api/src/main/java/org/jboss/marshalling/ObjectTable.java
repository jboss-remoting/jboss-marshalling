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

import java.io.IOException;

/**
 * A lookup mechanism for predefined object references.  Some marshallers can use this to
 * correlate to known object instances.
 */
public interface ObjectTable {
    /**
     * Determine whether the given object reference is a valid predefined reference.
     *
     * @param object the candidate object
     * @return the object writer, or {@code null} to use the default mechanism
     * @throws IOException if an I/O error occurs
     */
    Writer getObjectWriter(Object object) throws IOException;

    /**
     * Read an instance from the stream.  The instance will have been written by the
     * {@link #getObjectWriter(Object)} method's {@code Writer} instance, as defined above.
     *
     * @param unmarshaller the unmarshaller to read from
     * @return the object instance
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class could not be found
     */
    Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException;

    /**
     * The object writer for a specific object.
     */
    interface Writer {
        /**
         * Write the predefined object reference to the stream.
         *
         * @param marshaller the marshaller to write to
         * @param object the object reference to write
         * @throws IOException if an I/O error occurs
         */
        void writeObject(Marshaller marshaller, Object object) throws IOException;
    }
}
