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
import java.util.EnumSet;

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

    private final ConcurrentReferenceHashMap<Class<?>, SerializableClass> cache = new ConcurrentReferenceHashMap<Class<?>, SerializableClass>(512, 0.8f, 16, ConcurrentReferenceHashMap.ReferenceType.WEAK, ConcurrentReferenceHashMap.ReferenceType.STRONG, EnumSet.of(ConcurrentReferenceHashMap.Option.IDENTITY_COMPARISONS));

    /**
     * Look up serialization information for a class.  The resultant object will be cached.
     *
     * @param subject the class to look up
     * @return the serializable class informatio
     */
    public SerializableClass lookup(final Class<?> subject) {
        SerializableClass info = cache.get(subject);
        if (info != null) {
            return info;
        }
        info = AccessController.doPrivileged(new PrivilegedAction<SerializableClass>() {
            public SerializableClass run() {
                return new SerializableClass(subject);
            }
        });
        final SerializableClass old = cache.putIfAbsent(subject, info);
        return old != null ? old : info;
    }
}
