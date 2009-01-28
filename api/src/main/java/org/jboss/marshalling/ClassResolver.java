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
 * A class annotater and resolver.  Instances of this interface have the opportunity
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
     * @param proxyClass the chass that was written
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
