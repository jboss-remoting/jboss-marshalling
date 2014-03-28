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

    private static final ClassValue<SerializableClass> classValue = new ClassValue<SerializableClass>() {
        protected SerializableClass computeValue(final Class<?> type) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                return AccessController.doPrivileged(new PrivilegedAction<SerializableClass>() {
                    public SerializableClass run() {
                        return new SerializableClass(type);
                    }
                });
            } else {
                return new SerializableClass(type);
            }
        }
    };

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
        return classValue.get(subject);
    }

    /**
     * Release all reflection information belonging to the given class loader.
     *
     * @param classLoader the class loader to release
     * @deprecated No longer required.
     */
    @SuppressWarnings("unused")
    public void release(ClassLoader classLoader) {
        // no-op
    }
}
