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
import java.lang.reflect.Proxy;

/**
 * A base implementation of {@code ClassResolver} which simply resolves the class
 * against a classloader which is specified by the subclass implementation.
 */
public abstract class AbstractClassResolver implements ClassResolver {
    /**
     * Construct a new instance.
     */
    protected AbstractClassResolver() {
    }

    /**
     * Get the classloader to use to resolve classes for this resolver.
     *
     * @return the classloader
     */
    protected abstract ClassLoader getClassLoader();

    /** {@inheritDoc}  The base implementation takes no action. */
    public void annotateClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
        // no operation
    }

    /** {@inheritDoc}  The base implementation takes no action. */
    public void annotateProxyClass(final Marshaller marshaller, final Class<?> proxyClass) throws IOException {
        // no operation
    }

    private ClassLoader getClassLoaderChecked() throws ClassNotFoundException {
        final ClassLoader loader = getClassLoader();
        if (loader == null) {
            throw new ClassNotFoundException("No classloader available");
        }
        return loader;
    }

    /** {@inheritDoc}  The base implemenation returns the name of the class. */
    public String getClassName(final Class<?> clazz) throws IOException {
        return clazz.getName();
    }

    /** {@inheritDoc}  The base implementation returns the name of each interface (via {@link #getClassName(Class) getClassName()} implemented by the given class. */
    public String[] getProxyInterfaces(final Class<?> proxyClass) throws IOException {
        final Class<?>[] interfaces = proxyClass.getInterfaces();
        final String[] names = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            names[i] = getClassName(interfaces[i]);
        }
        return names;
    }

    /**
     * {@inheritDoc}  The base implementation uses the class loader returned from {@code getClassLoader()} and
     * loads the class by name.
     */
    public Class<?> resolveClass(final Unmarshaller unmarshaller, final String name, final long serialVersionUID) throws IOException, ClassNotFoundException {
        return loadClass(name);
    }

    /**
     * Load a class with the given name.  The base implementation uses the classloader returned from {@link #getClassLoader()}.
     *
     * @param name the name of the class
     * @return the class
     * @throws ClassNotFoundException if the class is not found, or if there is no classloader
     */
    protected Class<?> loadClass(final String name) throws ClassNotFoundException {
        return Class.forName(name, false, getClassLoaderChecked());
    }

    /**
     * {@inheritDoc}  The base implementation uses the class loader returned from {@code getClassLoader()} and loads
     * each interface by name, returning a proxy class from that class loader.
     */
    public Class<?> resolveProxyClass(final Unmarshaller unmarshaller, final String[] interfaces) throws IOException, ClassNotFoundException {
        final ClassLoader classLoader = getClassLoaderChecked();
        final int length = interfaces.length;
        Class<?>[] classes = new Class<?>[length];
        for (int i = 0; i < length; i ++) {
            classes[i] = loadClass(interfaces[i]);
        }
        return Proxy.getProxyClass(classLoader, classes);
    }
}
