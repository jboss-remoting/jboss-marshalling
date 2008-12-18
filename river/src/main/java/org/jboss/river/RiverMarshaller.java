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

package org.jboss.river;

import org.jboss.marshalling.AbstractMarshaller;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.MarshallingConfiguration;
import java.io.IOException;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.NotSerializableException;
import java.io.InvalidObjectException;
import java.io.InvalidClassException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

/**
 *
 */
public class RiverMarshaller extends AbstractMarshaller {
    private final IdentityIntMap<Object> instanceCache;
    private final IdentityIntMap<Class<?>> classCache;
    private final IdentityHashMap<Object, Object> replacementCache;
    private int instanceSeq;
    private int classSeq;
    private final SerializableClassRegistry registry;
    private RiverObjectOutputStream objectOutputStream;
    private RiverObjectOutput objectOutput;

    protected RiverMarshaller(final RiverMarshallerFactory marshallerFactory, final SerializableClassRegistry registry, final MarshallingConfiguration configuration) {
        super(marshallerFactory, configuration);
        this.registry = registry;
        final float loadFactor = 0x0.5p0f;
        instanceCache = new IdentityIntMap<Object>((int) ((double)configuration.getInstanceCount() / (double)loadFactor), loadFactor);
        classCache = new IdentityIntMap<Class<?>>((int) ((double)configuration.getClassCount() / (double)loadFactor), loadFactor);
        replacementCache = new IdentityHashMap<Object, Object>(configuration.getInstanceCount());
    }

    protected void doWriteObject(final Object original, final boolean unshared) throws IOException {
        final ExternalizerFactory externalizerFactory = this.externalizerFactory;
        final ObjectResolver objectResolver = this.objectResolver;
        boolean replacing = true;
        Object obj = original;
        Class<?> objClass;
        int id;
        boolean isArray, isEnum;
        SerializableClass info;
        int ttl = 100;
        for (;;) {
            if (--ttl == 0) {
                throw new InvalidObjectException("Replacement looped too many times");
            }
            if (obj == null) {
                write(Protocol.ID_NULL_OBJECT);
                return;
            }
            if (replacementCache.containsKey(obj)) {
                obj = replacementCache.get(obj);
                continue;
            }
            final int rid;
            if (! unshared && (rid = instanceCache.get(obj, -1)) != -1) {
                write(Protocol.ID_REPEAT_OBJECT);
                writeInt(rid);
                return;
            }
            final ObjectTable.Writer objectTableWriter;
            if (! unshared && (objectTableWriter = objectTable.getObjectWriter(obj)) != null) {
                write(Protocol.ID_PREDEFINED_OBJECT);
                objectTableWriter.writeObject(this, obj);
                return;
            }
            objClass = obj.getClass();
            id = BASIC_CLASSES.get(objClass, -1);
            // First, non-replaceable classes
            if (id == Protocol.ID_CLASS_CLASS) {
                final Class classObj = (Class) obj;
                write(Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_CLASS_CLASS);
                writeClassClass(classObj);
                instanceCache.put(classObj, instanceSeq++);
                return;
            }
            isEnum = obj instanceof Enum;
            isArray = objClass.isArray();
            info = isArray || isEnum ? null : registry.lookup(objClass);
            // replace once
            if (replacing) {
                if (info != null) {
                    // check for a user replacement
                    if (info.hasWriteReplace()) {
                        obj = info.callWriteReplace(obj);
                    }
                }
                // Check for a global replacement
                obj = objectResolver.writeReplace(obj);
                replacing = false;
                continue;
            } else {
                break;
            }
        }

        // Cache the replacement
        if (obj != original) {
            replacementCache.put(original, obj);
        }

        if (isEnum) {
            // objClass cannot equal Enum.class because it is abstract
            final Enum<?> theEnum = (Enum<?>) obj;
            // enums are always shared
            write(Protocol.ID_NEW_OBJECT);
            writeEnumClass(theEnum.getDeclaringClass());
            writeString(theEnum.name());
            instanceCache.put(obj, instanceSeq++);
            return;
        }
        // Now replaceable classes
        switch (id) {
            case Protocol.ID_BYTE_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_BYTE_CLASS);
                writeByte(((Byte) original).byteValue());
                return;
            }
            case Protocol.ID_BOOLEAN_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_BOOLEAN_CLASS);
                writeBoolean(((Boolean) original).booleanValue());
                return;
            }
            case Protocol.ID_CHARACTER_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_CHARACTER_CLASS);
                writeChar(((Character) original).charValue());
                return;
            }
            case Protocol.ID_DOUBLE_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_DOUBLE_CLASS);
                writeDouble(((Double) original).doubleValue());
                return;
            }
            case Protocol.ID_FLOAT_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_FLOAT_CLASS);
                writeFloat(((Float) original).floatValue());
                return;
            }
            case Protocol.ID_INTEGER_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_INTEGER_CLASS);
                writeInt(((Integer) original).intValue());
                return;
            }
            case Protocol.ID_LONG_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_LONG_CLASS);
                writeLong(((Long) original).longValue());
                return;
            }
            case Protocol.ID_SHORT_CLASS: {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_SHORT_CLASS);
                writeShort(((Short) original).shortValue());
                return;
            }
            case Protocol.ID_STRING_CLASS: {
                final String string = (String) obj;
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_STRING_CLASS);
                writeString(string);
                if (unshared) {
                    instanceCache.put(obj, -1);
                    instanceSeq++;
                } else {
                    instanceCache.put(obj, instanceSeq++);
                }
                return;
            }
            case Protocol.ID_BYTE_ARRAY_CLASS: {
                if (! unshared) {
                    instanceCache.put(obj, instanceSeq++);
                }
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_BYTE_ARRAY_CLASS);
                final byte[] bytes = (byte[]) obj;
                final int len = bytes.length;
                writeInt(len);
                write(bytes, 0, len);
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case Protocol.ID_BOOLEAN_ARRAY_CLASS: {
                if (! unshared) {
                    instanceCache.put(obj, instanceSeq++);
                }
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_BOOLEAN_ARRAY_CLASS);
                final boolean[] booleans = (boolean[]) obj;
                final int len = booleans.length;
                writeInt(len);
                final int bc = len & ~7;
                for (int i = 0; i < bc;) {
                    write(
                            (booleans[i++] ? 1 : 0)
                                    | (booleans[i++] ? 2 : 0)
                                    | (booleans[i++] ? 4 : 0)
                                    | (booleans[i++] ? 8 : 0)
                                    | (booleans[i++] ? 16 : 0)
                                    | (booleans[i++] ? 32 : 0)
                                    | (booleans[i++] ? 64 : 0)
                                    | (booleans[i++] ? 128 : 0)
                    );
                }
                if (bc < len) {
                    int out = 0;
                    int bit = 1;
                    for (int i = bc; i < len; i++) {
                        if (booleans[i]) out |= bit;
                        bit <<= 1;
                    }
                    write(out);
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case Protocol.ID_CHAR_ARRAY_CLASS: {
                if (! unshared) {
                    instanceCache.put(obj, instanceSeq++);
                }
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_CHAR_ARRAY_CLASS);
                final char[] chars = (char[]) obj;
                final int len = chars.length;
                writeInt(len);
                for (int i = 0; i < len; i ++) {
                    writeChar(chars[i]);
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case Protocol.ID_SHORT_ARRAY_CLASS: {
                if (! unshared) {
                    instanceCache.put(obj, instanceSeq++);
                }
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_SHORT_ARRAY_CLASS);
                final short[] shorts = (short[]) obj;
                final int len = shorts.length;
                writeInt(len);
                for (int i = 0; i < len; i ++) {
                    writeShort(shorts[i]);
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case Protocol.ID_INT_ARRAY_CLASS: {
                if (! unshared) {
                    instanceCache.put(obj, instanceSeq++);
                }
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_INT_ARRAY_CLASS);
                final int[] ints = (int[]) obj;
                final int len = ints.length;
                writeInt(len);
                for (int i = 0; i < len; i ++) {
                    writeInt(ints[i]);
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case Protocol.ID_LONG_ARRAY_CLASS: {
                if (! unshared) {
                    instanceCache.put(obj, instanceSeq++);
                }
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_LONG_ARRAY_CLASS);
                final long[] longs = (long[]) obj;
                final int len = longs.length;
                writeInt(len);
                for (int i = 0; i < len; i ++) {
                    writeLong(longs[i]);
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case Protocol.ID_FLOAT_ARRAY_CLASS: {
                if (! unshared) {
                    instanceCache.put(obj, instanceSeq++);
                }
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_FLOAT_ARRAY_CLASS);
                final float[] floats = (float[]) obj;
                final int len = floats.length;
                writeInt(len);
                for (int i = 0; i < len; i ++) {
                    writeFloat(floats[i]);
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case Protocol.ID_DOUBLE_ARRAY_CLASS: {
                instanceCache.put(obj, instanceSeq++);
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                write(Protocol.ID_DOUBLE_ARRAY_CLASS);
                final double[] doubles = (double[]) obj;
                final int len = doubles.length;
                writeInt(len);
                for (int i = 0; i < len; i ++) {
                    writeDouble(doubles[i]);
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            case -1: break;
            default: throw new NotSerializableException(objClass.getName());
        }
        if (isArray) {
            write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
            writeObjectArrayClass(objClass);
            instanceCache.put(obj, instanceSeq++);
            final Object[] objects = (Object[]) obj;
            final int len = objects.length;
            writeInt(len);
            for (int i = 0; i < len; i++) {
                doWriteObject(objects[i], unshared);
            }
            if (unshared) {
                instanceCache.put(obj, -1);
            }
            return;
        }
        // serialize proxies efficiently
        if (Proxy.isProxyClass(objClass)) {
            write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
            instanceCache.put(obj, instanceSeq++);
            writeProxyClass(objClass);
            doWriteObject(Proxy.getInvocationHandler(obj), false);
            if (unshared) {
                instanceCache.put(obj, -1);
            }
            return;
        }
        // it's a user type
        // user type #1: externalizer
        final Externalizer externalizer = externalizerFactory.getExternalizer(obj);
        if (externalizer != null) {
            write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
            writeExternalizerClass(objClass, externalizer);
            instanceCache.put(obj, instanceSeq++);
            final RiverObjectOutput objectOutput = getObjectOutput();
            externalizer.writeExternal(obj, objectOutput);
            if (unshared) {
                instanceCache.put(obj, -1);
            }
            return;
        }
        // user type #2: externalizable
        if (obj instanceof Externalizable) {
            write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
            instanceCache.put(obj, instanceSeq++);
            final Externalizable ext = (Externalizable) obj;
            final RiverObjectOutput objectOutput = getObjectOutput();
            writeExternalizableClass(objClass);
            objectOutput.start();
            ext.writeExternal(objectOutput);
            objectOutput.finish();
            if (unshared) {
                instanceCache.put(obj, -1);
            }
            return;
        }
        // user type #3: serializable
        if (obj instanceof Serializable) {
            write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
            writeSerializableClass(objClass);
            instanceCache.put(obj, instanceSeq++);
            doWriteSerializableObject(info, obj, objClass);
            if (unshared) {
                instanceCache.put(obj, -1);
            }
            return;
        }
        throw new NotSerializableException(objClass.getName());
    }

    protected RiverObjectOutput getObjectOutput() {
        final RiverObjectOutput output = objectOutput;
        return output == null ? objectOutput = new RiverObjectOutput(this) : output;
    }


    protected void doWriteSerializableObject(final SerializableClass info, final Object obj, final Class<?> objClass) throws IOException {
        final Class<?> superclass = objClass.getSuperclass();
        if (Serializable.class.isAssignableFrom(superclass)) {
            doWriteSerializableObject(registry.lookup(superclass), obj, superclass);
        }
        if (info.hasWriteObject()) {
            final RiverObjectOutputStream objectOutputStream = getObjectOutputStream();
            final SerializableClass oldInfo = objectOutputStream.swapClass(info);
            final Object oldObj = objectOutputStream.swapCurrent(obj);
            final RiverObjectOutputStream.State restoreState = objectOutputStream.start();
            boolean ok = false;
            try {
                info.callWriteObject(obj, objectOutputStream);
                objectOutputStream.finish(restoreState);
                objectOutputStream.swapCurrent(oldObj);
                objectOutputStream.swapClass(oldInfo);
                ok = true;
            } finally {
                if (! ok) {
                    objectOutputStream.fullReset();
                }
            }
        } else {
            doWriteFields(info, obj);
            // todo - write empty block marker?
        }
    }

    protected void doWriteFields(final SerializableClass info, final Object obj) throws IOException {
        final SerializableField[] serializableFields = info.getFields();
        final int cnt = serializableFields.length;
        for (int i = 0; i < cnt; i++) {
            SerializableField serializableField = serializableFields[i];
            try {
                final Field field = serializableField.getField();
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        writeBoolean(field.getBoolean(obj));
                        break;
                    }
                    case BYTE: {
                        writeByte(field.getByte(obj));
                        break;
                    }
                    case SHORT: {
                        writeShort(field.getShort(obj));
                        break;
                    }
                    case INT: {
                        writeInt(field.getInt(obj));
                        break;
                    }
                    case CHAR: {
                        writeChar(field.getChar(obj));
                        break;
                    }
                    case LONG: {
                        writeLong(field.getLong(obj));
                        break;
                    }
                    case DOUBLE: {
                        writeDouble(field.getDouble(obj));
                        break;
                    }
                    case FLOAT: {
                        writeFloat(field.getFloat(obj));
                        break;
                    }
                    case OBJECT: {
                        doWriteObject(field.get(obj), serializableField.isUnshared());
                        break;
                    }
                }
            } catch (IllegalAccessException e) {
                final InvalidObjectException ioe = new InvalidObjectException("Unexpected illegal access exception");
                ioe.initCause(e);
                throw ioe;
            }
        }
    }

    private RiverObjectOutputStream getObjectOutputStream() throws IOException {
        final RiverObjectOutputStream objectOutputStream = this.objectOutputStream;
        return objectOutputStream == null ? this.objectOutputStream = createObjectOutputStream() : objectOutputStream;
    }

    private RiverObjectOutputStream createObjectOutputStream() throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<RiverObjectOutputStream>() {
                public RiverObjectOutputStream run() throws IOException {
                    return new RiverObjectOutputStream(RiverMarshaller.this);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    protected void writeProxyClass(final Class<?> objClass) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewProxyClass(objClass);
        }
    }

    protected void writeNewProxyClass(final Class<?> objClass) throws IOException {
        ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
        if (classTableWriter != null) {
            write(Protocol.ID_PREDEFINED_PROXY_CLASS);
            classCache.put(objClass, classSeq++);
            classTableWriter.writeClass(this, objClass);
        } else {
            write(Protocol.ID_PROXY_CLASS);
            final Class<?>[] interfaces = objClass.getInterfaces();
            writeInt(interfaces.length);
            for (Class<?> interf : interfaces) {
                writeString(interf.getName());
            }
            classCache.put(objClass, classSeq++);
            classResolver.annotateProxyClass(this, objClass);
        }
    }

    protected void writeEnumClass(final Class<? extends Enum> objClass) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewEnumClass(objClass);
        }
    }

    protected void writeNewEnumClass(final Class<? extends Enum> objClass) throws IOException {
        ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
        if (classTableWriter != null) {
            write(Protocol.ID_PREDEFINED_ENUM_TYPE_CLASS);
            classCache.put(objClass, classSeq++);
            classTableWriter.writeClass(this, objClass);
        } else {
            write(Protocol.ID_ENUM_TYPE_CLASS);
            writeString(objClass.getName());
            classCache.put(objClass, classSeq++);
            doAnnotateClass(objClass);
        }
    }

    protected void writeClassClass(final Class<?> classObj) throws IOException {
        write(Protocol.ID_CLASS_CLASS);
        writeClass(classObj);
        // not cached
    }

    protected void writeObjectArrayClass(final Class<?> objClass) throws IOException {
        write(Protocol.ID_OBJECT_ARRAY_TYPE_CLASS);
        writeClass(objClass.getComponentType());
        classCache.put(objClass, classSeq++);
    }

    protected void writeClass(final Class<?> objClass) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewClass(objClass);
        }
    }

    private static final IdentityIntMap<Class<?>> BASIC_CLASSES;

    static {
        final IdentityIntMap<Class<?>> map = new IdentityIntMap<Class<?>>(0x0.6p0f);

        map.put(byte.class, Protocol.ID_PRIM_BYTE);
        map.put(boolean.class, Protocol.ID_PRIM_BOOLEAN);
        map.put(char.class, Protocol.ID_PRIM_CHAR);
        map.put(double.class, Protocol.ID_PRIM_DOUBLE);
        map.put(float.class, Protocol.ID_PRIM_FLOAT);
        map.put(int.class, Protocol.ID_PRIM_INT);
        map.put(long.class, Protocol.ID_PRIM_LONG);
        map.put(short.class, Protocol.ID_PRIM_SHORT);

        map.put(void.class, Protocol.ID_VOID);

        map.put(Byte.class, Protocol.ID_BYTE_CLASS);
        map.put(Boolean.class, Protocol.ID_BOOLEAN_CLASS);
        map.put(Character.class, Protocol.ID_CHARACTER_CLASS);
        map.put(Double.class, Protocol.ID_DOUBLE_CLASS);
        map.put(Float.class, Protocol.ID_FLOAT_CLASS);
        map.put(Integer.class, Protocol.ID_INTEGER_CLASS);
        map.put(Long.class, Protocol.ID_LONG_CLASS);
        map.put(Short.class, Protocol.ID_SHORT_CLASS);

        map.put(Void.class, Protocol.ID_VOID_CLASS);

        map.put(Object.class, Protocol.ID_OBJECT_CLASS);
        map.put(Class.class, Protocol.ID_CLASS_CLASS);
        map.put(String.class, Protocol.ID_STRING_CLASS);
        map.put(Enum.class, Protocol.ID_ENUM_CLASS);

        map.put(byte[].class, Protocol.ID_BYTE_ARRAY_CLASS);
        map.put(boolean[].class, Protocol.ID_BOOLEAN_ARRAY_CLASS);
        map.put(char[].class, Protocol.ID_CHAR_ARRAY_CLASS);
        map.put(double[].class, Protocol.ID_DOUBLE_ARRAY_CLASS);
        map.put(float[].class, Protocol.ID_FLOAT_ARRAY_CLASS);
        map.put(int[].class, Protocol.ID_INT_ARRAY_CLASS);
        map.put(long[].class, Protocol.ID_LONG_ARRAY_CLASS);
        map.put(short[].class, Protocol.ID_SHORT_ARRAY_CLASS);

        BASIC_CLASSES = map;
    }

    protected void writeNewClass(final Class<?> objClass) throws IOException {
        if (objClass.isEnum()) {
            writeNewEnumClass(objClass.asSubclass(Enum.class));
        } else if (Proxy.isProxyClass(objClass)) {
            writeNewProxyClass(objClass);
        } else if (objClass.isArray()) {
            writeObjectArrayClass(objClass);
        } else if (Serializable.class.isAssignableFrom(objClass)) {
            if (Externalizable.class.isAssignableFrom(objClass)) {
                writeNewExternalizableClass(objClass);
            } else {
                writeNewSerializableClass(objClass);
            }
        } else {
            ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
            if (classTableWriter != null) {
                write(Protocol.ID_PREDEFINED_PLAIN_CLASS);
                classCache.put(objClass, classSeq++);
                classTableWriter.writeClass(this, objClass);
            } else {
                write(Protocol.ID_PLAIN_CLASS);
                writeString(objClass.getName());
                doAnnotateClass(objClass);
                classCache.put(objClass, classSeq++);
            }
        }
    }

    protected boolean writeKnownClass(final Class<?> objClass) throws IOException {
        int i = BASIC_CLASSES.get(objClass, -1);
        if (i != -1) {
            write(i);
            return true;
        }
        i = classCache.get(objClass, -1);
        if (i != -1) {
            write(Protocol.ID_REPEAT_CLASS);
            writeInt(i);
            return true;
        }
        return false;
    }

    protected void writeSerializableClass(final Class<?> objClass) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewSerializableClass(objClass);
        }
    }

    protected void writeNewSerializableClass(final Class<?> objClass) throws IOException {
        ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
        if (classTableWriter != null) {
            write(Protocol.ID_PREDEFINED_SERIALIZABLE_CLASS);
            classCache.put(objClass, classSeq++);
            classTableWriter.writeClass(this, objClass);
        } else {
            write(Protocol.ID_SERIALIZABLE_CLASS);
            writeString(objClass.getName());
            final SerializableClass info = registry.lookup(objClass);
            writeLong(info.getEffectiveSerialVersionUID());
            classCache.put(objClass, classSeq++);
            doAnnotateClass(objClass);
            final SerializableField[] fields = info.getFields();
            final int cnt = fields.length;
            writeInt(cnt);
            for (int i = 0; i < cnt; i++) {
                SerializableField field = fields[i];
                writeUTF(field.getName());
                try {
                    writeClass(field.getType());
                } catch (ClassNotFoundException e) {
                    throw new InvalidClassException("Class of field was unloaded");
                }
                writeBoolean(field.isUnshared());
            }
        }
        writeClass(objClass.getSuperclass());
    }

    protected void writeExternalizableClass(final Class<?> objClass) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewExternalizableClass(objClass);
        }
    }

    protected void writeNewExternalizableClass(final Class<?> objClass) throws IOException {
        ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
        if (classTableWriter != null) {
            write(Protocol.ID_PREDEFINED_EXTERNALIZABLE_CLASS);
            classCache.put(objClass, classSeq++);
            classTableWriter.writeClass(this, objClass);
        } else {
            write(Protocol.ID_EXTERNALIZABLE_CLASS);
            writeString(objClass.getName());
            writeLong(registry.lookup(objClass).getEffectiveSerialVersionUID());
            classCache.put(objClass, classSeq++);
            doAnnotateClass(objClass);
        }
    }

    protected void writeExternalizerClass(final Class<?> objClass, final Externalizer externalizer) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewExternalizerClass(objClass, externalizer);
        }
    }

    protected void writeNewExternalizerClass(final Class<?> objClass, final Externalizer externalizer) throws IOException {
        ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
        if (classTableWriter != null) {
            write(Protocol.ID_PREDEFINED_EXTERNALIZER_CLASS);
            classCache.put(objClass, classSeq++);
            classTableWriter.writeClass(this, objClass);
        } else {
            write(Protocol.ID_EXTERNALIZER_CLASS);
            writeString(objClass.getName());
            classCache.put(objClass, classSeq++);
            doAnnotateClass(objClass);
            writeObject(externalizer);
        }
    }

    protected void doAnnotateClass(final Class<?> objClass) throws IOException {
        classResolver.annotateClass(this, objClass);
    }

    public void clearInstanceCache() throws IOException {
        instanceCache.clear();
        replacementCache.clear();
        instanceSeq = 0;
    }

    public void clearClassCache() throws IOException {
        classCache.clear();
        classSeq = 0;
        clearInstanceCache();
    }

    private void writeString(String string) throws IOException {
        writeInt(string.length());
        flush();
        UTFUtils.writeUTFBytes(byteOutput, string);
    }

    // Replace writeUTF with a faster, non-scanning version

    public void writeUTF(final String string) throws IOException {
        writeInt(string.length());
        flush();
        UTFUtils.writeUTFBytes(byteOutput, string);
    }
}
