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

package org.jboss.marshalling.cloner;

import java.io.IOException;

/**
 * A cloner for class types.  Used (for example) to load an equivalent class from an alternate classloader.
 */
public interface ClassCloner {

    /**
     * Clone the given class.
     *
     * @param original the class to clone
     * @return the cloned class
     * @throws IOException if cloning fails due to a serialization problem
     * @throws ClassNotFoundException if cloning fails due to an unavailable class
     */
    Class<?> clone(Class<?> original) throws IOException, ClassNotFoundException;

    /**
     * Clone the given reflection proxy class.
     *
     * @param proxyClass the proxy class to clone
     * @return the cloned proxy class
     * @throws IOException if cloning fails due to a serialization problem
     * @throws ClassNotFoundException if cloning fails due to an unavailable class
     */
    Class<?> cloneProxy(Class<?> proxyClass) throws IOException, ClassNotFoundException;

    /**
     * A class cloner which just returns the class it is given.  This cloner can be used in cases where an object
     * must be deep-cloned within the same class loader.
     */
    ClassCloner IDENTITY = new ClassCloner() {
        public Class<?> clone(final Class<?> original) {
            return original;
        }

        public Class<?> cloneProxy(final Class<?> proxyClass) {
            return proxyClass;
        }
    };
}
