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

/**
 * An object creator that uses methods only found in certain JVMs to create a new constructor if needed.
 *
 * @deprecated This creator is no longer used and will be removed in 1.5.
 */
@Deprecated
public class SunReflectiveCreator extends ReflectiveCreator {

    private static SerializableClassRegistry registry = SerializableClassRegistry.getInstanceUnchecked();

    /**
     * {@inheritDoc}  This implementation will attempt to create a new constructor if one is not available.
     */
    protected <T> Constructor<T> getNewConstructor(final Class<T> clazz) {
        return registry.lookup(clazz).getNoInitConstructor();
    }
}
