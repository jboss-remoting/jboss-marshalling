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

package org.jboss.marshalling.river;

import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.reflect.SerializableClass;

/**
 *
 */
public abstract class SerializableClassDescriptor extends ClassDescriptor {

    protected SerializableClassDescriptor() {}

    public abstract Class<?> getType();

    public abstract int getTypeID();

    public abstract ClassDescriptor getSuperClassDescriptor();

    public abstract SerializableField[] getFields();

    public abstract SerializableClass getSerializableClass();

    public String toString() {
        final ClassDescriptor superClassDescriptor = getSuperClassDescriptor();
        if (superClassDescriptor == null) {
            return String.format("%s for %s (id %x02)", getClass().getSimpleName(), getType(), Integer.valueOf(getTypeID()));
        } else {
            return String.format("%s for %s (id %x02) extends %s", getClass().getSimpleName(), getType(), Integer.valueOf(getTypeID()), superClassDescriptor);
        }
    }

    public Class<?> getNonSerializableSuperclass() {
        final ClassDescriptor descriptor = getSuperClassDescriptor();
        if (descriptor instanceof SerializableClassDescriptor) {
            return ((SerializableClassDescriptor) descriptor).getNonSerializableSuperclass();
        } else {
            return descriptor.getType();
        }
    }
}
