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
import java.lang.reflect.Proxy;

/**
 * A class cloner which uses the given class loader to resolve classes.
 */
public class ClassLoaderClassCloner implements ClassCloner {

    private final ClassLoader destClassLoader;

    /**
     * Construct a new instance.
     *
     * @param destClassLoader the class loader to use
     */
    public ClassLoaderClassCloner(final ClassLoader destClassLoader) {
        this.destClassLoader = destClassLoader;
    }

    /** {@inheritDoc} */
    public Class<?> clone(final Class<?> original) throws IOException, ClassNotFoundException {
        if (original.isPrimitive()) {
            return original;
        }
        final String name = original.getName();
        if (name.startsWith("java.")) {
            return original;
        } else if (original.getClassLoader() == destClassLoader) {
            return original;
        } else {
            return Class.forName(name, true, destClassLoader);
        }
    }

    /** {@inheritDoc} */
    public Class<?> cloneProxy(final Class<?> proxyClass) throws IOException, ClassNotFoundException {
        final Class<?>[] origInterfaces = proxyClass.getInterfaces();
        final Class<?>[] interfaces = new Class[origInterfaces.length];
        for (int i = 0, origInterfacesLength = origInterfaces.length; i < origInterfacesLength; i++) {
            interfaces[i] = clone(origInterfaces[i]);
        }
        return Proxy.getProxyClass(destClassLoader, interfaces);
    }
}
