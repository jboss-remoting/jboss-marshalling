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

package org.jboss.marshalling.reflect;

import java.io.SerializablePermission;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import static org.jboss.marshalling.reflect.ConcurrentReferenceHashMap.ReferenceType.WEAK;
import static org.jboss.marshalling.reflect.ConcurrentReferenceHashMap.ReferenceType.STRONG;
import static org.jboss.marshalling.reflect.ConcurrentReferenceHashMap.Option.IDENTITY_COMPARISONS;

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

    static SerializableClassRegistry getInstanceUnchecked() {
        return INSTANCE;
    }

    private final ConcurrentReferenceHashMap<Class<?>, SerializableClass> cache = new ConcurrentReferenceHashMap<Class<?>, SerializableClass>(512, 0x0.Cp0f, 16, WEAK, STRONG, EnumSet.of(IDENTITY_COMPARISONS));

    /**
     * Look up serialization information for a class.  The resultant object will be cached.
     *
     * @param subject the class to look up
     * @return the serializable class information
     */
    public SerializableClass lookup(final Class<?> subject) {
        SerializableClass info = cache.get(subject);
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
        final SerializableClass old = cache.putIfAbsent(subject, info);
        return old != null ? old : info;
    }
}
