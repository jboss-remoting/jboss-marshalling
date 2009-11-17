/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A setter for a (possibly final) field, which allows for correct object initialization of {@link java.io.Serializable} objects
 * with {@code readObject()} methods, even in the presence of {@code final} fields.
 */
public final class FieldSetter {
    private final Field field;

    private FieldSetter(final Field field) {
        this.field = field;
    }

    /**
     * Set the value of the field to the given object.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void set(Object instance, Object value) throws IllegalArgumentException {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setBoolean(Object instance, boolean value) throws IllegalArgumentException {
        try {
            field.setBoolean(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setByte(Object instance, byte value) throws IllegalArgumentException {
        try {
            field.setByte(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setChar(Object instance, char value) throws IllegalArgumentException {
        try {
            field.setChar(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setDouble(Object instance, double value) throws IllegalArgumentException {
        try {
            field.setDouble(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setFloat(Object instance, float value) throws IllegalArgumentException {
        try {
            field.setFloat(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setInt(Object instance, int value) throws IllegalArgumentException {
        try {
            field.setInt(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setLong(Object instance, long value) throws IllegalArgumentException {
        try {
            field.setLong(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setShort(Object instance, short value) throws IllegalArgumentException {
        try {
            field.setShort(instance, value);
        } catch (IllegalAccessException e) {
            throw illegalState(e);
        }
    }

    private IllegalStateException illegalState(final IllegalAccessException e) {
        return new IllegalStateException("Unexpected illegal access of accessible field", e);
    }

    /**
     * Get an instance for the current class.
     *
     * @param clazz the class containing the field
     * @param name the name of the field
     * @return the {@code Field} instance
     * @throws SecurityException if the field does not belong to the caller's class, or the field is static
     * @throws IllegalArgumentException if there is no field of the given name on the given class
     */
    public static <T> FieldSetter get(final Class<T> clazz, final String name) throws SecurityException, IllegalArgumentException {
        final Class[] stackTrace = getInstance.STACK_TRACE_READER.getClassContext();
        if (stackTrace[2] != clazz) {
            throw new SecurityException("Cannot get accessible field from someone else's class");
        }
        return new FieldSetter(AccessController.doPrivileged(new GetFieldAction(clazz, name)));
    }

    private static final class getInstance {

        static final StackTraceReader STACK_TRACE_READER;

        static {
            STACK_TRACE_READER = AccessController.doPrivileged(new PrivilegedAction<StackTraceReader>() {
                public StackTraceReader run() {
                    return new StackTraceReader();
                }
            });
        }

        private getInstance() {
        }

        static final class StackTraceReader extends SecurityManager {
            protected Class[] getClassContext() {
                return super.getClassContext();
            }
        }
    }

    private static class GetFieldAction implements PrivilegedAction<Field> {

        private final Class<?> clazz;
        private final String name;

        private GetFieldAction(final Class<?> clazz, final String name) {
            this.clazz = clazz;
            this.name = name;
        }

        public Field run() {
            try {
                final Field field = clazz.getDeclaredField(name);
                final int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    throw new SecurityException("Cannot get access to static field '" + name + "'");
                }
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("No such field '" + name + "'", e);
            }
        }
    }
}
