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
import org.jboss.marshalling.ClassExternalizerFactory;
import org.jboss.marshalling.MarshallerObjectOutput;
import org.jboss.marshalling.util.IdentityIntMap;
import java.io.IOException;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.NotSerializableException;
import java.io.InvalidObjectException;
import java.io.InvalidClassException;
import java.io.ObjectOutput;
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
    private final IdentityHashMap<Class<?>, Externalizer> externalizers;
    private int instanceSeq;
    private int classSeq;
    private final SerializableClassRegistry registry;
    private RiverObjectOutputStream objectOutputStream;
    private ObjectOutput objectOutput;
    private BlockMarshaller blockMarshaller;

    protected RiverMarshaller(final RiverMarshallerFactory marshallerFactory, final SerializableClassRegistry registry, final MarshallingConfiguration configuration) throws IOException {
        super(marshallerFactory, configuration);
        if (configuredVersion > Protocol.MAX_VERSION) {
            throw new IOException("Protocol version not supported");
        }
        this.registry = registry;
        final float loadFactor = 0x0.5p0f;
        instanceCache = new IdentityIntMap<Object>((int) ((double)configuration.getInstanceCount() / (double)loadFactor), loadFactor);
        classCache = new IdentityIntMap<Class<?>>((int) ((double)configuration.getClassCount() / (double)loadFactor), loadFactor);
        externalizers = new IdentityHashMap<Class<?>, Externalizer>(configuration.getClassCount());
    }

    protected void doWriteObject(final Object original, final boolean unshared) throws IOException {
        final ClassExternalizerFactory classExternalizerFactory = this.classExternalizerFactory;
        final ExternalizerFactory externalizerFactory = this.externalizerFactory;
        final ObjectResolver objectResolver = this.objectResolver;
        Object obj = original;
        Class<?> objClass;
        int id;
        boolean isArray, isEnum;
        SerializableClass info;
        boolean unreplaced = true;
        try {
            for (;;) {
                if (obj == null) {
                    write(Protocol.ID_NULL_OBJECT);
                    return;
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
                    if (configuredVersion > 0) {
                        objectTableWriter.writeObject(getBlockMarshaller(), obj);
                        writeEndBlock();
                    } else {
                        objectTableWriter.writeObject(this, obj);
                    }
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
                if (unreplaced) {
                    if (info != null) {
                        // check for a user replacement
                        if (info.hasWriteReplace()) {
                            obj = info.callWriteReplace(obj);
                        }
                    }
                    // Check for a global replacement
                    obj = objectResolver.writeReplace(obj);
                    unreplaced = false;
                    continue;
                } else {
                    break;
                }
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
                    writeByte(((Byte) obj).byteValue());
                    return;
                }
                case Protocol.ID_BOOLEAN_CLASS: {
                    write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                    write(Protocol.ID_BOOLEAN_CLASS);
                    writeBoolean(((Boolean) obj).booleanValue());
                    return;
                }
                case Protocol.ID_CHARACTER_CLASS: {
                    write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                    write(Protocol.ID_CHARACTER_CLASS);
                    writeChar(((Character) obj).charValue());
                    return;
                }
                case Protocol.ID_DOUBLE_CLASS: {
                    write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                    write(Protocol.ID_DOUBLE_CLASS);
                    writeDouble(((Double) obj).doubleValue());
                    return;
                }
                case Protocol.ID_FLOAT_CLASS: {
                    write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                    write(Protocol.ID_FLOAT_CLASS);
                    writeFloat(((Float) obj).floatValue());
                    return;
                }
                case Protocol.ID_INTEGER_CLASS: {
                    write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                    write(Protocol.ID_INTEGER_CLASS);
                    writeInt(((Integer) obj).intValue());
                    return;
                }
                case Protocol.ID_LONG_CLASS: {
                    write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                    write(Protocol.ID_LONG_CLASS);
                    writeLong(((Long) obj).longValue());
                    return;
                }
                case Protocol.ID_SHORT_CLASS: {
                    write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                    write(Protocol.ID_SHORT_CLASS);
                    writeShort(((Short) obj).shortValue());
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
            Externalizer externalizer;
            if (externalizers.containsKey(objClass)) {
                externalizer = externalizers.get(objClass);
            } else {
                externalizer = classExternalizerFactory.getExternalizer(objClass);
                if (externalizer == null) {
                    externalizer = externalizerFactory.getExternalizer(obj);
                }
                externalizers.put(objClass, externalizer);
            }
            if (externalizer != null) {
                write(unshared ? Protocol.ID_NEW_OBJECT_UNSHARED : Protocol.ID_NEW_OBJECT);
                writeExternalizerClass(objClass, externalizer);
                instanceCache.put(obj, instanceSeq++);
                final ObjectOutput objectOutput;
                objectOutput = getObjectOutput();
                externalizer.writeExternal(obj, objectOutput);
                writeEndBlock();
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
                final ObjectOutput objectOutput = getObjectOutput();
                writeExternalizableClass(objClass);
                ext.writeExternal(objectOutput);
                writeEndBlock();
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
        } finally {
            if (! unreplaced && obj != original) {
                final int replId = instanceCache.get(obj, -1);
                if (replId != -1) {
                    instanceCache.put(original, replId);
                }
            }
        }
    }

    private void writeEndBlock() throws IOException {
        final BlockMarshaller blockMarshaller = this.blockMarshaller;
        if (blockMarshaller != null) {
            blockMarshaller.flush();
            writeByte(Protocol.ID_END_BLOCK_DATA);
        }
    }

    protected ObjectOutput getObjectOutput() {
        final ObjectOutput output = objectOutput;
        return output == null ? configuredVersion == 0 ? (objectOutput = new MarshallerObjectOutput(this)) : (objectOutput = getBlockMarshaller()) : output;
    }

    protected BlockMarshaller getBlockMarshaller() {
        final BlockMarshaller blockMarshaller = this.blockMarshaller;
        return blockMarshaller == null ? (this.blockMarshaller = new BlockMarshaller(this, bufferSize)) : blockMarshaller;
    }

    private RiverObjectOutputStream getObjectOutputStream() throws IOException {
        final RiverObjectOutputStream objectOutputStream = this.objectOutputStream;
        return objectOutputStream == null ? this.objectOutputStream = createObjectOutputStream() : objectOutputStream;
    }

    private final PrivilegedExceptionAction<RiverObjectOutputStream> createObjectOutputStreamAction = new PrivilegedExceptionAction<RiverObjectOutputStream>() {
        public RiverObjectOutputStream run() throws IOException {
            return new RiverObjectOutputStream(configuredVersion == 0 ? RiverMarshaller.this : getBlockMarshaller(), RiverMarshaller.this);
        }
    };

    private RiverObjectOutputStream createObjectOutputStream() throws IOException {
        try {
            return AccessController.doPrivileged(createObjectOutputStreamAction);
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
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
                writeEndBlock();
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
        }
    }

    protected void doWriteFields(final SerializableClass info, final Object obj) throws IOException {
        final SerializableField[] serializableFields = info.getFields();
        for (SerializableField serializableField : serializableFields) {
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
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(Protocol.ID_PROXY_CLASS);
            final String[] names = classResolver.getProxyInterfaces(objClass);
            writeInt(names.length);
            for (String name : names) {
                writeString(name);
            }
            classCache.put(objClass, classSeq++);
            if (configuredVersion > 0) {
                final BlockMarshaller blockMarshaller = getBlockMarshaller();
                classResolver.annotateProxyClass(blockMarshaller, objClass);
                writeEndBlock();
            } else {
                classResolver.annotateProxyClass(this, objClass);
            }
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
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(Protocol.ID_ENUM_TYPE_CLASS);
            writeString(classResolver.getClassName(objClass));
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
        } else if (! objClass.isInterface() && Serializable.class.isAssignableFrom(objClass)) {
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
                writeClassTableData(objClass, classTableWriter);
            } else {
                write(Protocol.ID_PLAIN_CLASS);
                writeString(classResolver.getClassName(objClass));
                doAnnotateClass(objClass);
                classCache.put(objClass, classSeq++);
            }
        }
    }

    private void writeClassTableData(final Class<?> objClass, final ClassTable.Writer classTableWriter) throws IOException {
        if (configuredVersion > 0) {
            classTableWriter.writeClass(getBlockMarshaller(), objClass);
            writeEndBlock();
        } else {
            classTableWriter.writeClass(this, objClass);
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
            writeClassTableData(objClass, classTableWriter);
        } else {
            final SerializableClass info = registry.lookup(objClass);
            if (configuredVersion > 0 && info.hasWriteObject()) {
                write(Protocol.ID_WRITE_OBJECT_CLASS);
            } else {
                write(Protocol.ID_SERIALIZABLE_CLASS);
            }
            writeString(classResolver.getClassName(objClass));
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
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(Protocol.ID_EXTERNALIZABLE_CLASS);
            writeString(classResolver.getClassName(objClass));
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
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(Protocol.ID_EXTERNALIZER_CLASS);
            writeString(classResolver.getClassName(objClass));
            classCache.put(objClass, classSeq++);
            doAnnotateClass(objClass);
        }
        writeObject(externalizer);
    }

    protected void doAnnotateClass(final Class<?> objClass) throws IOException {
        if (configuredVersion > 0) {
            classResolver.annotateClass(getBlockMarshaller(), objClass);
            writeEndBlock();
        } else {
            classResolver.annotateClass(this, objClass);
        }
    }

    public void clearInstanceCache() throws IOException {
        instanceCache.clear();
        instanceSeq = 0;
        if (byteOutput != null) {
            write(Protocol.ID_CLEAR_INSTANCE_CACHE);
        }
    }

    public void clearClassCache() throws IOException {
        classCache.clear();
        externalizers.clear();
        classSeq = 0;
        instanceCache.clear();
        instanceSeq = 0;
        if (byteOutput != null) {
            write(Protocol.ID_CLEAR_CLASS_CACHE);
        }
    }

    protected void doStart() throws IOException {
        super.doStart();
        final int configuredVersion = this.configuredVersion;
        if (configuredVersion > 0) {
            writeByte(configuredVersion);
        }
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
