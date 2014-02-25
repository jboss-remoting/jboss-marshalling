/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableField;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class FutureSerializableClassDescriptor extends SerializableClassDescriptor {
    private final Class<?> type;
    private final int typeID;

    private SerializableClassDescriptor result;

    public FutureSerializableClassDescriptor(final Class<?> type, final int typeID) {
        this.type = type;
        this.typeID = typeID;
    }

    public Class<?> getType() {
        return type;
    }

    public int getTypeID() {
        return typeID;
    }

    public ClassDescriptor getSuperClassDescriptor() {
        return check(result).getSuperClassDescriptor();
    }

    public SerializableField[] getFields() {
        return check(result).getFields();
    }

    public SerializableClass getSerializableClass() {
        return check(result).getSerializableClass();
    }

    public void setResult(final SerializableClassDescriptor result) {
        this.result = result;
    }

    private SerializableClassDescriptor check(final SerializableClassDescriptor result) {
        if (result == null) {
            throw new IllegalStateException("Serializable class not resolved");
        }
        return result;
    }

    public String toString() {
        return "future " + result;
    }
}
