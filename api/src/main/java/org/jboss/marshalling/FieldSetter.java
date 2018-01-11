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

import static java.security.AccessController.doPrivileged;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jboss.marshalling._private.GetUnsafeAction;
import sun.misc.Unsafe;

/**
 * A setter for a (possibly final) field, which allows for correct object initialization of {@link java.io.Serializable} objects
 * with {@code readObject()} methods, even in the presence of {@code final} fields.
 */
public final class FieldSetter {
    static final Unsafe unsafe = doPrivileged(GetUnsafeAction.INSTANCE);

    private final Class<?> clazz;
    private final Class<?> fieldType;
    private final long fieldOffset;

    private FieldSetter(final Field field) {
        this.clazz = field.getDeclaringClass();
        this.fieldType = field.getType();
        fieldOffset = unsafe.objectFieldOffset(field);
    }

    /**
     * Set the value of the field to the given object.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void set(Object instance, Object value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (! fieldType.isInstance(value)) {
            throw incorrectValueType();
        }
        unsafe.putObject(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setBoolean(Object instance, boolean value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != boolean.class) {
            throw incorrectValueType();
        }
        unsafe.putBoolean(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setByte(Object instance, byte value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != byte.class) {
            throw incorrectValueType();
        }
        unsafe.putByte(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setChar(Object instance, char value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != char.class) {
            throw incorrectValueType();
        }
        unsafe.putChar(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setDouble(Object instance, double value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != double.class) {
            throw incorrectValueType();
        }
        unsafe.putDouble(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setFloat(Object instance, float value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != float.class) {
            throw incorrectValueType();
        }
        unsafe.putFloat(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setInt(Object instance, int value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != int.class) {
            throw incorrectValueType();
        }
        unsafe.putInt(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setLong(Object instance, long value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != long.class) {
            throw incorrectValueType();
        }
        unsafe.putLong(instance, fieldOffset, value);
    }

    /**
     * Set the value of the field to the given value.
     *
     * @param instance the instance to set
     * @param value the new value
     * @throws IllegalArgumentException if the given instance is {@code null} or not of the correct class
     */
    public void setShort(Object instance, short value) throws IllegalArgumentException {
        if (! clazz.isInstance(instance)) {
            throw incorrectType();
        }
        if (fieldType != short.class) {
            throw incorrectValueType();
        }
        unsafe.putShort(instance, fieldOffset, value);
    }

    private static IllegalArgumentException incorrectType() {
        return new IllegalArgumentException("Instance is not of the correct type");
    }

    private static IllegalArgumentException incorrectValueType() {
        return new IllegalArgumentException("Value is not of the correct type");
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
        final Class<?> caller = JDKSpecific.getMyCaller();
        if (caller != clazz) {
            throw new SecurityException("Cannot get field from someone else's class");
        }
        final Field field;
        try {
            field = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("No such field '" + name + "'", e);
        }
        final int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            throw new SecurityException("Cannot get access to static field '" + name + "'");
        }
        return new FieldSetter(field);
    }
}
