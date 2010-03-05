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
import java.lang.reflect.Proxy;

/**
 * A class cloner which uses the given class loader to resolve classes.
 */
public class ClassLoaderClassCloner implements ClassCloner {

    private final ClassLoader destClassLoader;

    public ClassLoaderClassCloner(final ClassLoader destClassLoader) {
        this.destClassLoader = destClassLoader;
    }

    public Class<?> clone(final Class<?> original) throws IOException, ClassNotFoundException {
        final String name = original.getName();
        if (name.startsWith("java.")) {
            return original;
        } else if (original.getClassLoader() == destClassLoader) {
            return original;
        } else {
            return Class.forName(name, true, destClassLoader);
        }
    }

    public Class<?> cloneProxy(final Class<?> proxyClass) throws IOException, ClassNotFoundException {
        final Class<?>[] origInterfaces = proxyClass.getInterfaces();
        final Class<?>[] interfaces = new Class[origInterfaces.length];
        for (int i = 0, origInterfacesLength = origInterfaces.length; i < origInterfacesLength; i++) {
            interfaces[i] = clone(origInterfaces[i]);
        }
        return Proxy.getProxyClass(destClassLoader, interfaces);
    }
}
