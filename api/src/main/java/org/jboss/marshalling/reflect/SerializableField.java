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

import java.lang.reflect.Field;
import org.jboss.marshalling.util.Kind;

/**
 * Reflection information about a field on a serializable class.
 */
public final class SerializableField {
    // the type of the field itself
    private final Class<?> type;
    private final Field field;
    private final String name;
    private final boolean unshared;
    private final Kind kind;

    SerializableField(Class<?> type, String name, boolean unshared, final Field field) {
        this.type = type;
        this.name = name;
        this.unshared = unshared;
        this.field = field;
        if (field != null) {
            // verify field information
            if (field.getType() != type) {
                throw new IllegalStateException("Constructed a serializable field with the wrong type (field type is " + field.getType() + ", our type is " + type + ")");
            }
            if (! field.getName().equals(name)) {
                throw new IllegalStateException("Constructed a serializable field with the wrong name (field name is " + field.getName() + ", our type is " + name + ")");
            }
        }
        // todo - see if a small Map is faster
        if (type == boolean.class) {
            kind = Kind.BOOLEAN;
        } else if (type == byte.class) {
            kind = Kind.BYTE;
        } else if (type == short.class) {
            kind = Kind.SHORT;
        } else if (type == int.class) {
            kind = Kind.INT;
        } else if (type == long.class) {
            kind = Kind.LONG;
        } else if (type == char.class) {
            kind = Kind.CHAR;
        } else if (type == float.class) {
            kind = Kind.FLOAT;
        } else if (type == double.class) {
            kind = Kind.DOUBLE;
        } else {
            kind = Kind.OBJECT;
        }
    }

    /**
     * Get the reflection {@code Field} for this serializable field.  The resultant field will be accessible.
     *
     * @return the reflection field
     */
    public Field getField() {
        return field;
    }

    /**
     * Get the name of the field.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Determine whether this field is marked as "unshared".
     *
     * @return {@code true} if the field is unshared
     */
    public boolean isUnshared() {
        return unshared;
    }

    /**
     * Get the kind of field.
     *
     * @return the kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Get the field type.
     *
     * @return the field type
     */
    public Class<?> getType() throws ClassNotFoundException {
        return type;
    }
}
