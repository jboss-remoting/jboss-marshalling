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

package org.jboss.marshalling.reflect;

import java.io.SerializablePermission;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry for reflection information usable by serialization implementations.  Objects returned from this registry
 * can be used to invoke private methods without security checks, so it is important to be careful not to "leak" instances
 * out of secured implementations.
 */
public final class SerializableClassRegistry {
    private SerializableClassRegistry() {
    }

    private static final SerializableClassRegistry INSTANCE = new SerializableClassRegistry();

    private static final SerializablePermission PERMISSION = new SerializablePermission("allowSerializationReflection");

    /**
     * Get the serializable class registry instance, if allowed by the current security manager.  The caller must have
     * the {@code java.io.SerializablePermission} {@code "allowSerializationReflection"} in order to invoke this method.
     *
     * @return the registry
     * @throws SecurityException if the caller does not have sufficient privileges
     */
    public static SerializableClassRegistry getInstance() throws SecurityException {
        SecurityManager manager = System.getSecurityManager();
        if (manager != null) {
            manager.checkPermission(PERMISSION);
        }
        return INSTANCE;
    }

    private final ConcurrentMap<ClassLoader, ConcurrentMap<Class<?>, SerializableClass>> registry = new UnlockedHashMap<ClassLoader, ConcurrentMap<Class<?>, SerializableClass>>();

    static SerializableClassRegistry getInstanceUnchecked() {
        return INSTANCE;
    }

    /**
     * Look up serialization information for a class.  The resultant object will be cached.
     *
     * @param subject the class to look up
     * @return the serializable class information
     */
    public SerializableClass lookup(final Class<?> subject) {
        if (subject == null) {
            return null;
        }
        final ClassLoader classLoader = subject.getClassLoader();
        ConcurrentMap<Class<?>, SerializableClass> loaderMap = registry.get(classLoader);
        if (loaderMap == null) {
            final ConcurrentMap<Class<?>, SerializableClass> existing = registry.putIfAbsent(classLoader, loaderMap = new UnlockedHashMap<Class<?>, SerializableClass>());
            if (existing != null) {
                loaderMap = existing;
            }
        }
        SerializableClass info = loaderMap.get(subject);
        if (info != null) {
            return info;
        }
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            info = AccessController.doPrivileged(new PrivilegedAction<SerializableClass>() {
                public SerializableClass run() {
                    return new SerializableClass(subject);
                }
            });
        } else {
            info = new SerializableClass(subject);
        }
        final SerializableClass existing = loaderMap.putIfAbsent(subject, info);
        return existing != null ? existing : info;
    }

    /**
     * Release all reflection information belonging to the given class loader.
     *
     * @param classLoader the class loader to release
     */
    public void release(ClassLoader classLoader) {
        registry.remove(classLoader);
    }

    static final class DuhMap<K, V> extends IdentityHashMap<K, V> implements ConcurrentMap<K, V> {

        public V putIfAbsent(final K key, final V value) {
            return containsKey(key) ? get(key) : put(key, value);
        }

        public boolean remove(final Object key, final Object value) {
            return get(key) == value ? remove(key) != this : false;
        }

        public boolean replace(final K key, final V oldValue, final V newValue) {
            return get(key) == oldValue ? put(key, newValue) != this : false;
        }

        public V replace(final K key, final V value) {
            return containsKey(key) ? put(key, value) : null;
        }
    }
}
