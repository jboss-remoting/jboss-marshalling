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
     * @apiviz.exclude
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
