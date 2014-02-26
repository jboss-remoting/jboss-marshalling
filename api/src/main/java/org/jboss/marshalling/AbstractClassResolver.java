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
import java.io.StreamCorruptedException;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.HashMap;
import org.jboss.marshalling.reflect.SerializableClassRegistry;

/**
 * A base implementation of {@code ClassResolver} which simply resolves the class
 * against a classloader which is specified by the subclass implementation.
 */
public abstract class AbstractClassResolver implements ClassResolver {

    /**
     * Specifies whether an exception should be thrown on an incorrect serialVersionUID.
     */
    protected final boolean enforceSerialVersionUid;

    private static final SerializableClassRegistry registry;

    static {
        registry = AccessController.doPrivileged(new PrivilegedAction<SerializableClassRegistry>() {
            public SerializableClassRegistry run() {
                return SerializableClassRegistry.getInstance();
            }
        });
    }

    /**
     * Construct a new instance.
     */
    protected AbstractClassResolver() {
        this(false);
    }

    /**
     * Construct a new instance.
     *
     * @param enforceSerialVersionUid {@code true} if an exception should be thrown on an incorrect serialVersionUID
     */
    protected AbstractClassResolver(final boolean enforceSerialVersionUid) {
        this.enforceSerialVersionUid = enforceSerialVersionUid;
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

    /** {@inheritDoc}  The base implementation returns the name of the class. */
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
        final Class<?> clazz = loadClass(name);
        if (enforceSerialVersionUid) {
            final long uid = registry.lookup(clazz).getEffectiveSerialVersionUID();
            if (uid != serialVersionUID) {
                throw new StreamCorruptedException("serialVersionUID does not match!");
            }
        }
        return clazz;
    }

    /**
     * Load a class with the given name.  The base implementation uses the classloader returned from {@link #getClassLoader()}.
     *
     * @param name the name of the class
     * @return the class
     * @throws ClassNotFoundException if the class is not found, or if there is no classloader
     */
    protected Class<?> loadClass(final String name) throws ClassNotFoundException {
        final Class<?> prim = primitives.get(name);
        return prim != null ? prim : Class.forName(name, false, getClassLoaderChecked());
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

    private static final Map<String, Class<?>> primitives;

    static {
        final Map<String, Class<?>> map = new HashMap<String, Class<?>>();
        map.put("void", void.class);

        map.put("byte", byte.class);
        map.put("short", short.class);
        map.put("int", int.class);
        map.put("long", long.class);

        map.put("char", char.class);

        map.put("boolean", boolean.class);

        map.put("float", float.class);
        map.put("double", double.class);
        primitives = map;
    }
}
