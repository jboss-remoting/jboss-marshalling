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

import static java.security.AccessController.doPrivileged;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jboss.marshalling._private.GetUnsafeAction;
import org.jboss.marshalling.util.Kind;
import sun.misc.Unsafe;

/**
 * Reflection information about a field on a serializable class.
 */
public final class SerializableField {
    static final Unsafe unsafe = doPrivileged(GetUnsafeAction.INSTANCE);

    // the type of the field itself
    private final Class<?> type;
    private final Field field;
    private final String name;
    private final boolean unshared;
    private final Kind kind;
    private final long fieldOffset;

    public SerializableField(Class<?> type, String name, boolean unshared) {
        this(type, name, unshared, null);
    }

    SerializableField(Class<?> type, String name, boolean unshared, final Field field) {
        assert field == null || (field.getModifiers() & Modifier.STATIC) == 0 && ! field.getDeclaringClass().isArray();
        this.type = type;
        this.name = name;
        this.unshared = unshared;
        this.field = field;
        fieldOffset = field == null ? -1 : unsafe.objectFieldOffset(field);
        if (field != null) {
            // verify field information
            if (field.getType() != type) {
                throw new IllegalStateException("Constructed a serializable field with the wrong type (field type is " + field.getType() + ", our type is " + type + ")");
            }
            if (! field.getName().equals(name)) {
                throw new IllegalStateException("Constructed a serializable field with the wrong name (field name is " + field.getName() + ", our name is " + name + ")");
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
     * @deprecated As of Java 9, accessible fields are generally disallowed; use the {@code #setXXX(Object,value)} methods instead.
     */
    @Deprecated
    public Field getField() {
        return field;
    }

    /**
     * Determine if this object may be used to get or set an object field value.
     *
     * @return {@code true} if this object may be used to get or set an object field value, {@code false} otherwise
     */
    public boolean isAccessible() {
        return field != null;
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

    /**
     * Set the boolean value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setBoolean(Object instance, boolean value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != boolean.class) {
            throw new ClassCastException();
        }
        unsafe.putBoolean(instance, fieldOffset, value);
    }

    /**
     * Set the char value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setChar(Object instance, char value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != char.class) {
            throw new ClassCastException();
        }
        unsafe.putChar(instance, fieldOffset, value);
    }

    /**
     * Set the byte value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setByte(Object instance, byte value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != byte.class) {
            throw new ClassCastException();
        }
        unsafe.putByte(instance, fieldOffset, value);
    }

    /**
     * Set the short value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setShort(Object instance, short value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != short.class) {
            throw new ClassCastException();
        }
        unsafe.putShort(instance, fieldOffset, value);
    }

    /**
     * Set the integer value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setInt(Object instance, int value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != int.class) {
            throw new ClassCastException();
        }
        unsafe.putInt(instance, fieldOffset, value);
    }

    /**
     * Set the long value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setLong(Object instance, long value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != long.class) {
            throw new ClassCastException();
        }
        unsafe.putLong(instance, fieldOffset, value);
    }

    /**
     * Set the float value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setFloat(Object instance, float value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != float.class) {
            throw new ClassCastException();
        }
        unsafe.putFloat(instance, fieldOffset, value);
    }

    /**
     * Set the double value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setDouble(Object instance, double value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != double.class) {
            throw new ClassCastException();
        }
        unsafe.putDouble(instance, fieldOffset, value);
    }

    /**
     * Set the object value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @param value the value to set
     * @throws ClassCastException if {@code instance} or the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public void setObject(Object instance, Object value) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        final Class<?> fieldType = field.getType();
        if (fieldType.isPrimitive()) {
            throw new ClassCastException();
        }
        fieldType.cast(value);
        unsafe.putObject(instance, fieldOffset, value);
    }

    /**
     * Get the boolean value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public boolean getBoolean(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != boolean.class) {
            throw new ClassCastException();
        }
        return unsafe.getBoolean(instance, fieldOffset);
    }

    /**
     * Get the char value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public char getChar(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != char.class) {
            throw new ClassCastException();
        }
        return unsafe.getChar(instance, fieldOffset);
    }

    /**
     * Get the byte value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public byte getByte(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != byte.class) {
            throw new ClassCastException();
        }
        return unsafe.getByte(instance, fieldOffset);
    }

    /**
     * Get the short value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public short getShort(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != short.class) {
            throw new ClassCastException();
        }
        return unsafe.getShort(instance, fieldOffset);
    }

    /**
     * Get the integer value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public int getInt(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != int.class) {
            throw new ClassCastException();
        }
        return unsafe.getInt(instance, fieldOffset);
    }

    /**
     * Get the long value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public long getLong(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != long.class) {
            throw new ClassCastException();
        }
        return unsafe.getLong(instance, fieldOffset);
    }

    /**
     * Get the float value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public float getFloat(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != float.class) {
            throw new ClassCastException();
        }
        return unsafe.getFloat(instance, fieldOffset);
    }

    /**
     * Get the double value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public double getDouble(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType() != double.class) {
            throw new ClassCastException();
        }
        return unsafe.getDouble(instance, fieldOffset);
    }

    /**
     * Get the object value of this field on the given object instance.
     *
     * @param instance the object instance (must not be {@code null}, must be of the correct type)
     * @return the value of the field
     * @throws ClassCastException if the field is not of the correct type
     * @throws IllegalArgumentException if this instance has no reflection field set on it
     */
    public Object getObject(Object instance) throws ClassCastException, IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("instance is null");
        }
        if (field == null) {
            throw new IllegalArgumentException();
        }
        field.getDeclaringClass().cast(instance);
        if (field.getType().isPrimitive()) {
            throw new ClassCastException();
        }
        return unsafe.getObject(instance, fieldOffset);
    }

    /**
     * Read the field value from the stream.
     *
     * @param instance the instance
     * @param input the source stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class could not be loaded
     */
    public void readFrom(Object instance, ObjectInput input) throws IOException, ClassNotFoundException {
        switch (kind) {
            case BOOLEAN:
                setBoolean(instance, input.readBoolean());
                break;
            case BYTE:
                setByte(instance, input.readByte());
                break;
            case CHAR:
                setChar(instance, input.readChar());
                break;
            case DOUBLE:
                setDouble(instance, input.readDouble());
                break;
            case FLOAT:
                setFloat(instance, input.readFloat());
                break;
            case INT:
                setInt(instance, input.readInt());
                break;
            case LONG:
                setLong(instance, input.readLong());
                break;
            case SHORT:
                setShort(instance, input.readShort());
                break;
            case OBJECT:
                setObject(instance, input.readObject());
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public void writeTo(Object instance, ObjectOutput output) throws IOException {
        switch (kind) {
            case BOOLEAN:
                output.writeBoolean(getBoolean(instance));
                break;
            case BYTE:
                output.writeByte(getByte(instance));
                break;
            case CHAR:
                output.writeChar(getChar(instance));
                break;
            case DOUBLE:
                output.writeDouble(getDouble(instance));
                break;
            case FLOAT:
                output.writeFloat(getFloat(instance));
                break;
            case INT:
                output.writeInt(getInt(instance));
                break;
            case LONG:
                output.writeLong(getLong(instance));
                break;
            case SHORT:
                output.writeShort(getShort(instance));
                break;
            case OBJECT:
                output.writeObject(getObject(instance));
                break;
            default:
                throw new IllegalStateException();
        }
    }
}
