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
    public static FieldSetter get(final Class<?> clazz, final String name) throws SecurityException, IllegalArgumentException {
        final Class[] stackTrace = Holder.STACK_TRACE_READER.getClassContext();
        if (stackTrace[2] != clazz) {
            throw new SecurityException("Cannot get accessible field from someone else's class");
        }
        return new FieldSetter(AccessController.doPrivileged(new GetFieldAction(clazz, name)));
    }

    private static final class Holder {

        static final StackTraceReader STACK_TRACE_READER;

        static {
            STACK_TRACE_READER = AccessController.doPrivileged(new PrivilegedAction<StackTraceReader>() {
                public StackTraceReader run() {
                    return new StackTraceReader();
                }
            });
        }

        private Holder() {
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
