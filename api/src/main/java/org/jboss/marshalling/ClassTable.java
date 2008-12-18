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
 * A lookup mechanism for predefined classes.  Some marshallers can use this to
 * avoid sending lengthy class descriptor information.
 */
public interface ClassTable {
    /**
     * Determine whether the given class reference is a valid predefined reference.
     *
     * @param clazz the candidate class
     * @return the class writer, or {@code null} to use the default mechanism
     * @throws IOException if an I/O error occurs
     */
    Writer getClassWriter(Class<?> clazz) throws IOException;

    /**
     * Read a class from the stream.  The class will have been written by the
     * {@link #getClassWriter(Class)} method's {@code Writer} instance, as defined above.
     *
     * @param unmarshaller the unmarshaller to read from
     * @return the class
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class could not be found
     */
    Class<?> readClass(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException;

    /**
     * The class writer for a specific class.
     */
    interface Writer {
        /**
         * Write the predefined class reference to the stream.
         *
         * @param marshaller the marshaller to write to
         * @param clazz the class reference to write
         * @throws IOException if an I/O error occurs
         */
        void writeClass(Marshaller marshaller, Class<?> clazz) throws IOException;
    }
}
