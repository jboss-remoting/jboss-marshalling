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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Permission;
import java.io.InvalidClassException;
import java.io.SerializablePermission;
import org.jboss.marshalling.Creator;

/**
 * A creator that simply uses reflection to locate and invoke a zero-argument constructor.
 */
public class ReflectiveCreator implements Creator {

    private static final Permission CREATOR_PERM = new SerializablePermission("creator");

    public ReflectiveCreator() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CREATOR_PERM);
        }
    }

    /**
     * Get the constructor to use for a class.  Returns {@code null} if no suitable constructor is available.
     *
     * @param clazz the class to get a constructor for
     * @return the constructor, or {@code null} if none is available
     */
    protected <T> Constructor<T> getNewConstructor(final Class<T> clazz) {
        final Constructor<T> newConstructor = AccessController.doPrivileged(new PrivilegedAction<Constructor<T>>() {
            public Constructor<T> run() {
                try {
                    final Constructor<T> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    return constructor;
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        });
        return newConstructor;
    }

    /** {@inheritDoc} */
    public <T> T create(final Class<T> clazz) throws InvalidClassException {
        final Constructor<T> newConstructor = getNewConstructor(clazz);
        if (newConstructor == null) {
            throw new InvalidClassException(clazz.getName(), "No suitable constructor could be found");
        }
        final Constructor<T> constructor = newConstructor;
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException e) {
            final InvalidClassException ice = new InvalidClassException(clazz.getName(), "Constructor threw an exception");
            ice.initCause(e);
            throw ice;
        } catch (IllegalAccessException e) {
            throw new InvalidClassException(clazz.getName(), "Illegal access exception occurred accessing the constructor: " + String.valueOf(e));
        } catch (InstantiationException e) {
            throw new InvalidClassException(clazz.getName(), "Instantiation exception: " + String.valueOf(e));
        }
    }
}
