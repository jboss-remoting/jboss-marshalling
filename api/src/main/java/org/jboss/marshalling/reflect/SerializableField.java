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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.jboss.marshalling.util.Kind;

/**
 * Reflection information about a field on a serializable class.
 */
public final class SerializableField {
    // the class that the field is attached to
    private final WeakReference<Class<?>> classRef;
    // the type of the field itself
    private final WeakReference<Class<?>> typeRef;
    private final AtomicReference<WeakReference<Field>> fieldRefRef = new AtomicReference<WeakReference<Field>>();
    private final String name;
    private final boolean unshared;
    private final Kind kind;
    private volatile boolean missing;

    SerializableField(Class<?> clazz, Class<?> type, String name, boolean unshared) {
        classRef = new WeakReference<Class<?>>(clazz);
        typeRef = new WeakReference<Class<?>>(type);
        this.name = name;
        this.unshared = unshared;
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

    private Field lookupField() {
        if (missing) {
            return null;
        }
        final Class<?> clazz = classRef.get();
        if (clazz != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Field>() {
                public Field run() {
                    try {
                        return clazz.getDeclaredField(name);
                    } catch (NoSuchFieldException e) {
                        missing = true;
                        return null;
                    }
                }
            });
        } else {
            throw new IllegalStateException("Class unloaded");
        }
    }

    /**
     * Get the reflection {@code Field} for this serializable field.  The resultant field will be accessible.
     *
     * @return the reflection field
     */
    public Field getField() {
        final WeakReference<Field> fieldRef = fieldRefRef.get();
        if (fieldRef == null) {
            final Field field = lookupField();
            if (field != null) {
                fieldRefRef.compareAndSet(null, new WeakReference<Field>(field));
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        field.setAccessible(true);
                        return null;
                    }
                });
            }
            return field;
        } else {
            final Field field = fieldRef.get();
            if (field != null) {
                return field;
            }
            return AccessController.doPrivileged(new PrivilegedAction<Field>() {
                public Field run() {
                    final Field newField = lookupField();
                    final WeakReference<Field> newFieldRef;
                    if (newField == null) {
                        newFieldRef = null;
                    } else {
                        newField.setAccessible(true);
                        newFieldRef = new WeakReference<Field>(newField);
                    }
                    fieldRefRef.compareAndSet(fieldRef, newFieldRef);
                    return newField;
                }
            });
        }
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
        return SerializableClass.dereference(typeRef);
    }
}
