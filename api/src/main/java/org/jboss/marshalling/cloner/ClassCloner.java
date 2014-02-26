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
