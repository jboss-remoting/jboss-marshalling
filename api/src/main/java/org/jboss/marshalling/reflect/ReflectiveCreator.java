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
