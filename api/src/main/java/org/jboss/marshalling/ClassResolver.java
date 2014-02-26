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
 * A class annotator and resolver.  Instances of this interface have the opportunity
 * to append information (such as classloader information, or class bytes) to a written
 * class descriptor.  This information can then be used on unmarshalling to aid in the
 * selection (or creation) of the proper {@code Class} based on the class descriptor and
 * the annotation data.
 */
public interface ClassResolver {
    /**
     * Add optional information about a class to a stream.  The class descriptor will
     * already have been written.
     *
     * @see java.io.ObjectOutputStream#annotateClass(Class)
     *
     * @param marshaller the marshaller to write to
     * @param clazz the class that was written
     * @throws IOException if an error occurs
     */
    void annotateClass(Marshaller marshaller, Class<?> clazz) throws IOException;

    /**
     * Add optional information about a proxy class to a stream.  The class descriptor will
     * already have been written.
     *
     * @see java.io.ObjectOutputStream#annotateProxyClass(Class)
     *
     * @param marshaller the marshaller to write to
     * @param proxyClass the class that was written
     * @throws IOException if an error occurs
     */
    void annotateProxyClass(Marshaller marshaller, Class<?> proxyClass) throws IOException;

    /**
     * Get the class name to write for a given class.  The class name will be written as part of the
     * class descriptor.
     *
     * @param clazz the class
     * @return the class name
     * @throws IOException if an error occurs
     */
    String getClassName(Class<?> clazz) throws IOException;

    /**
     * Get the interface names to write for a given proxy class.  The interface names will be written as part
     * of the class descriptor.
     *
     * @param proxyClass the proxy class
     * @return the proxy class interface names
     * @throws IOException if an error occurs
     */
    String[] getProxyInterfaces(Class<?> proxyClass) throws IOException;

    /**
     * Load the local class for a class descriptor.  The class descriptor has already been read,
     * but any data written by {@link #annotateClass(Marshaller, Class)} should be read by this method.
     *
     * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
     *
     * @param unmarshaller the unmarshaller from which to read annotation data, if any
     * @param name the class name
     * @param serialVersionUID the serial version UID
     * @return the corresponding class
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class could not be loaded
     */
    Class<?> resolveClass(Unmarshaller unmarshaller, String name, long serialVersionUID) throws IOException, ClassNotFoundException;

    /**
     * Load a proxy class that implements the given interfaces.
     *
     * @see java.io.ObjectInputStream#resolveProxyClass(String[])
     *
     * @param unmarshaller the unmarshaller from which to read annotation data, if any
     * @param interfaces the class descriptor
     * @return the proxy class
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the proxy class could not be loaded
     */
    Class<?> resolveProxyClass(Unmarshaller unmarshaller, String[] interfaces) throws IOException, ClassNotFoundException;
}
