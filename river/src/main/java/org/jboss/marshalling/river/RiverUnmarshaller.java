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

import org.jboss.marshalling.AbstractUnmarshaller;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.MarshallerObjectInput;
import java.util.ArrayList;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Collections;
import java.util.HashSet;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.InvalidObjectException;
import java.io.InvalidClassException;
import java.io.Externalizable;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

/**
 *
 */
public class RiverUnmarshaller extends AbstractUnmarshaller {
    private final ArrayList<Object> instanceCache;
    private final ArrayList<ClassDescriptor> classCache;
    private final SerializableClassRegistry registry;
    private ObjectInput objectInput;
    private int version;
    private int depth;
    private BlockUnmarshaller blockUnmarshaller;
    private RiverObjectInputStream objectInputStream;
    private final SortedMap<Integer, Set<ObjectInputValidation>> validationMap = new TreeMap<Integer, Set<ObjectInputValidation>>(Collections.reverseOrder());

    private static final Field proxyInvocationHandler;

    static {
        proxyInvocationHandler = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    final Field field = Proxy.class.getDeclaredField("h");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new NoSuchFieldError(e.getMessage());
                }
            }
        });
    }

    protected RiverUnmarshaller(final RiverMarshallerFactory marshallerFactory, final SerializableClassRegistry registry, final MarshallingConfiguration configuration) {
        super(marshallerFactory, configuration);
        this.registry = registry;
        instanceCache = new ArrayList<Object>(configuration.getInstanceCount());
        classCache = new ArrayList<ClassDescriptor>(configuration.getClassCount());
    }

    public void clearInstanceCache() throws IOException {
        instanceCache.clear();
    }

    public void clearClassCache() throws IOException {
        clearInstanceCache();
        classCache.clear();
    }

    public void close() throws IOException {
        finish();
    }

    public void finish() throws IOException {
        super.finish();
        blockUnmarshaller = null;
        objectInput = null;
        objectInputStream = null;
    }

    private ObjectInput getObjectInput() {
        final ObjectInput objectInput = this.objectInput;
        return objectInput == null ? (this.objectInput = (version > 0 ? getBlockUnmarshaller() : new MarshallerObjectInput(this))) : objectInput;
    }

    private BlockUnmarshaller getBlockUnmarshaller() {
        final BlockUnmarshaller blockUnmarshaller = this.blockUnmarshaller;
        return blockUnmarshaller == null ? this.blockUnmarshaller = new BlockUnmarshaller(this) : blockUnmarshaller;
    }

    private final PrivilegedExceptionAction<RiverObjectInputStream> createObjectInputStreamAction = new PrivilegedExceptionAction<RiverObjectInputStream>() {
        public RiverObjectInputStream run() throws IOException {
            return new RiverObjectInputStream(RiverUnmarshaller.this, version > 0 ? getBlockUnmarshaller() : RiverUnmarshaller.this);
        }
    };

    private RiverObjectInputStream getObjectInputStream() throws IOException {
        final RiverObjectInputStream objectInputStream = this.objectInputStream;
        return objectInputStream == null ? this.objectInputStream = createObjectInputStream() : objectInputStream;
    }

    private RiverObjectInputStream createObjectInputStream() throws IOException {
        try {
            return AccessController.doPrivileged(createObjectInputStreamAction);
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    protected Object doReadObject(final boolean unshared) throws ClassNotFoundException, IOException {
        final Object obj = doReadObject(readUnsignedByte(), unshared);
        if (depth == 0) try {
            for (Set<ObjectInputValidation> validations : validationMap.values()) {
                for (ObjectInputValidation validation : validations) {
                    validation.validateObject();
                }
            }
        } finally {
            validationMap.clear();
        }
        return obj;
    }

    Object doReadObject(int leadByte, final boolean unshared) throws IOException, ClassNotFoundException {
        depth ++;
        try {
            for (;;) switch (leadByte) {
                case Protocol.ID_NULL_OBJECT: {
                    return null;
                }
                case Protocol.ID_REPEAT_OBJECT: {
                    if (unshared) {
                        throw new InvalidObjectException("Attempt to read a backreference as unshared");
                    }
                    try {
                        final Object obj = instanceCache.get(readInt());
                        if (obj != null) return obj;
                    } catch (IndexOutOfBoundsException e) {
                    }
                    throw new InvalidObjectException("Attempt to read a backreference with an invalid ID");
                }
                case Protocol.ID_NEW_OBJECT:
                case Protocol.ID_NEW_OBJECT_UNSHARED: {
                    if (unshared != (leadByte == Protocol.ID_NEW_OBJECT_UNSHARED)) {
                        throw new InvalidObjectException("Shared/unshared object mismatch");
                    }
                    return doReadNewObject(readUnsignedByte(), unshared);
                }
                case Protocol.ID_PREDEFINED_OBJECT: {
                    if (unshared) {
                        throw new InvalidObjectException("Attempt to read a predefined object as unshared");
                    }
                    if (version == 1) {
                        final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                        final Object obj = objectTable.readObject(blockUnmarshaller);
                        blockUnmarshaller.readToEndBlockData();
                        blockUnmarshaller.unblock();
                        return obj;
                    } else {
                        return objectTable.readObject(this);
                    }
                }
                case Protocol.ID_CLEAR_CLASS_CACHE: {
                    if (depth > 1) {
                        throw new StreamCorruptedException("ID_CLEAR_CLASS_CACHE token in the middle of stream processing");
                    }
                    classCache.clear();
                    instanceCache.clear();
                    leadByte = readUnsignedByte();
                    continue;
                }
                case Protocol.ID_CLEAR_INSTANCE_CACHE: {
                    if (depth > 1) {
                        throw new StreamCorruptedException("ID_CLEAR_INSTANCE_CACHE token in the middle of stream processing");
                    }
                    instanceCache.clear();
                    continue;
                }
                default: {
                    throw new StreamCorruptedException("Unexpected byte found when reading an object: " + leadByte);
                }
            }
        } finally {
            depth --;
        }
    }

    ClassDescriptor doReadClassDescriptor(final int classType) throws IOException, ClassNotFoundException {
        final ArrayList<ClassDescriptor> classCache = this.classCache;
        switch (classType) {
            case Protocol.ID_REPEAT_CLASS: {
                return classCache.get(readInt());
            }
            case Protocol.ID_PREDEFINED_ENUM_TYPE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, Protocol.ID_ENUM_TYPE_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case Protocol.ID_PREDEFINED_EXTERNALIZABLE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, Protocol.ID_EXTERNALIZABLE_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case Protocol.ID_PREDEFINED_EXTERNALIZER_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final Externalizer externalizer = (Externalizer) readObject();
                final ClassDescriptor descriptor = new ExternalizerClassDescriptor(type, externalizer);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case Protocol.ID_PREDEFINED_PLAIN_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, Protocol.ID_PLAIN_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case Protocol.ID_PREDEFINED_PROXY_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, Protocol.ID_PROXY_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case Protocol.ID_PREDEFINED_SERIALIZABLE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final SerializableClass serializableClass = registry.lookup(type);
                int descType = version > 0 && serializableClass.hasWriteObject() ? Protocol.ID_WRITE_OBJECT_CLASS : Protocol.ID_SERIALIZABLE_CLASS;
                final ClassDescriptor descriptor = new SerializableClassDescriptor(serializableClass, doReadClassDescriptor(readUnsignedByte()), serializableClass.getFields(), descType);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case Protocol.ID_PLAIN_CLASS: {
                final String className = readString();
                final Class<?> clazz = doResolveClass(className, 0L);
                final ClassDescriptor descriptor = new ClassDescriptor(clazz, Protocol.ID_PLAIN_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case Protocol.ID_PROXY_CLASS: {
                String[] interfaces = new String[readInt()];
                for (int i = 0; i < interfaces.length; i ++) {
                    interfaces[i] = readString();
                }
                final ClassDescriptor descriptor;
                if (version == 1) {
                    final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                    descriptor = new ClassDescriptor(classResolver.resolveProxyClass(blockUnmarshaller, interfaces), Protocol.ID_PROXY_CLASS);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else {
                    descriptor = new ClassDescriptor(classResolver.resolveProxyClass(this, interfaces), Protocol.ID_PROXY_CLASS);
                }
                classCache.add(descriptor);
                return descriptor;
            }
            case Protocol.ID_WRITE_OBJECT_CLASS:
            case Protocol.ID_SERIALIZABLE_CLASS: {
                int idx = classCache.size();
                classCache.add(null);
                final String className = readString();
                final long uid = readLong();
                final Class<?> clazz = doResolveClass(className, uid);
                final Class<?> superClazz = clazz.getSuperclass();
                classCache.set(idx, new IncompleteClassDescriptor(clazz, classType));
                final int cnt = readInt();
                final String[] names = new String[cnt];
                final ClassDescriptor[] descriptors = new ClassDescriptor[cnt];
                final boolean[] unshareds = new boolean[cnt];
                for (int i = 0; i < cnt; i ++) {
                    names[i] = readUTF();
                    descriptors[i] = doReadClassDescriptor(readUnsignedByte());
                    unshareds[i] = readBoolean();
                }
                ClassDescriptor superDescriptor = doReadClassDescriptor(readUnsignedByte());
                if (superDescriptor != null) {
                    final Class<?> superType = superDescriptor.getType();
                    if (! superType.isAssignableFrom(clazz)) {
                        throw new InvalidClassException(clazz.getName(), "Class does not extend stream superclass");
                    }
                    Class<?> cl = superClazz;
                    while (cl != superType) {
                        superDescriptor = new SerializableClassDescriptor(registry.lookup(cl), superDescriptor);
                        cl = cl.getSuperclass();
                    }
                } else if (superClazz != null) {
                    Class<?> cl = superClazz;
                    while (Serializable.class.isAssignableFrom(cl)) {
                        superDescriptor = new SerializableClassDescriptor(registry.lookup(cl), superDescriptor);
                        cl = cl.getSuperclass();
                    }
                }
                final SerializableClass serializableClass = registry.lookup(clazz);
                final SerializableField[] fields = new SerializableField[cnt];
                for (int i = 0; i < cnt; i ++) {
                    fields[i] = serializableClass.getSerializableField(names[i], descriptors[i].getType(), unshareds[i]);
                }
                final ClassDescriptor descriptor = new SerializableClassDescriptor(serializableClass, superDescriptor, fields, classType);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case Protocol.ID_EXTERNALIZABLE_CLASS: {
                final String className = readString();
                final long uid = readLong();
                final Class<?> clazz = doResolveClass(className, uid);
                final ClassDescriptor descriptor = new ClassDescriptor(clazz, Protocol.ID_EXTERNALIZABLE_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case Protocol.ID_EXTERNALIZER_CLASS: {
                final String className = readString();
                int idx = classCache.size();
                classCache.add(null);
                final Class<?> clazz = doResolveClass(className, 0L);
                final Externalizer externalizer = (Externalizer) readObject();
                final ClassDescriptor descriptor = new ExternalizerClassDescriptor(clazz, externalizer);
                classCache.set(idx, descriptor);
                return descriptor;
            }

            case Protocol.ID_ENUM_TYPE_CLASS: {
                final ClassDescriptor descriptor = new ClassDescriptor(doResolveClass(readString(), 0L), Protocol.ID_ENUM_TYPE_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case Protocol.ID_OBJECT_ARRAY_TYPE_CLASS: {
                final ClassDescriptor elementType = doReadClassDescriptor(readUnsignedByte());
                final ClassDescriptor arrayDescriptor = new ClassDescriptor(Array.newInstance(elementType.getType(), 0).getClass(), Protocol.ID_OBJECT_ARRAY_TYPE_CLASS);
                classCache.add(arrayDescriptor);
                return arrayDescriptor;
            }

            case Protocol.ID_STRING_CLASS: {
                return ClassDescriptor.STRING_DESCRIPTOR;
            }
            case Protocol.ID_OBJECT_CLASS: {
                return ClassDescriptor.OBJECT_DESCRIPTOR;
            }
            case Protocol.ID_CLASS_CLASS: {
                return ClassDescriptor.CLASS_DESCRIPTOR;
            }
            case Protocol.ID_ENUM_CLASS: {
                return ClassDescriptor.ENUM_DESCRIPTOR;
            }

            case Protocol.ID_BOOLEAN_ARRAY_CLASS: {
                return ClassDescriptor.BOOLEAN_ARRAY;
            }
            case Protocol.ID_BYTE_ARRAY_CLASS: {
                return ClassDescriptor.BYTE_ARRAY;
            }
            case Protocol.ID_SHORT_ARRAY_CLASS: {
                return ClassDescriptor.SHORT_ARRAY;
            }
            case Protocol.ID_INT_ARRAY_CLASS: {
                return ClassDescriptor.INT_ARRAY;
            }
            case Protocol.ID_LONG_ARRAY_CLASS: {
                return ClassDescriptor.LONG_ARRAY;
            }
            case Protocol.ID_CHAR_ARRAY_CLASS: {
                return ClassDescriptor.CHAR_ARRAY;
            }
            case Protocol.ID_FLOAT_ARRAY_CLASS: {
                return ClassDescriptor.FLOAT_ARRAY;
            }
            case Protocol.ID_DOUBLE_ARRAY_CLASS: {
                return ClassDescriptor.DOUBLE_ARRAY;
            }

            case Protocol.ID_PRIM_BOOLEAN: {
                return ClassDescriptor.BOOLEAN;
            }
            case Protocol.ID_PRIM_BYTE: {
                return ClassDescriptor.BYTE;
            }
            case Protocol.ID_PRIM_CHAR: {
                return ClassDescriptor.CHAR;
            }
            case Protocol.ID_PRIM_DOUBLE: {
                return ClassDescriptor.DOUBLE;
            }
            case Protocol.ID_PRIM_FLOAT: {
                return ClassDescriptor.FLOAT;
            }
            case Protocol.ID_PRIM_INT: {
                return ClassDescriptor.INT;
            }
            case Protocol.ID_PRIM_LONG: {
                return ClassDescriptor.LONG;
            }
            case Protocol.ID_PRIM_SHORT: {
                return ClassDescriptor.SHORT;
            }

            case Protocol.ID_VOID: {
                return ClassDescriptor.VOID;
            }

            case Protocol.ID_BOOLEAN_CLASS: {
                return ClassDescriptor.BOOLEAN_OBJ;
            }
            case Protocol.ID_BYTE_CLASS: {
                return ClassDescriptor.BYTE_OBJ;
            }
            case Protocol.ID_SHORT_CLASS: {
                return ClassDescriptor.SHORT_OBJ;
            }
            case Protocol.ID_INTEGER_CLASS: {
                return ClassDescriptor.INTEGER_OBJ;
            }
            case Protocol.ID_LONG_CLASS: {
                return ClassDescriptor.LONG_OBJ;
            }
            case Protocol.ID_CHARACTER_CLASS: {
                return ClassDescriptor.CHARACTER_OBJ;
            }
            case Protocol.ID_FLOAT_CLASS: {
                return ClassDescriptor.FLOAT_OBJ;
            }
            case Protocol.ID_DOUBLE_CLASS: {
                return ClassDescriptor.DOUBLE_OBJ;
            }

            case Protocol.ID_VOID_CLASS: {
                return ClassDescriptor.VOID_OBJ;
            }

            default: {
                throw new InvalidClassException("Unexpected class ID " + classType);
            }
        }
    }

    private Class<?> readClassTableClass() throws IOException, ClassNotFoundException {
        if (version == 1) {
            final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
            final Class<?> type = classTable.readClass(blockUnmarshaller);
            blockUnmarshaller.readToEndBlockData();
            blockUnmarshaller.unblock();
            return type;
        } else {
            return classTable.readClass(this);
        }
    }

    private Class<?> doResolveClass(final String className, final long uid) throws IOException, ClassNotFoundException {
        if (version == 1) {
            final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
            final Class<?> resolvedClass = classResolver.resolveClass(blockUnmarshaller, className, uid);
            blockUnmarshaller.readToEndBlockData();
            blockUnmarshaller.unblock();
            return resolvedClass;
        } else {
            return classResolver.resolveClass(this, className, uid);
        }
    }

    protected String readString() throws IOException {
        final int length = readInt();
        return UTFUtils.readUTFBytes(this, length);
    }

    private static final class DummyInvocationHandler implements InvocationHandler {
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            throw new NoSuchMethodError("Invocation handler not yet loaded");
        }
    }

    protected void doStart() throws IOException {
        super.doStart();
        if (configuredVersion > 0) {
            int version = readUnsignedByte();
            if (version > Protocol.MAX_VERSION) {
                throw new IOException("Unsupported protocol version " + version);
            }
            this.version = version;
        } else {
            version = 0;
        }
    }

    private static final InvocationHandler DUMMY_HANDLER = new DummyInvocationHandler();

    private static Object createProxyInstance(Creator creator, Class<?> type) throws IOException {
        try {
            return creator.create(type);
        } catch (Exception e) {
            return Proxy.newProxyInstance(type.getClassLoader(), type.getInterfaces(), DUMMY_HANDLER);
        }
    }

    protected Object doReadNewObject(final int streamClassType, final boolean unshared) throws ClassNotFoundException, IOException {
        final ClassDescriptor descriptor = doReadClassDescriptor(streamClassType);
        final int classType = descriptor.getTypeID();
        switch (classType) {
            case Protocol.ID_PROXY_CLASS: {
                final Class<?> type = descriptor.getType();
                final Object obj = createProxyInstance(creator, type);
                final int idx = instanceCache.size();
                instanceCache.add(obj);
                try {
                    proxyInvocationHandler.set(obj, doReadObject(unshared));
                } catch (IllegalAccessException e) {
                    throw new InvalidClassException(type.getName(), "Unable to set proxy invocation handler");
                }
                final Object resolvedObject = objectResolver.readResolve(obj);
                if (unshared) {
                    instanceCache.set(idx, null);
                } else if (obj != resolvedObject) {
                    instanceCache.set(idx, resolvedObject);
                }
                return resolvedObject;
            }
            case Protocol.ID_WRITE_OBJECT_CLASS:
            case Protocol.ID_SERIALIZABLE_CLASS: {
                final SerializableClassDescriptor serializableClassDescriptor = (SerializableClassDescriptor) descriptor;
                final Class<?> type = descriptor.getType();
                final SerializableClass serializableClass = serializableClassDescriptor.getSerializableClass();
                final Object obj = creator.create(type);
                final int idx = instanceCache.size();
                instanceCache.add(obj);
                doInitSerializable(obj, serializableClassDescriptor);
                final Object resolvedObject = objectResolver.readResolve(serializableClass.hasReadResolve() ? serializableClass.callReadResolve(obj) : obj);
                if (unshared) {
                    instanceCache.set(idx, null);
                } else if (obj != resolvedObject) {
                    instanceCache.set(idx, resolvedObject);
                }
                return resolvedObject;
            }
            case Protocol.ID_EXTERNALIZABLE_CLASS: {
                final Class<?> type = descriptor.getType();
                final SerializableClass serializableClass = registry.lookup(type);
                final Externalizable obj = (Externalizable) creator.create(type);
                final int idx = instanceCache.size();
                instanceCache.add(obj);
                if (version > 0) {
                    final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                    obj.readExternal(blockUnmarshaller);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else {
                    obj.readExternal(getObjectInput());
                }
                final Object resolvedObject = objectResolver.readResolve(serializableClass.hasReadResolve() ? serializableClass.callReadResolve(obj) : obj);
                if (unshared) {
                    instanceCache.set(idx, null);
                } else if (obj != resolvedObject) {
                    instanceCache.set(idx, resolvedObject);
                }
                return resolvedObject;
            }
            case Protocol.ID_EXTERNALIZER_CLASS: {
                final int idx = instanceCache.size();
                instanceCache.add(null);
                Externalizer externalizer = ((ExternalizerClassDescriptor) descriptor).getExternalizer();
                final Class<?> type = descriptor.getType();
                final SerializableClass serializableClass = registry.lookup(type);
                final Object obj;
                if (version > 0) {
                    final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                    obj = externalizer.createExternal(type, blockUnmarshaller, creator);
                    instanceCache.set(idx, obj);
                    externalizer.readExternal(obj, blockUnmarshaller);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else {
                    obj = externalizer.createExternal(type, this, creator);
                    instanceCache.set(idx, obj);
                    externalizer.readExternal(obj, getObjectInput());
                }
                final Object resolvedObject = objectResolver.readResolve(serializableClass.hasReadResolve() ? serializableClass.callReadResolve(obj) : obj);
                if (unshared) {
                    instanceCache.set(idx, null);
                } else if (obj != resolvedObject) {
                    instanceCache.set(idx, resolvedObject);
                }
                return resolvedObject;
            }
            case Protocol.ID_ENUM_TYPE_CLASS: {
                final String name = readString();
                final Enum obj = resolveEnumConstant(descriptor, name);
                final int idx = instanceCache.size();
                instanceCache.add(obj);
                final Object resolvedObject = objectResolver.readResolve(obj);
                if (unshared) {
                    instanceCache.set(idx, null);
                } else if (obj != resolvedObject) {
                    instanceCache.set(idx, resolvedObject);
                }
                return resolvedObject;
            }
            case Protocol.ID_OBJECT_ARRAY_TYPE_CLASS: {
                final int cnt = readInt();
                final Object[] array = (Object[]) Array.newInstance(descriptor.getType().getComponentType(), cnt);
                final int idx = instanceCache.size();
                instanceCache.add(array);
                for (int i = 0; i < cnt; i ++) {
                    array[i] = doReadObject(unshared);
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                if (unshared) {
                    instanceCache.set(idx, null);
                } else if (array != resolvedObject) {
                    instanceCache.set(idx, resolvedObject);
                }
                return resolvedObject;
            }
            case Protocol.ID_STRING_CLASS: {
                final String obj = readString();
                final Object resolvedObject = objectResolver.readResolve(obj);
                if (unshared) {
                    instanceCache.add(null);
                } else {
                    instanceCache.add(resolvedObject);
                }
                return resolvedObject;
            }
            case Protocol.ID_CLASS_CLASS: {
                final ClassDescriptor nestedDescriptor = doReadClassDescriptor(readUnsignedByte());
                // Classes are not resolved and may not be unshared!
                final Class<?> obj = nestedDescriptor.getType();
                return obj;
            }
            case Protocol.ID_BOOLEAN_ARRAY_CLASS: {
                final int cnt = readInt();
                final boolean[] array = new boolean[cnt];
                int v = 0;
                int bc = cnt & ~7;
                for (int i = 0; i < bc; ) {
                    v = readByte();
                    array[i++] = (v & 1) != 0;
                    array[i++] = (v & 2) != 0;
                    array[i++] = (v & 4) != 0;
                    array[i++] = (v & 8) != 0;
                    array[i++] = (v & 16) != 0;
                    array[i++] = (v & 32) != 0;
                    array[i++] = (v & 64) != 0;
                    array[i++] = (v & 128) != 0;
                }
                if (bc < cnt) {
                    v = readUnsignedByte();
                    int bit = 1;
                    for (int i = bc; i < cnt; i ++) {
                        array[i] = (v & bit) != 0;
                        bit <<= 1;
                    }
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_BYTE_ARRAY_CLASS: {
                final int cnt = readInt();
                final byte[] array = new byte[cnt];
                readFully(array, 0, array.length);
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_SHORT_ARRAY_CLASS: {
                final int cnt = readInt();
                final short[] array = new short[cnt];
                for (int i = 0; i < cnt; i ++) {
                    array[i] = readShort();
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_INT_ARRAY_CLASS: {
                final int cnt = readInt();
                final int[] array = new int[cnt];
                for (int i = 0; i < cnt; i ++) {
                    array[i] = readInt();
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_LONG_ARRAY_CLASS: {
                final int cnt = readInt();
                final long[] array = new long[cnt];
                for (int i = 0; i < cnt; i ++) {
                    array[i] = readLong();
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_CHAR_ARRAY_CLASS: {
                final int cnt = readInt();
                final char[] array = new char[cnt];
                for (int i = 0; i < cnt; i ++) {
                    array[i] = readChar();
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_FLOAT_ARRAY_CLASS: {
                final int cnt = readInt();
                final float[] array = new float[cnt];
                for (int i = 0; i < cnt; i ++) {
                    array[i] = readFloat();
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_DOUBLE_ARRAY_CLASS: {
                final int cnt = readInt();
                final double[] array = new double[cnt];
                for (int i = 0; i < cnt; i ++) {
                    array[i] = readDouble();
                }
                final Object resolvedObject = objectResolver.readResolve(array);
                instanceCache.add(unshared ? null : resolvedObject);
                return resolvedObject;
            }
            case Protocol.ID_BOOLEAN_CLASS: {
                return objectResolver.readResolve(Boolean.valueOf(readBoolean()));
            }
            case Protocol.ID_BYTE_CLASS: {
                return objectResolver.readResolve(Byte.valueOf(readByte()));
            }
            case Protocol.ID_SHORT_CLASS: {
                return objectResolver.readResolve(Short.valueOf(readShort()));
            }
            case Protocol.ID_INTEGER_CLASS: {
                return objectResolver.readResolve(Integer.valueOf(readInt()));
            }
            case Protocol.ID_LONG_CLASS: {
                return objectResolver.readResolve(Long.valueOf(readLong()));
            }
            case Protocol.ID_CHARACTER_CLASS: {
                return objectResolver.readResolve(Character.valueOf(readChar()));
            }
            case Protocol.ID_FLOAT_CLASS: {
                return objectResolver.readResolve(Float.valueOf(readFloat()));
            }
            case Protocol.ID_DOUBLE_CLASS: {
                return objectResolver.readResolve(Double.valueOf(readDouble()));
            }
            case Protocol.ID_OBJECT_CLASS:
            case Protocol.ID_PLAIN_CLASS: {
                throw new NotSerializableException("(remote)" + descriptor.getType().getName());
            }
            default: {
                throw new InvalidObjectException("Unexpected class type " + classType);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private static Enum resolveEnumConstant(final ClassDescriptor descriptor, final String name) {
        return Enum.valueOf((Class<? extends Enum>)descriptor.getType(), name);
    }

    private void doInitSerializable(final Object obj, final SerializableClassDescriptor descriptor) throws IOException, ClassNotFoundException {
        final Class<?> type = descriptor.getType();
        final SerializableClass info = registry.lookup(type);
        final ClassDescriptor superDescriptor = descriptor.getSuperClassDescriptor();
        if (superDescriptor instanceof SerializableClassDescriptor) {
            final SerializableClassDescriptor serializableSuperDescriptor = (SerializableClassDescriptor) superDescriptor;
            doInitSerializable(obj, serializableSuperDescriptor);
        }
        final int typeId = descriptor.getTypeID();
        if (descriptor.isGap()) {
            if (info.hasReadObjectNoData()) {
                info.callReadObjectNoData(obj);
            }
        } else if (info.hasReadObject()) {
            final RiverObjectInputStream objectInputStream = getObjectInputStream();
            final SerializableClassDescriptor oldDescriptor = objectInputStream.swapClass(descriptor);
            final Object oldObj = objectInputStream.swapCurrent(obj);
            final RiverObjectInputStream.State restoreState = objectInputStream.start();
            boolean ok = false;
            try {
                if (typeId == Protocol.ID_WRITE_OBJECT_CLASS) {
                    // protocol version >= 1; read fields
                    info.callReadObject(obj, objectInputStream);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else if (version >= 1) {
                    // protocol version >= 1 but no fields to read
                    blockUnmarshaller.endOfStream();
                    info.callReadObject(obj, objectInputStream);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else {
                    // protocol version 0
                    info.callReadObject(obj, objectInputStream);
                }
                objectInputStream.finish(restoreState);
                objectInputStream.swapCurrent(oldObj);
                objectInputStream.swapClass(oldDescriptor);
                ok = true;
            } finally {
                if (! ok) {
                    objectInputStream.fullReset();
                }
            }
        } else {
            readFields(obj, descriptor);
            if (typeId == Protocol.ID_WRITE_OBJECT_CLASS) {
                // protocol version >= 1 with useless user data
                blockUnmarshaller.readToEndBlockData();
                blockUnmarshaller.unblock();
            }
        }
    }

    protected void readFields(final Object obj, final SerializableClassDescriptor descriptor) throws IOException, ClassNotFoundException {
        for (SerializableField serializableField : descriptor.getFields()) {
            final Field field = serializableField.getField();
            if (field == null) {
                // missing; consume stream data only
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        readBoolean();
                        break;
                    }
                    case BYTE: {
                        readByte();
                        break;
                    }
                    case CHAR: {
                        readChar();
                        break;
                    }
                    case DOUBLE: {
                        readDouble();
                        break;
                    }
                    case FLOAT: {
                        readFloat();
                        break;
                    }
                    case INT: {
                        readInt();
                        break;
                    }
                    case LONG: {
                        readLong();
                        break;
                    }
                    case OBJECT: {
                        if (serializableField.isUnshared()) {
                            readObjectUnshared();
                        } else {
                            readObject();
                        }
                        break;
                    }
                    case SHORT: {
                        readShort();
                        break;
                    }
                }
            } else try {
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        field.setBoolean(obj, readBoolean());
                        break;
                    }
                    case BYTE: {
                        field.setByte(obj, readByte());
                        break;
                    }
                    case CHAR: {
                        field.setChar(obj, readChar());
                        break;
                    }
                    case DOUBLE: {
                        field.setDouble(obj, readDouble());
                        break;
                    }
                    case FLOAT: {
                        field.setFloat(obj, readFloat());
                        break;
                    }
                    case INT: {
                        field.setInt(obj, readInt());
                        break;
                    }
                    case LONG: {
                        field.setLong(obj, readLong());
                        break;
                    }
                    case OBJECT: {
                        final Object robj;
                        if (serializableField.isUnshared()) {
                            robj = readObjectUnshared();
                        } else {
                            robj = readObject();
                        }
                        field.set(obj, robj);
                        break;
                    }
                    case SHORT: {
                        field.setShort(obj, readShort());
                        break;
                    }
                }
            } catch (IllegalAccessException e) {
                final InvalidObjectException ioe = new InvalidObjectException("Unable to set a field");
                ioe.initCause(e);
                throw ioe;
            }
        }
    }

    void addValidation(final ObjectInputValidation validation, final int prio) {
        final Set<ObjectInputValidation> validations;
        final Integer prioKey = Integer.valueOf(prio);
        if (validationMap.containsKey(prioKey)) {
            validations = validationMap.get(prioKey);
        } else {
            validations = new HashSet<ObjectInputValidation>();
            validationMap.put(prioKey, validations);
        }
        validations.add(validation);
    }

    public String readUTF() throws IOException {
        final int len = readInt();
        return UTFUtils.readUTFBytes(this, len);
    }
}
