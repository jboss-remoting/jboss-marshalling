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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractQueue;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.Vector;
import java.util.Stack;
import org.jboss.marshalling.AbstractUnmarshaller;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.TraceInformation;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;
import static org.jboss.marshalling.river.Protocol.*;

/**
 *
 */
public class RiverUnmarshaller extends AbstractUnmarshaller {
    private final ArrayList<Object> instanceCache;
    private final ArrayList<ClassDescriptor> classCache;
    private final SerializableClassRegistry registry;
    private int version;
    private int depth;
    private BlockUnmarshaller blockUnmarshaller;
    private RiverObjectInputStream objectInputStream;
    private SortedSet<Validator> validators;
    private int validatorSeq;

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
        objectInputStream = null;
    }

    private BlockUnmarshaller getBlockUnmarshaller() {
        final BlockUnmarshaller blockUnmarshaller = this.blockUnmarshaller;
        return blockUnmarshaller == null ? this.blockUnmarshaller = new BlockUnmarshaller(this) : blockUnmarshaller;
    }

    private final PrivilegedExceptionAction<RiverObjectInputStream> createObjectInputStreamAction = new PrivilegedExceptionAction<RiverObjectInputStream>() {
        public RiverObjectInputStream run() throws IOException {
            return new RiverObjectInputStream(RiverUnmarshaller.this, getBlockUnmarshaller());
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

    Object doReadNestedObject(final boolean unshared, final String enclosingClassName) throws ClassNotFoundException, IOException {
        try {
            return doReadObject(unshared);
        } catch (IOException e) {
            TraceInformation.addIncompleteObjectInformation(e, enclosingClassName);
            throw e;
        } catch (ClassNotFoundException e) {
            TraceInformation.addIncompleteObjectInformation(e, enclosingClassName);
            throw e;
        } catch (RuntimeException e) {
            TraceInformation.addIncompleteObjectInformation(e, enclosingClassName);
            throw e;
        }
    }

    Object doReadCollectionObject(final boolean unshared, final int idx, final int size) throws ClassNotFoundException, IOException {
        try {
            return doReadObject(unshared);
        } catch (IOException e) {
            TraceInformation.addIndexInformation(e, idx, size, TraceInformation.IndexType.ELEMENT);
            throw e;
        } catch (ClassNotFoundException e) {
            TraceInformation.addIndexInformation(e, idx, size, TraceInformation.IndexType.ELEMENT);
            throw e;
        } catch (RuntimeException e) {
            TraceInformation.addIndexInformation(e, idx, size, TraceInformation.IndexType.ELEMENT);
            throw e;
        }
    }

    Object doReadMapObject(final boolean unshared, final int idx, final int size, final boolean key) throws ClassNotFoundException, IOException {
        try {
            return doReadObject(unshared);
        } catch (IOException e) {
            TraceInformation.addIndexInformation(e, idx, size, key ? TraceInformation.IndexType.MAP_KEY : TraceInformation.IndexType.MAP_VALUE);
            throw e;
        } catch (ClassNotFoundException e) {
            TraceInformation.addIndexInformation(e, idx, size, key ? TraceInformation.IndexType.MAP_KEY : TraceInformation.IndexType.MAP_VALUE);
            throw e;
        } catch (RuntimeException e) {
            TraceInformation.addIndexInformation(e, idx, size, key ? TraceInformation.IndexType.MAP_KEY : TraceInformation.IndexType.MAP_VALUE);
            throw e;
        }
    }

    protected Object doReadObject(final boolean unshared) throws ClassNotFoundException, IOException {
        final Object obj = doReadObject(readUnsignedByte(), unshared);
        if (depth == 0) {
            final SortedSet<Validator> validators = this.validators;
            if (validators != null) {
                this.validators = null;
                validatorSeq = 0;
                for (Validator validator : validators) {
                    validator.getValidation().validateObject();
                }
            }
        }
        return obj;
    }

    @SuppressWarnings({ "unchecked" })
    Object doReadObject(int leadByte, final boolean unshared) throws IOException, ClassNotFoundException {
        depth ++;
        try {
            for (;;) switch (leadByte) {
                case ID_NULL: {
                    return null;
                }
                case ID_REPEAT_OBJECT_FAR: {
                    if (unshared) {
                        throw new InvalidObjectException("Attempt to read a backreference as unshared");
                    }
                    final int index = readInt();
                    try {
                        final Object obj = instanceCache.get(index);
                        if (obj != null) return obj;
                    } catch (IndexOutOfBoundsException e) {
                    }
                    throw new InvalidObjectException("Attempt to read a backreference with an invalid ID (absolute " + index + ")");
                }
                case ID_REPEAT_OBJECT_NEAR: {
                    if (unshared) {
                        throw new InvalidObjectException("Attempt to read a backreference as unshared");
                    }
                    final int index = readByte() | 0xffffff00;
                    try {
                        final Object obj = instanceCache.get(index + instanceCache.size());
                        if (obj != null) return obj;
                    } catch (IndexOutOfBoundsException e) {
                    }
                    throw new InvalidObjectException("Attempt to read a backreference with an invalid ID (relative near " + index + ")");
                }
                case ID_REPEAT_OBJECT_NEARISH: {
                    if (unshared) {
                        throw new InvalidObjectException("Attempt to read a backreference as unshared");
                    }
                    final int index = readShort() | 0xffff0000;
                    try {
                        final Object obj = instanceCache.get(index + instanceCache.size());
                        if (obj != null) return obj;
                    } catch (IndexOutOfBoundsException e) {
                    }
                    throw new InvalidObjectException("Attempt to read a backreference with an invalid ID (relative nearish " + index + ")");
                }
                case ID_NEW_OBJECT:
                case ID_NEW_OBJECT_UNSHARED: {
                    if (unshared != (leadByte == ID_NEW_OBJECT_UNSHARED)) {
                        throw sharedMismatch();
                    }
                    return doReadNewObject(readUnsignedByte(), unshared);
                }
                // v2 string types
                case ID_STRING_EMPTY: {
                    return "";
                }
                case ID_STRING_SMALL: {
                    // ignore unshared setting
                    int length = readUnsignedByte();
                    final String s = UTFUtils.readUTFBytes(this, length == 0 ? 0x100 : length);
                    instanceCache.add(s);
                    return s;
                }
                case ID_STRING_MEDIUM: {
                    // ignore unshared setting
                    int length = readUnsignedShort();
                    final String s = UTFUtils.readUTFBytes(this, length == 0 ? 0x10000 : length);
                    instanceCache.add(s);
                    return s;
                }
                case ID_STRING_LARGE: {
                    // ignore unshared setting
                    int length = readInt();
                    if (length <= 0) {
                        throw new StreamCorruptedException("Invalid length value for string in stream (" + length + ")");
                    }
                    final String s = UTFUtils.readUTFBytes(this, length);
                    instanceCache.add(s);
                    return s;
                }
                case ID_ARRAY_EMPTY:
                case ID_ARRAY_EMPTY_UNSHARED: {
                    if (unshared != (leadByte == ID_ARRAY_EMPTY_UNSHARED)) {
                        throw sharedMismatch();
                    }
                    final ArrayList<Object> instanceCache = this.instanceCache;
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Array.newInstance(doReadClassDescriptor(readUnsignedByte()).getType(), 0);
                    instanceCache.set(idx, obj);
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (unshared) {
                        instanceCache.set(idx, null);
                    } else if (obj != resolvedObject) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return obj;
                }
                case ID_ARRAY_SMALL:
                case ID_ARRAY_SMALL_UNSHARED: {
                    if (unshared != (leadByte == ID_ARRAY_SMALL_UNSHARED)) {
                        throw sharedMismatch();
                    }
                    final int len = readUnsignedByte();
                    return doReadArray(len == 0 ? 0x100 : len, unshared);
                }
                case ID_ARRAY_MEDIUM:
                case ID_ARRAY_MEDIUM_UNSHARED: {
                    if (unshared != (leadByte == ID_ARRAY_MEDIUM_UNSHARED)) {
                        throw sharedMismatch();
                    }
                    final int len = readUnsignedShort();
                    return doReadArray(len == 0 ? 0x10000 : len, unshared);
                }
                case ID_ARRAY_LARGE:
                case ID_ARRAY_LARGE_UNSHARED: {
                    if (unshared != (leadByte == ID_ARRAY_LARGE_UNSHARED)) {
                        throw sharedMismatch();
                    }
                    final int len = readInt();
                    if (len <= 0) {
                        throw new StreamCorruptedException("Invalid length value for array in stream (" + len + ")");
                    }
                    return doReadArray(len, unshared);
                }
                case ID_PREDEFINED_OBJECT: {
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
                        // v2 does not require a block for this
                        return objectTable.readObject(this);
                    }
                }
                case ID_BOOLEAN_OBJECT_TRUE: {
                    return objectResolver.readResolve(Boolean.TRUE);
                }
                case ID_BOOLEAN_OBJECT_FALSE: {
                    return objectResolver.readResolve(Boolean.FALSE);
                }
                case ID_BYTE_OBJECT: {
                    return objectResolver.readResolve(Byte.valueOf(readByte()));
                }
                case ID_SHORT_OBJECT: {
                    return objectResolver.readResolve(Short.valueOf(readShort()));
                }
                case ID_INTEGER_OBJECT: {
                    return objectResolver.readResolve(Integer.valueOf(readInt()));
                }
                case ID_LONG_OBJECT: {
                    return objectResolver.readResolve(Long.valueOf(readLong()));
                }
                case ID_FLOAT_OBJECT: {
                    return objectResolver.readResolve(Float.valueOf(readFloat()));
                }
                case ID_DOUBLE_OBJECT: {
                    return objectResolver.readResolve(Double.valueOf(readDouble()));
                }
                case ID_CHARACTER_OBJECT: {
                    return objectResolver.readResolve(Character.valueOf(readChar()));
                }
                case ID_PRIM_BYTE: {
                    return byte.class;
                }
                case ID_PRIM_BOOLEAN: {
                    return boolean.class;
                }
                case ID_PRIM_CHAR: {
                    return char.class;
                }
                case ID_PRIM_DOUBLE: {
                    return double.class;
                }
                case ID_PRIM_FLOAT: {
                    return float.class;
                }
                case ID_PRIM_INT: {
                    return int.class;
                }
                case ID_PRIM_LONG: {
                    return long.class;
                }
                case ID_PRIM_SHORT: {
                    return short.class;
                }

                case ID_VOID: {
                    return void.class;
                }

                case ID_BYTE_CLASS: {
                    return Byte.class;
                }
                case ID_BOOLEAN_CLASS: {
                    return Boolean.class;
                }
                case ID_CHARACTER_CLASS: {
                    return Character.class;
                }
                case ID_DOUBLE_CLASS: {
                    return Double.class;
                }
                case ID_FLOAT_CLASS: {
                    return Float.class;
                }
                case ID_INTEGER_CLASS: {
                    return Integer.class;
                }
                case ID_LONG_CLASS: {
                    return Long.class;
                }
                case ID_SHORT_CLASS: {
                    return Short.class;
                }

                case ID_VOID_CLASS: {
                    return Void.class;
                }

                case ID_OBJECT_CLASS: {
                    return Object.class;
                }
                case ID_CLASS_CLASS: {
                    return Class.class;
                }
                case ID_STRING_CLASS: {
                    return String.class;
                }
                case ID_ENUM_CLASS: {
                    return Enum.class;
                }

                case ID_BYTE_ARRAY_CLASS: {
                    return byte[].class;
                }
                case ID_BOOLEAN_ARRAY_CLASS: {
                    return boolean[].class;
                }
                case ID_CHAR_ARRAY_CLASS: {
                    return char[].class;
                }
                case ID_DOUBLE_ARRAY_CLASS: {
                    return double[].class;
                }
                case ID_FLOAT_ARRAY_CLASS: {
                    return float[].class;
                }
                case ID_INT_ARRAY_CLASS: {
                    return int[].class;
                }
                case ID_LONG_ARRAY_CLASS: {
                    return long[].class;
                }
                case ID_SHORT_ARRAY_CLASS: {
                    return short[].class;
                }

                case ID_CC_ARRAY_LIST: {
                    return ArrayList.class;
                }
                case ID_CC_HASH_MAP: {
                    return HashMap.class;
                }
                case ID_CC_HASH_SET: {
                    return HashSet.class;
                }
                case ID_CC_HASHTABLE: {
                    return Hashtable.class;
                }
                case ID_CC_IDENTITY_HASH_MAP: {
                    return HashMap.class;
                }
                case ID_CC_LINKED_HASH_MAP: {
                    return LinkedHashMap.class;
                }
                case ID_CC_LINKED_HASH_SET: {
                    return LinkedHashSet.class;
                }
                case ID_CC_LINKED_LIST: {
                    return LinkedList.class;
                }
                case ID_CC_TREE_MAP: {
                    return TreeMap.class;
                }
                case ID_CC_TREE_SET: {
                    return TreeSet.class;
                }
                case ID_ABSTRACT_COLLECTION: {
                    return AbstractCollection.class;
                }
                case ID_ABSTRACT_LIST: {
                    return AbstractList.class;
                }
                case ID_ABSTRACT_QUEUE: {
                    return AbstractQueue.class;
                }
                case ID_ABSTRACT_SEQUENTIAL_LIST: {
                    return AbstractSequentialList.class;
                }
                case ID_ABSTRACT_SET: {
                    return AbstractSet.class;
                }

                case ID_SINGLETON_LIST_OBJECT: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Collections.singletonList(doReadNestedObject(false, "Collections#singletonList()"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return resolvedObject;
                }
                case ID_SINGLETON_SET_OBJECT: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Collections.singleton(doReadNestedObject(false, "Collections#singleton()"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return resolvedObject;
                }
                case ID_SINGLETON_MAP_OBJECT: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Collections.singletonMap(doReadNestedObject(false, "Collections#singletonMap() [key]"), doReadNestedObject(false, "Collections#singletonMap() [value]"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return resolvedObject;
                }

                case ID_EMPTY_LIST_OBJECT: {
                    return Collections.emptyList();
                }
                case ID_EMPTY_SET_OBJECT: {
                    return Collections.emptySet();
                }
                case ID_EMPTY_MAP_OBJECT: {
                    return Collections.emptyMap();
                }

                case ID_COLLECTION_EMPTY:
                case ID_COLLECTION_EMPTY_UNSHARED:
                case ID_COLLECTION_SMALL:
                case ID_COLLECTION_SMALL_UNSHARED:
                case ID_COLLECTION_MEDIUM:
                case ID_COLLECTION_MEDIUM_UNSHARED:
                case ID_COLLECTION_LARGE:
                case ID_COLLECTION_LARGE_UNSHARED:
                {
                    final int len;
                    switch (leadByte) {
                        case ID_COLLECTION_EMPTY:
                        case ID_COLLECTION_EMPTY_UNSHARED: {
                            len = 0;
                            break;
                        }
                        case ID_COLLECTION_SMALL:
                        case ID_COLLECTION_SMALL_UNSHARED: {
                            int b = readUnsignedByte();
                            len = b == 0 ? 0x100 : b;
                            break;
                        }
                        case ID_COLLECTION_MEDIUM:
                        case ID_COLLECTION_MEDIUM_UNSHARED: {
                            int b = readUnsignedShort();
                            len = b == 0 ? 0x10000 : b;
                            break;
                        }
                        case ID_COLLECTION_LARGE:
                        case ID_COLLECTION_LARGE_UNSHARED: {
                            len = readInt();
                            break;
                        }
                        default: {
                            throw new IllegalStateException();
                        }
                    }
                    final int id = readUnsignedByte();
                    switch (id) {
                        case ID_CC_ARRAY_LIST: {
                            return readCollectionData(unshared, len, new ArrayList(len));
                        }
                        case ID_CC_HASH_SET: {
                            return readCollectionData(unshared, len, new HashSet(len));
                        }
                        case ID_CC_LINKED_HASH_SET: {
                            return readCollectionData(unshared, len, new LinkedHashSet(len));
                        }
                        case ID_CC_LINKED_LIST: {
                            return readCollectionData(unshared, len, new LinkedList());
                        }
                        case ID_CC_TREE_SET: {
                            return readCollectionData(unshared, len, new TreeSet((Comparator)doReadNestedObject(false, "java.util.TreeSet comparator")));
                        }
                        case ID_CC_ENUM_SET_PROXY: {
                            final ClassDescriptor nestedDescriptor = doReadClassDescriptor(readUnsignedByte());
                            final Class<? extends Enum> elementType = nestedDescriptor.getType().asSubclass(Enum.class);
                            return readCollectionData(unshared, len, EnumSet.noneOf(elementType));
                        }
                        case ID_CC_VECTOR: {
                            return readCollectionData(unshared, len, new Vector(len));
                        }
                        case ID_CC_STACK: {
                            return readCollectionData(unshared, len, new Stack());
                        }

                        case ID_CC_HASH_MAP: {
                            return readMapData(unshared, len, new HashMap(len));
                        }
                        case ID_CC_HASHTABLE: {
                            return readMapData(unshared, len, new Hashtable(len));
                        }
                        case ID_CC_IDENTITY_HASH_MAP: {
                            return readMapData(unshared, len, new IdentityHashMap(len));
                        }
                        case ID_CC_LINKED_HASH_MAP: {
                            return readMapData(unshared, len, new LinkedHashMap(len));
                        }
                        case ID_CC_TREE_MAP: {
                            return readMapData(unshared, len, new TreeMap((Comparator)doReadNestedObject(false, "java.util.TreeMap comparator")));
                        }
                        case ID_CC_ENUM_MAP: {
                            final ClassDescriptor nestedDescriptor = doReadClassDescriptor(readUnsignedByte());
                            final Class<? extends Enum> elementType = nestedDescriptor.getType().asSubclass(Enum.class);
                            return readMapData(unshared, len, new EnumMap(elementType));
                        }
                        default: {
                            throw new StreamCorruptedException("Unexpected byte found when reading a collection type: " + leadByte);
                        }
                    }
                }

                case ID_CLEAR_CLASS_CACHE: {
                    if (depth > 1) {
                        throw new StreamCorruptedException("ID_CLEAR_CLASS_CACHE token in the middle of stream processing");
                    }
                    classCache.clear();
                    instanceCache.clear();
                    leadByte = readUnsignedByte();
                    continue;
                }
                case ID_CLEAR_INSTANCE_CACHE: {
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

    @SuppressWarnings({ "unchecked" })
    private Object readCollectionData(final boolean unshared, final int len, final Collection target) throws ClassNotFoundException, IOException {
        final ArrayList<Object> instanceCache = this.instanceCache;
        final int idx;
        idx = instanceCache.size();
        instanceCache.add(target);
        for (int i = 0; i < len; i ++) {
            target.add(doReadCollectionObject(false, idx, len));
        }
        final Object resolvedObject = objectResolver.readResolve(target);
        if (unshared) {
            instanceCache.set(idx, null);
        } else if (target != resolvedObject) {
            instanceCache.set(idx, resolvedObject);
        }
        return resolvedObject;
    }

    @SuppressWarnings({ "unchecked" })
    private Object readMapData(final boolean unshared, final int len, final Map target) throws ClassNotFoundException, IOException {
        final ArrayList<Object> instanceCache = this.instanceCache;
        final int idx;
        idx = instanceCache.size();
        instanceCache.add(target);
        for (int i = 0; i < len; i ++) {
            target.put(doReadMapObject(false, idx, len, true), doReadMapObject(false, idx, len, false));
        }
        final Object resolvedObject = objectResolver.readResolve(target);
        if (unshared) {
            instanceCache.set(idx, null);
        } else if (target != resolvedObject) {
            instanceCache.set(idx, resolvedObject);
        }
        return resolvedObject;
    }

    private static InvalidObjectException sharedMismatch() {
        return new InvalidObjectException("Shared/unshared object mismatch");
    }

    ClassDescriptor doReadClassDescriptor(final int classType) throws IOException, ClassNotFoundException {
        final ArrayList<ClassDescriptor> classCache = this.classCache;
        switch (classType) {
            case ID_REPEAT_CLASS_FAR: {
                return classCache.get(readInt());
            }
            case ID_REPEAT_CLASS_NEAR: {
                return classCache.get((readByte() | 0xffffff00) + classCache.size());
            }
            case ID_REPEAT_CLASS_NEARISH: {
                return classCache.get((readShort() | 0xffff0000) + classCache.size());
            }
            case ID_PREDEFINED_ENUM_TYPE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, ID_ENUM_TYPE_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_EXTERNALIZABLE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, ID_EXTERNALIZABLE_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_EXTERNALIZER_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final Externalizer externalizer = (Externalizer) readObject();
                final ClassDescriptor descriptor = new ExternalizerClassDescriptor(type, externalizer);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_PLAIN_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, ID_PLAIN_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_PROXY_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final ClassDescriptor descriptor = new ClassDescriptor(type, ID_PROXY_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_SERIALIZABLE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = readClassTableClass();
                final SerializableClass serializableClass = registry.lookup(type);
                int descType = serializableClass.hasWriteObject() ? ID_WRITE_OBJECT_CLASS : ID_SERIALIZABLE_CLASS;
                final ClassDescriptor descriptor = new SerializableClassDescriptor(serializableClass, doReadClassDescriptor(readUnsignedByte()), serializableClass.getFields(), descType);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PLAIN_CLASS: {
                final String className = readString();
                final Class<?> clazz = doResolveClass(className, 0L);
                final ClassDescriptor descriptor = new ClassDescriptor(clazz, ID_PLAIN_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case ID_PROXY_CLASS: {
                String[] interfaces = new String[readInt()];
                for (int i = 0; i < interfaces.length; i ++) {
                    interfaces[i] = readString();
                }
                final ClassDescriptor descriptor;
                if (version == 1) {
                    final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                    descriptor = new ClassDescriptor(classResolver.resolveProxyClass(blockUnmarshaller, interfaces), ID_PROXY_CLASS);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else {
                    // v2 does not require a block for this
                    descriptor = new ClassDescriptor(classResolver.resolveProxyClass(this, interfaces), ID_PROXY_CLASS);
                }
                classCache.add(descriptor);
                return descriptor;
            }
            case ID_WRITE_OBJECT_CLASS:
            case ID_SERIALIZABLE_CLASS: {
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
            case ID_EXTERNALIZABLE_CLASS: {
                final String className = readString();
                final long uid = readLong();
                final Class<?> clazz = doResolveClass(className, uid);
                final ClassDescriptor descriptor = new ClassDescriptor(clazz, ID_EXTERNALIZABLE_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case ID_EXTERNALIZER_CLASS: {
                final String className = readString();
                int idx = classCache.size();
                classCache.add(null);
                final Class<?> clazz = doResolveClass(className, 0L);
                final Externalizer externalizer = (Externalizer) readObject();
                final ClassDescriptor descriptor = new ExternalizerClassDescriptor(clazz, externalizer);
                classCache.set(idx, descriptor);
                return descriptor;
            }

            case ID_ENUM_TYPE_CLASS: {
                final ClassDescriptor descriptor = new ClassDescriptor(doResolveClass(readString(), 0L), ID_ENUM_TYPE_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case ID_OBJECT_ARRAY_TYPE_CLASS: {
                final ClassDescriptor elementType = doReadClassDescriptor(readUnsignedByte());
                final ClassDescriptor arrayDescriptor = new ClassDescriptor(Array.newInstance(elementType.getType(), 0).getClass(), ID_OBJECT_ARRAY_TYPE_CLASS);
                classCache.add(arrayDescriptor);
                return arrayDescriptor;
            }

            case ID_CC_ARRAY_LIST: {
                return ClassDescriptor.CC_ARRAY_LIST;
            }
            case ID_CC_LINKED_LIST: {
                return ClassDescriptor.CC_LINKED_LIST;
            }

            case ID_CC_HASH_SET: {
                return ClassDescriptor.CC_HASH_SET;
            }
            case ID_CC_LINKED_HASH_SET: {
                return ClassDescriptor.CC_LINKED_HASH_SET;
            }
            case ID_CC_TREE_SET: {
                return ClassDescriptor.CC_TREE_SET;
            }

            case ID_CC_IDENTITY_HASH_MAP: {
                return ClassDescriptor.CC_IDENTITY_HASH_MAP;
            }
            case ID_CC_HASH_MAP: {
                return ClassDescriptor.CC_HASH_MAP;
            }
            case ID_CC_HASHTABLE: {
                return ClassDescriptor.CC_HASHTABLE;
            }
            case ID_CC_LINKED_HASH_MAP: {
                return ClassDescriptor.CC_LINKED_HASH_MAP;
            }
            case ID_CC_TREE_MAP: {
                return ClassDescriptor.CC_TREE_MAP;
            }

            case ID_CC_ENUM_SET: {
                return ClassDescriptor.CC_ENUM_SET;
            }
            case ID_CC_ENUM_MAP: {
                return ClassDescriptor.CC_ENUM_MAP;
            }

            case ID_ABSTRACT_COLLECTION: {
                return ClassDescriptor.ABSTRACT_COLLECTION;
            }
            case ID_ABSTRACT_LIST: {
                return ClassDescriptor.ABSTRACT_LIST;
            }
            case ID_ABSTRACT_QUEUE: {
                return ClassDescriptor.ABSTRACT_QUEUE;
            }
            case ID_ABSTRACT_SEQUENTIAL_LIST: {
                return ClassDescriptor.ABSTRACT_SEQUENTIAL_LIST;
            }
            case ID_ABSTRACT_SET: {
                return ClassDescriptor.ABSTRACT_SET;
            }

            case ID_CC_CONCURRENT_HASH_MAP: {
                return ClassDescriptor.CONCURRENT_HASH_MAP;
            }
            case ID_CC_COPY_ON_WRITE_ARRAY_LIST: {
                return ClassDescriptor.COPY_ON_WRITE_ARRAY_LIST;
            }
            case ID_CC_COPY_ON_WRITE_ARRAY_SET: {
                return ClassDescriptor.COPY_ON_WRITE_ARRAY_SET;
            }
            case ID_CC_VECTOR: {
                return ClassDescriptor.VECTOR;
            }
            case ID_CC_STACK: {
                return ClassDescriptor.STACK;
            }

            case ID_STRING_CLASS: {
                return ClassDescriptor.STRING_DESCRIPTOR;
            }
            case ID_OBJECT_CLASS: {
                return ClassDescriptor.OBJECT_DESCRIPTOR;
            }
            case ID_CLASS_CLASS: {
                return ClassDescriptor.CLASS_DESCRIPTOR;
            }
            case ID_ENUM_CLASS: {
                return ClassDescriptor.ENUM_DESCRIPTOR;
            }

            case ID_BOOLEAN_ARRAY_CLASS: {
                return ClassDescriptor.BOOLEAN_ARRAY;
            }
            case ID_BYTE_ARRAY_CLASS: {
                return ClassDescriptor.BYTE_ARRAY;
            }
            case ID_SHORT_ARRAY_CLASS: {
                return ClassDescriptor.SHORT_ARRAY;
            }
            case ID_INT_ARRAY_CLASS: {
                return ClassDescriptor.INT_ARRAY;
            }
            case ID_LONG_ARRAY_CLASS: {
                return ClassDescriptor.LONG_ARRAY;
            }
            case ID_CHAR_ARRAY_CLASS: {
                return ClassDescriptor.CHAR_ARRAY;
            }
            case ID_FLOAT_ARRAY_CLASS: {
                return ClassDescriptor.FLOAT_ARRAY;
            }
            case ID_DOUBLE_ARRAY_CLASS: {
                return ClassDescriptor.DOUBLE_ARRAY;
            }

            case ID_PRIM_BOOLEAN: {
                return ClassDescriptor.BOOLEAN;
            }
            case ID_PRIM_BYTE: {
                return ClassDescriptor.BYTE;
            }
            case ID_PRIM_CHAR: {
                return ClassDescriptor.CHAR;
            }
            case ID_PRIM_DOUBLE: {
                return ClassDescriptor.DOUBLE;
            }
            case ID_PRIM_FLOAT: {
                return ClassDescriptor.FLOAT;
            }
            case ID_PRIM_INT: {
                return ClassDescriptor.INT;
            }
            case ID_PRIM_LONG: {
                return ClassDescriptor.LONG;
            }
            case ID_PRIM_SHORT: {
                return ClassDescriptor.SHORT;
            }

            case ID_VOID: {
                return ClassDescriptor.VOID;
            }

            case ID_BOOLEAN_CLASS: {
                return ClassDescriptor.BOOLEAN_OBJ;
            }
            case ID_BYTE_CLASS: {
                return ClassDescriptor.BYTE_OBJ;
            }
            case ID_SHORT_CLASS: {
                return ClassDescriptor.SHORT_OBJ;
            }
            case ID_INTEGER_CLASS: {
                return ClassDescriptor.INTEGER_OBJ;
            }
            case ID_LONG_CLASS: {
                return ClassDescriptor.LONG_OBJ;
            }
            case ID_CHARACTER_CLASS: {
                return ClassDescriptor.CHARACTER_OBJ;
            }
            case ID_FLOAT_CLASS: {
                return ClassDescriptor.FLOAT_OBJ;
            }
            case ID_DOUBLE_CLASS: {
                return ClassDescriptor.DOUBLE_OBJ;
            }

            case ID_VOID_CLASS: {
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
            // v2 does not require a block for this
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
            // v2 does not require a block for this
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
        int version = readUnsignedByte();
        if (version < MIN_VERSION || version > configuredVersion || version > MAX_VERSION) {
            throw new IOException("Unsupported protocol version " + version);
        }
        this.version = version;
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
        try {
            final int classType = descriptor.getTypeID();
            final List<Object> instanceCache = this.instanceCache;
            switch (classType) {
                case ID_PROXY_CLASS: {
                    final Class<?> type = descriptor.getType();
                    final Object obj = createProxyInstance(creator, type);
                    final int idx = instanceCache.size();
                    instanceCache.add(obj);
                    try {
                        proxyInvocationHandler.set(obj, doReadNestedObject(unshared, "[proxy invocation handler]"));
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
                case ID_WRITE_OBJECT_CLASS:
                case ID_SERIALIZABLE_CLASS: {
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
                case ID_EXTERNALIZABLE_CLASS: {
                    final Class<?> type = descriptor.getType();
                    final SerializableClass serializableClass = registry.lookup(type);
                    final Externalizable obj = (Externalizable) creator.create(type);
                    final int idx = instanceCache.size();
                    instanceCache.add(obj);
                    final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                    obj.readExternal(blockUnmarshaller);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                    final Object resolvedObject = objectResolver.readResolve(serializableClass.hasReadResolve() ? serializableClass.callReadResolve(obj) : obj);
                    if (unshared) {
                        instanceCache.set(idx, null);
                    } else if (obj != resolvedObject) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return resolvedObject;
                }
                case ID_EXTERNALIZER_CLASS: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    Externalizer externalizer = ((ExternalizerClassDescriptor) descriptor).getExternalizer();
                    final Class<?> type = descriptor.getType();
                    final SerializableClass serializableClass = registry.lookup(type);
                    final Object obj;
                    final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                    obj = externalizer.createExternal(type, blockUnmarshaller, creator);
                    instanceCache.set(idx, obj);
                    externalizer.readExternal(obj, blockUnmarshaller);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                    final Object resolvedObject = objectResolver.readResolve(serializableClass.hasReadResolve() ? serializableClass.callReadResolve(obj) : obj);
                    if (unshared) {
                        instanceCache.set(idx, null);
                    } else if (obj != resolvedObject) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return resolvedObject;
                }
                case ID_ENUM_TYPE_CLASS: {
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
                case ID_OBJECT_ARRAY_TYPE_CLASS: {
                    return doReadObjectArray(readInt(), descriptor.getType().getComponentType(), unshared);
                }
                case ID_STRING_CLASS: {
                    // v1 string
                    final String obj = readString();
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    instanceCache.add(unshared ? null : resolvedObject);
                    return resolvedObject;
                }
                case ID_CLASS_CLASS: {
                    final ClassDescriptor nestedDescriptor = doReadClassDescriptor(readUnsignedByte());
                    // Classes are not resolved and may not be unshared!
                    final Class<?> obj = nestedDescriptor.getType();
                    return obj;
                }
                case ID_BOOLEAN_ARRAY_CLASS: {
                    return doReadBooleanArray(readInt(), unshared);
                }
                case ID_BYTE_ARRAY_CLASS: {
                    return doReadByteArray(readInt(), unshared);
                }
                case ID_SHORT_ARRAY_CLASS: {
                    return doReadShortArray(readInt(), unshared);
                }
                case ID_INT_ARRAY_CLASS: {
                    return doReadIntArray(readInt(), unshared);
                }
                case ID_LONG_ARRAY_CLASS: {
                    return doReadLongArray(readInt(), unshared);
                }
                case ID_CHAR_ARRAY_CLASS: {
                    return doReadCharArray(readInt(), unshared);
                }
                case ID_FLOAT_ARRAY_CLASS: {
                    return doReadFloatArray(readInt(), unshared);
                }
                case ID_DOUBLE_ARRAY_CLASS: {
                    return doReadDoubleArray(readInt(), unshared);
                }
                case ID_BOOLEAN_CLASS: {
                    return objectResolver.readResolve(Boolean.valueOf(readBoolean()));
                }
                case ID_BYTE_CLASS: {
                    return objectResolver.readResolve(Byte.valueOf(readByte()));
                }
                case ID_SHORT_CLASS: {
                    return objectResolver.readResolve(Short.valueOf(readShort()));
                }
                case ID_INTEGER_CLASS: {
                    return objectResolver.readResolve(Integer.valueOf(readInt()));
                }
                case ID_LONG_CLASS: {
                    return objectResolver.readResolve(Long.valueOf(readLong()));
                }
                case ID_CHARACTER_CLASS: {
                    return objectResolver.readResolve(Character.valueOf(readChar()));
                }
                case ID_FLOAT_CLASS: {
                    return objectResolver.readResolve(Float.valueOf(readFloat()));
                }
                case ID_DOUBLE_CLASS: {
                    return objectResolver.readResolve(Double.valueOf(readDouble()));
                }
                case ID_OBJECT_CLASS:
                case ID_PLAIN_CLASS: {
                    throw new NotSerializableException("(remote)" + descriptor.getType().getName());
                }
                default: {
                    throw new InvalidObjectException("Unexpected class type " + classType);
                }
            }
        } catch (IOException e) {
            TraceInformation.addIncompleteObjectInformation(e, descriptor.getType());
            exceptionListener.handleUnmarshallingException(e, descriptor.getType());
            throw e;
        } catch (ClassNotFoundException e) {
            TraceInformation.addIncompleteObjectInformation(e, descriptor.getType());
            exceptionListener.handleUnmarshallingException(e, descriptor.getType());
            throw e;
        } catch (RuntimeException e) {
            TraceInformation.addIncompleteObjectInformation(e, descriptor.getType());
            exceptionListener.handleUnmarshallingException(e, descriptor.getType());
            throw e;
        }
    }

    private Object doReadDoubleArray(final int cnt, final boolean unshared) throws IOException {
        final double[] array = new double[cnt];
        for (int i = 0; i < cnt; i ++) {
            array[i] = readDouble();
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadFloatArray(final int cnt, final boolean unshared) throws IOException {
        final float[] array = new float[cnt];
        for (int i = 0; i < cnt; i ++) {
            array[i] = readFloat();
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadCharArray(final int cnt, final boolean unshared) throws IOException {
        final char[] array = new char[cnt];
        for (int i = 0; i < cnt; i ++) {
            array[i] = readChar();
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadLongArray(final int cnt, final boolean unshared) throws IOException {
        final long[] array = new long[cnt];
        for (int i = 0; i < cnt; i ++) {
            array[i] = readLong();
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadIntArray(final int cnt, final boolean unshared) throws IOException {
        final int[] array = new int[cnt];
        for (int i = 0; i < cnt; i ++) {
            array[i] = readInt();
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadShortArray(final int cnt, final boolean unshared) throws IOException {
        final short[] array = new short[cnt];
        for (int i = 0; i < cnt; i ++) {
            array[i] = readShort();
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadByteArray(final int cnt, final boolean unshared) throws IOException {
        final byte[] array = new byte[cnt];
        readFully(array, 0, array.length);
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadBooleanArray(final int cnt, final boolean unshared) throws IOException {
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
            v = readByte();
            switch (cnt & 7) {
                case 7:
                    array[bc + 6] = (v & 64) != 0;
                case 6:
                    array[bc + 5] = (v & 32) != 0;
                case 5:
                    array[bc + 4] = (v & 16) != 0;
                case 4:
                    array[bc + 3] = (v & 8) != 0;
                case 3:
                    array[bc + 2] = (v & 4) != 0;
                case 2:
                    array[bc + 1] = (v & 2) != 0;
                case 1:
                    array[bc] = (v & 1) != 0;
            }
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        instanceCache.add(unshared ? null : resolvedObject);
        return resolvedObject;
    }

    private Object doReadObjectArray(final int cnt, final Class<?> type, final boolean unshared) throws ClassNotFoundException, IOException {
        final Object[] array = (Object[]) Array.newInstance(type, cnt);
        final int idx = instanceCache.size();
        instanceCache.add(array);
        for (int i = 0; i < cnt; i ++) {
            array[i] = doReadCollectionObject(unshared, i, cnt);
        }
        final Object resolvedObject = objectResolver.readResolve(array);
        if (unshared) {
            instanceCache.set(idx, null);
        } else if (array != resolvedObject) {
            instanceCache.set(idx, resolvedObject);
        }
        return resolvedObject;
    }

    private Object doReadArray(final int cnt, final boolean unshared) throws ClassNotFoundException, IOException {
        final int leadByte = readUnsignedByte();
        switch (leadByte) {
            case ID_PRIM_BOOLEAN: {
                return doReadBooleanArray(cnt, unshared);
            }
            case ID_PRIM_BYTE: {
                return doReadByteArray(cnt, unshared);
            }
            case ID_PRIM_CHAR: {
                return doReadCharArray(cnt, unshared);
            }
            case ID_PRIM_DOUBLE: {
                return doReadDoubleArray(cnt, unshared);
            }
            case ID_PRIM_FLOAT: {
                return doReadFloatArray(cnt, unshared);
            }
            case ID_PRIM_INT: {
                return doReadIntArray(cnt, unshared);
            }
            case ID_PRIM_LONG: {
                return doReadLongArray(cnt, unshared);
            }
            case ID_PRIM_SHORT: {
                return doReadShortArray(cnt, unshared);
            }
            default: {
                return doReadObjectArray(cnt, doReadClassDescriptor(leadByte).getType(), unshared);
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
        final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
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
                if (typeId == ID_WRITE_OBJECT_CLASS) {
                    // read fields
                    info.callReadObject(obj, objectInputStream);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else { // typeid == ID_SERIALIZABLE_CLASS
                    // no user data to read - mark the OIS so that it calls endOfBlock after reading fields!
                    objectInputStream.noCustomData();
                    info.callReadObject(obj, objectInputStream);
                    blockUnmarshaller.restore(objectInputStream.getRestoreIdx());
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
            if (typeId == ID_WRITE_OBJECT_CLASS) {
                // useless user data
                blockUnmarshaller.readToEndBlockData();
                blockUnmarshaller.unblock();
            }
        }
    }

    protected void readFields(final Object obj, final SerializableClassDescriptor descriptor) throws IOException, ClassNotFoundException {
        for (SerializableField serializableField : descriptor.getFields()) {
            try {
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
                            doReadObject(serializableField.isUnshared());
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
                            field.set(obj, doReadObject(serializableField.isUnshared()));
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
            } catch (IOException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
            } catch (ClassNotFoundException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
            } catch (RuntimeException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
            }
        }
    }

    void addValidation(final ObjectInputValidation validation, final int prio) {
        final Validator validator = new Validator(prio, validatorSeq++, validation);
        final SortedSet<Validator> validators = this.validators;
        (validators == null ? this.validators = new TreeSet<Validator>() : validators).add(validator);
    }

    public String readUTF() throws IOException {
        final int len = readInt();
        return UTFUtils.readUTFBytes(this, len);
    }
}
