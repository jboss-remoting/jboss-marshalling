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

package org.jboss.marshalling.river;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputValidation;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayDeque;
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
import java.util.SortedMap;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jboss.marshalling.AbstractUnmarshaller;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Pair;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.TraceInformation;
import org.jboss.marshalling.reflect.ReflectiveCreator;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.util.FlatNavigableMap;
import org.jboss.marshalling.util.FlatNavigableSet;

import static org.jboss.marshalling.river.Protocol.*;

/**
 *
 */
public class RiverUnmarshaller extends AbstractUnmarshaller {

    private static final ReflectiveCreator DEFAULT_CREATOR = new ReflectiveCreator();
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
                    return replace(doReadNewObject(readUnsignedByte(), unshared));
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
                    final Object obj = Array.newInstance(doReadClassDescriptor(readUnsignedByte(), true).getType(), 0);
                    instanceCache.set(idx, obj);
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (unshared) {
                        instanceCache.set(idx, null);
                    } else if (obj != resolvedObject) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return replace(obj);
                }
                case ID_ARRAY_SMALL:
                case ID_ARRAY_SMALL_UNSHARED: {
                    if (unshared != (leadByte == ID_ARRAY_SMALL_UNSHARED)) {
                        throw sharedMismatch();
                    }
                    final int len = readUnsignedByte();
                    return replace(doReadArray(len == 0 ? 0x100 : len, unshared));
                }
                case ID_ARRAY_MEDIUM:
                case ID_ARRAY_MEDIUM_UNSHARED: {
                    if (unshared != (leadByte == ID_ARRAY_MEDIUM_UNSHARED)) {
                        throw sharedMismatch();
                    }
                    final int len = readUnsignedShort();
                    return replace(doReadArray(len == 0 ? 0x10000 : len, unshared));
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
                    return replace(doReadArray(len, unshared));
                }
                case ID_PREDEFINED_OBJECT: {
                    if (unshared) {
                        throw new InvalidObjectException("Attempt to read a predefined object as unshared");
                    }
                    return objectTable.readObject(this);
                }
                case ID_BOOLEAN_OBJECT_TRUE: {
                    return replace(objectResolver.readResolve(Boolean.TRUE));
                }
                case ID_BOOLEAN_OBJECT_FALSE: {
                    return replace(objectResolver.readResolve(Boolean.FALSE));
                }
                case ID_BYTE_OBJECT: {
                    return replace(objectResolver.readResolve(Byte.valueOf(readByte())));
                }
                case ID_SHORT_OBJECT: {
                    return replace(objectResolver.readResolve(Short.valueOf(readShort())));
                }
                case ID_INTEGER_OBJECT: {
                    return replace(objectResolver.readResolve(Integer.valueOf(readInt())));
                }
                case ID_LONG_OBJECT: {
                    return replace(objectResolver.readResolve(Long.valueOf(readLong())));
                }
                case ID_FLOAT_OBJECT: {
                    return replace(objectResolver.readResolve(Float.valueOf(readFloat())));
                }
                case ID_DOUBLE_OBJECT: {
                    return replace(objectResolver.readResolve(Double.valueOf(readDouble())));
                }
                case ID_CHARACTER_OBJECT: {
                    return replace(objectResolver.readResolve(Character.valueOf(readChar())));
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
                case ID_CC_LINKED_LIST: {
                    return LinkedList.class;
                }

                case ID_CC_HASH_SET: {
                    return HashSet.class;
                }
                case ID_CC_LINKED_HASH_SET: {
                    return LinkedHashSet.class;
                }
                case ID_CC_TREE_SET: {
                    return TreeSet.class;
                }

                case ID_CC_IDENTITY_HASH_MAP: {
                    return IdentityHashMap.class;
                }
                case ID_CC_HASH_MAP: {
                    return HashMap.class;
                }
                case ID_CC_HASHTABLE: {
                    return Hashtable.class;
                }
                case ID_CC_LINKED_HASH_MAP: {
                    return LinkedHashMap.class;
                }
                case ID_CC_TREE_MAP: {
                    return TreeMap.class;
                }

                case ID_CC_ENUM_SET_PROXY: {
                    return enumSetProxyClass;
                }
                case ID_CC_ENUM_SET: {
                    return EnumSet.class;
                }
                case ID_CC_ENUM_MAP: {
                    return EnumMap.class;
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

                case ID_CC_CONCURRENT_HASH_MAP: {
                    return ConcurrentHashMap.class;
                }
                case ID_CC_COPY_ON_WRITE_ARRAY_LIST: {
                    return CopyOnWriteArrayList.class;
                }
                case ID_CC_COPY_ON_WRITE_ARRAY_SET: {
                    return CopyOnWriteArraySet.class;
                }
                case ID_CC_VECTOR: {
                    return Vector.class;
                }
                case ID_CC_STACK: {
                    return Stack.class;
                }

                case ID_CC_NCOPIES: {
                    return nCopiesClass;
                }

                case ID_SINGLETON_LIST_OBJECT: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Collections.singletonList(doReadNestedObject(false, "Collections#singletonList()"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return replace(resolvedObject);
                }
                case ID_SINGLETON_SET_OBJECT: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Collections.singleton(doReadNestedObject(false, "Collections#singleton()"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return replace(resolvedObject);
                }
                case ID_SINGLETON_MAP_OBJECT: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Collections.singletonMap(doReadNestedObject(false, "Collections#singletonMap() [key]"), doReadNestedObject(false, "Collections#singletonMap() [value]"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return replace(resolvedObject);
                }
                case ID_REVERSE_ORDER2_OBJECT: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Collections.reverseOrder((Comparator<?>) doReadNestedObject(false, "Collections#reverseOrder()"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return replace(resolvedObject);
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
                case ID_REVERSE_ORDER_OBJECT: {
                    return Collections.reverseOrder();
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
                            return replace(readCollectionData(unshared, -1, len, new ArrayList(len)));
                        }
                        case ID_CC_HASH_SET: {
                            return replace(readCollectionData(unshared, -1, len, new HashSet(len)));
                        }
                        case ID_CC_LINKED_HASH_SET: {
                            return replace(readCollectionData(unshared, -1, len, new LinkedHashSet(len)));
                        }
                        case ID_CC_LINKED_LIST: {
                            return replace(readCollectionData(unshared, -1, len, new LinkedList()));
                        }
                        case ID_CC_TREE_SET: {
                            int idx = instanceCache.size();
                            instanceCache.add(null);
                            Comparator comp = (Comparator)doReadNestedObject(false, "java.util.TreeSet comparator");
                            return replace(readSortedSetData(unshared, idx, len, new TreeSet(comp)));
                        }
                        case ID_CC_ENUM_SET_PROXY: {
                            final ClassDescriptor nestedDescriptor = doReadClassDescriptor(readUnsignedByte(), true);
                            final Class<? extends Enum> elementType = nestedDescriptor.getType().asSubclass(Enum.class);
                            return replace(readCollectionData(unshared, -1, len, EnumSet.noneOf(elementType)));
                        }
                        case ID_CC_VECTOR: {
                            return replace(readCollectionData(unshared, -1, len, new Vector(len)));
                        }
                        case ID_CC_STACK: {
                            return replace(readCollectionData(unshared, -1, len, new Stack()));
                        }
                        case ID_CC_ARRAY_DEQUE: {
                            return replace(readCollectionData(unshared, -1, len, new ArrayDeque(len)));
                        }

                        case ID_CC_HASH_MAP: {
                            return replace(readMapData(unshared, -1, len, new HashMap(len)));
                        }
                        case ID_CC_HASHTABLE: {
                            return replace(readMapData(unshared, -1, len, new Hashtable(len)));
                        }
                        case ID_CC_IDENTITY_HASH_MAP: {
                            return replace(readMapData(unshared, -1, len, new IdentityHashMap(len)));
                        }
                        case ID_CC_LINKED_HASH_MAP: {
                            return replace(readMapData(unshared, -1, len, new LinkedHashMap(len)));
                        }
                        case ID_CC_TREE_MAP: {
                            int idx = instanceCache.size();
                            instanceCache.add(null);
                            Comparator comp = (Comparator)doReadNestedObject(false, "java.util.TreeSet comparator");
                            return replace(readSortedMapData(unshared, idx, len, new TreeMap(comp)));
                        }
                        case ID_CC_ENUM_MAP: {
                            int idx = instanceCache.size();
                            instanceCache.add(null);
                            final ClassDescriptor nestedDescriptor = doReadClassDescriptor(readUnsignedByte(), true);
                            final Class<? extends Enum> elementType = nestedDescriptor.getType().asSubclass(Enum.class);
                            return replace(readMapData(unshared, idx, len, new EnumMap(elementType)));
                        }
                        case ID_CC_NCOPIES: {
                            final int idx = instanceCache.size();
                            instanceCache.add(null);
                            final Object obj = Collections.nCopies(len, doReadNestedObject(false, "n-copies member object"));
                            final Object resolvedObject = objectResolver.readResolve(obj);
                            if (! unshared) {
                                instanceCache.set(idx, resolvedObject);
                            }
                            return replace(resolvedObject);
                        }
                        default: {
                            throw new StreamCorruptedException("Unexpected byte found when reading a collection type: " + leadByte);
                        }
                    }
                }

                case ID_PAIR: {
                    final int idx = instanceCache.size();
                    instanceCache.add(null);
                    final Object obj = Pair.create(doReadNestedObject(unshared, "java.util.marshalling.Pair [A]"), doReadNestedObject(unshared, "java.util.marshalling.Pair [B]"));
                    final Object resolvedObject = objectResolver.readResolve(obj);
                    if (! unshared) {
                        instanceCache.set(idx, resolvedObject);
                    }
                    return replace(resolvedObject);
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
                    leadByte = readUnsignedByte();
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
    private Object readCollectionData(final boolean unshared, int cacheIdx, final int len, final Collection target) throws ClassNotFoundException, IOException {
        final ArrayList<Object> instanceCache = this.instanceCache;
        final int idx;

        if (cacheIdx == -1) {
            idx = instanceCache.size();
            instanceCache.add(target);
        } else {
            idx = cacheIdx;
            instanceCache.set(idx, target);
        }

        for (int i = 0; i < len; i ++) {
            target.add(doReadCollectionObject(false, i, len));
        }
        final Object resolvedObject = objectResolver.readResolve(target);
        instanceCache.set(idx, unshared ? null : resolvedObject);

        return resolvedObject;
    }

    @SuppressWarnings({ "unchecked" })
    private Object readSortedSetData(final boolean unshared, int cacheIdx, final int len, final SortedSet target) throws ClassNotFoundException, IOException {
        final ArrayList<Object> instanceCache = this.instanceCache;
        final int idx;
        final FlatNavigableSet filler = new FlatNavigableSet(target.comparator());

        if (cacheIdx == -1) {
            idx = instanceCache.size();
            instanceCache.add(target);
        } else {
            idx = cacheIdx;
            instanceCache.set(idx, target);
        }

        for (int i = 0; i < len; i ++) {
            filler.add(doReadCollectionObject(false, i, len));
        }
        target.addAll(filler);
        final Object resolvedObject = objectResolver.readResolve(target);
        instanceCache.set(idx, unshared ? null : resolvedObject);

        return resolvedObject;
    }

    @SuppressWarnings({ "unchecked" })
    private Object readMapData(final boolean unshared, int cacheIdx, final int len, final Map target) throws ClassNotFoundException, IOException {
        final ArrayList<Object> instanceCache = this.instanceCache;
        final int idx;

        if (cacheIdx == -1) {
            idx = instanceCache.size();
            instanceCache.add(target);
        } else {
            idx = cacheIdx;
            instanceCache.set(idx, target);
        }

        for (int i = 0; i < len; i ++) {
            target.put(doReadMapObject(false, i, len, true), doReadMapObject(false, i, len, false));
        }
        final Object resolvedObject = objectResolver.readResolve(target);
        instanceCache.set(idx, unshared ? null : resolvedObject);

        return resolvedObject;
    }

    @SuppressWarnings({ "unchecked" })
    private Object readSortedMapData(final boolean unshared, int cacheIdx, final int len, final SortedMap target) throws ClassNotFoundException, IOException {
        final ArrayList<Object> instanceCache = this.instanceCache;
        final int idx;
        final FlatNavigableMap filler = new FlatNavigableMap(target.comparator());

        if (cacheIdx == -1) {
            idx = instanceCache.size();
            instanceCache.add(target);
        } else {
            idx = cacheIdx;
            instanceCache.set(idx, target);
        }

        for (int i = 0; i < len; i ++) {
            filler.put(doReadMapObject(false, i, len, true), doReadMapObject(false, i, len, false));
        }
        // should install entries in order, bypassing any circular ref issues, unless the map is mutated during deserialize of one of its elements
        target.putAll(filler);
        final Object resolvedObject = objectResolver.readResolve(target);
        instanceCache.set(idx, unshared ? null : resolvedObject);

        return resolvedObject;
    }

    private static InvalidObjectException sharedMismatch() {
        return new InvalidObjectException("Shared/unshared object mismatch");
    }

    ClassDescriptor doReadClassDescriptor(final int classType, final boolean required) throws IOException, ClassNotFoundException {
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
                final Class<?> type = classTable.readClass(this);
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(type, ID_ENUM_TYPE_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_EXTERNALIZABLE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = classTable.readClass(this);
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(type, ID_EXTERNALIZABLE_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_EXTERNALIZER_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = classTable.readClass(this);
                final Externalizer externalizer = (Externalizer) readObject();
                final SimpleClassDescriptor descriptor = new ExternalizerClassDescriptor(type, externalizer);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_PLAIN_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = classTable.readClass(this);
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(type, ID_PLAIN_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_PROXY_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = classTable.readClass(this);
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(type, ID_PROXY_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PREDEFINED_SERIALIZABLE_CLASS: {
                final int idx = classCache.size();
                classCache.add(null);
                final Class<?> type = classTable.readClass(this);
                final SerializableClass serializableClass = registry.lookup(type);
                int descType = serializableClass.hasWriteObject() ? ID_WRITE_OBJECT_CLASS : ID_SERIALIZABLE_CLASS;
                final ClassDescriptor descriptor = new BasicSerializableClassDescriptor(serializableClass, doReadClassDescriptor(readUnsignedByte(), true), serializableClass.getFields(), descType);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_PLAIN_CLASS: {
                final String className = readString();
                final Class<?> clazz = classResolver.resolveClass(this, className, 0L);
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(clazz, ID_PLAIN_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case ID_PROXY_CLASS: {
                String[] interfaces = new String[readInt()];
                for (int i = 0; i < interfaces.length; i ++) {
                    interfaces[i] = readString();
                }
                final int idx = classCache.size();
                classCache.add(null);
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(classResolver.resolveProxyClass(this, interfaces), ID_PROXY_CLASS);
                classCache.set(idx, descriptor);
                return descriptor;
            }
            case ID_WRITE_OBJECT_CLASS:
            case ID_SERIALIZABLE_CLASS: {
                int idx = classCache.size();
                classCache.add(null);
                final String className = readString();
                final long uid = readLong();
                Class<?> clazz = null;
                try {
                    clazz = classResolver.resolveClass(this, className, uid);
                } catch (ClassNotFoundException cnfe) {
                    if (required) throw cnfe;
                }
                final FutureSerializableClassDescriptor descriptor = new FutureSerializableClassDescriptor(clazz, classType);
                classCache.set(idx, descriptor);
                final int cnt = readInt();
                final String[] names = new String[cnt];
                final ClassDescriptor[] descriptors = new ClassDescriptor[cnt];
                final boolean[] unshareds = new boolean[cnt];
                for (int i = 0; i < cnt; i ++) {
                    names[i] = readUTF();
                    descriptors[i] = doReadClassDescriptor(readUnsignedByte(), true);
                    unshareds[i] = readBoolean();
                }
                ClassDescriptor superDescriptor = doReadClassDescriptor(readUnsignedByte(), false);
                final Class<?> superClazz = clazz == null ? superDescriptor.getNearestType() : clazz.getSuperclass();
                if (superDescriptor != null) {
                    final Class<?> superType = superDescriptor.getNearestType();
                    if (clazz != null && ! superType.isAssignableFrom(clazz)) {
                        throw new InvalidClassException(clazz.getName(), "Class does not extend stream superclass");
                    }
                    Class<?> cl = superClazz;
                    while (cl != superType) {
                        superDescriptor = new SerializableGapClassDescriptor(registry.lookup(cl), superDescriptor);
                        cl = cl.getSuperclass();
                    }
                } else if (superClazz != null) {
                    Class<?> cl = superClazz;
                    while (serializabilityChecker.isSerializable(cl)) {
                        superDescriptor = new SerializableGapClassDescriptor(registry.lookup(cl), superDescriptor);
                        cl = cl.getSuperclass();
                    }
                }
                final SerializableClass serializableClass;
                final SerializableField[] fields = new SerializableField[cnt];
                if (clazz != null) {
                    serializableClass = registry.lookup(clazz);
                    for (int i = 0; i < cnt; i ++) {
                        fields[i] = serializableClass.getSerializableField(names[i], descriptors[i].getType(), unshareds[i]);
                    }
                } else {
                    serializableClass = null;
                    for (int i = 0; i < cnt; i ++) {
                        fields[i] = new SerializableField(descriptors[i].getType(), names[i], unshareds[i]);
                    }
                }
                descriptor.setResult(new BasicSerializableClassDescriptor(serializableClass, superDescriptor, fields, classType));
                return descriptor;
            }
            case ID_EXTERNALIZABLE_CLASS: {
                final String className = readString();
                final long uid = readLong();
                final Class<?> clazz = classResolver.resolveClass(this, className, uid);
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(clazz, ID_EXTERNALIZABLE_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case ID_EXTERNALIZER_CLASS: {
                final String className = readString();
                int idx = classCache.size();
                classCache.add(null);
                final Class<?> clazz = classResolver.resolveClass(this, className, 0L);
                final Externalizer externalizer = (Externalizer) readObject();
                final SimpleClassDescriptor descriptor = new ExternalizerClassDescriptor(clazz, externalizer);
                classCache.set(idx, descriptor);
                return descriptor;
            }

            case ID_ENUM_TYPE_CLASS: {
                final SimpleClassDescriptor descriptor = new SimpleClassDescriptor(classResolver.resolveClass(this, readString(), 0L), ID_ENUM_TYPE_CLASS);
                classCache.add(descriptor);
                return descriptor;
            }
            case ID_OBJECT_ARRAY_TYPE_CLASS: {
                final ClassDescriptor elementType = doReadClassDescriptor(readUnsignedByte(), true);
                final SimpleClassDescriptor arrayDescriptor = new SimpleClassDescriptor(Array.newInstance(elementType.getType(), 0).getClass(), ID_OBJECT_ARRAY_TYPE_CLASS);
                classCache.add(arrayDescriptor);
                return arrayDescriptor;
            }

            case ID_CC_ARRAY_LIST: {
                return ClassDescriptors.CC_ARRAY_LIST;
            }
            case ID_CC_LINKED_LIST: {
                return ClassDescriptors.CC_LINKED_LIST;
            }

            case ID_CC_HASH_SET: {
                return ClassDescriptors.CC_HASH_SET;
            }
            case ID_CC_LINKED_HASH_SET: {
                return ClassDescriptors.CC_LINKED_HASH_SET;
            }
            case ID_CC_TREE_SET: {
                return ClassDescriptors.CC_TREE_SET;
            }

            case ID_CC_IDENTITY_HASH_MAP: {
                return ClassDescriptors.CC_IDENTITY_HASH_MAP;
            }
            case ID_CC_HASH_MAP: {
                return ClassDescriptors.CC_HASH_MAP;
            }
            case ID_CC_HASHTABLE: {
                return ClassDescriptors.CC_HASHTABLE;
            }
            case ID_CC_LINKED_HASH_MAP: {
                return ClassDescriptors.CC_LINKED_HASH_MAP;
            }
            case ID_CC_TREE_MAP: {
                return ClassDescriptors.CC_TREE_MAP;
            }

            case ID_CC_ENUM_SET: {
                return ClassDescriptors.CC_ENUM_SET;
            }
            case ID_CC_ENUM_MAP: {
                return ClassDescriptors.CC_ENUM_MAP;
            }

            case ID_ABSTRACT_COLLECTION: {
                return ClassDescriptors.ABSTRACT_COLLECTION;
            }
            case ID_ABSTRACT_LIST: {
                return ClassDescriptors.ABSTRACT_LIST;
            }
            case ID_ABSTRACT_QUEUE: {
                return ClassDescriptors.ABSTRACT_QUEUE;
            }
            case ID_ABSTRACT_SEQUENTIAL_LIST: {
                return ClassDescriptors.ABSTRACT_SEQUENTIAL_LIST;
            }
            case ID_ABSTRACT_SET: {
                return ClassDescriptors.ABSTRACT_SET;
            }

            case ID_CC_CONCURRENT_HASH_MAP: {
                return ClassDescriptors.CONCURRENT_HASH_MAP;
            }
            case ID_CC_COPY_ON_WRITE_ARRAY_LIST: {
                return ClassDescriptors.COPY_ON_WRITE_ARRAY_LIST;
            }
            case ID_CC_COPY_ON_WRITE_ARRAY_SET: {
                return ClassDescriptors.COPY_ON_WRITE_ARRAY_SET;
            }
            case ID_CC_VECTOR: {
                return ClassDescriptors.VECTOR;
            }
            case ID_CC_STACK: {
                return ClassDescriptors.STACK;
            }
            case ID_CC_ARRAY_DEQUE: {
                return ClassDescriptors.ARRAY_DEQUE;
            }
            case ID_CC_NCOPIES: {
                return ClassDescriptors.NCOPIES;
            }

            case ID_SINGLETON_MAP_OBJECT: {
                return ClassDescriptors.SINGLETON_MAP;
            }
            case ID_SINGLETON_SET_OBJECT: {
                return ClassDescriptors.SINGLETON_SET;
            }
            case ID_SINGLETON_LIST_OBJECT: {
                return ClassDescriptors.SINGLETON_LIST;
            }

            case ID_EMPTY_MAP_OBJECT: {
                return ClassDescriptors.EMPTY_MAP;
            }
            case ID_EMPTY_SET_OBJECT: {
                return ClassDescriptors.EMPTY_SET;
            }
            case ID_EMPTY_LIST_OBJECT: {
                return ClassDescriptors.EMPTY_LIST;
            }

            case ID_REVERSE_ORDER_OBJECT: {
                return ClassDescriptors.REVERSE_ORDER;
            }
            case ID_REVERSE_ORDER2_OBJECT: {
                return ClassDescriptors.REVERSE_ORDER2;
            }

            case ID_PAIR: {
                return ClassDescriptors.PAIR;
            }

            case ID_STRING_CLASS: {
                return ClassDescriptors.STRING_DESCRIPTOR;
            }
            case ID_OBJECT_CLASS: {
                return ClassDescriptors.OBJECT_DESCRIPTOR;
            }
            case ID_CLASS_CLASS: {
                return ClassDescriptors.CLASS_DESCRIPTOR;
            }
            case ID_ENUM_CLASS: {
                return ClassDescriptors.ENUM_DESCRIPTOR;
            }

            case ID_BOOLEAN_ARRAY_CLASS: {
                return ClassDescriptors.BOOLEAN_ARRAY;
            }
            case ID_BYTE_ARRAY_CLASS: {
                return ClassDescriptors.BYTE_ARRAY;
            }
            case ID_SHORT_ARRAY_CLASS: {
                return ClassDescriptors.SHORT_ARRAY;
            }
            case ID_INT_ARRAY_CLASS: {
                return ClassDescriptors.INT_ARRAY;
            }
            case ID_LONG_ARRAY_CLASS: {
                return ClassDescriptors.LONG_ARRAY;
            }
            case ID_CHAR_ARRAY_CLASS: {
                return ClassDescriptors.CHAR_ARRAY;
            }
            case ID_FLOAT_ARRAY_CLASS: {
                return ClassDescriptors.FLOAT_ARRAY;
            }
            case ID_DOUBLE_ARRAY_CLASS: {
                return ClassDescriptors.DOUBLE_ARRAY;
            }

            case ID_PRIM_BOOLEAN: {
                return ClassDescriptors.BOOLEAN;
            }
            case ID_PRIM_BYTE: {
                return ClassDescriptors.BYTE;
            }
            case ID_PRIM_CHAR: {
                return ClassDescriptors.CHAR;
            }
            case ID_PRIM_DOUBLE: {
                return ClassDescriptors.DOUBLE;
            }
            case ID_PRIM_FLOAT: {
                return ClassDescriptors.FLOAT;
            }
            case ID_PRIM_INT: {
                return ClassDescriptors.INT;
            }
            case ID_PRIM_LONG: {
                return ClassDescriptors.LONG;
            }
            case ID_PRIM_SHORT: {
                return ClassDescriptors.SHORT;
            }

            case ID_VOID: {
                return ClassDescriptors.VOID;
            }

            case ID_BOOLEAN_CLASS: {
                return ClassDescriptors.BOOLEAN_OBJ;
            }
            case ID_BYTE_CLASS: {
                return ClassDescriptors.BYTE_OBJ;
            }
            case ID_SHORT_CLASS: {
                return ClassDescriptors.SHORT_OBJ;
            }
            case ID_INTEGER_CLASS: {
                return ClassDescriptors.INTEGER_OBJ;
            }
            case ID_LONG_CLASS: {
                return ClassDescriptors.LONG_OBJ;
            }
            case ID_CHARACTER_CLASS: {
                return ClassDescriptors.CHARACTER_OBJ;
            }
            case ID_FLOAT_CLASS: {
                return ClassDescriptors.FLOAT_OBJ;
            }
            case ID_DOUBLE_CLASS: {
                return ClassDescriptors.DOUBLE_OBJ;
            }

            case ID_VOID_CLASS: {
                return ClassDescriptors.VOID_OBJ;
            }

            default: {
                throw new InvalidClassException("Unexpected class ID " + classType);
            }
        }
    }

    protected String readString() throws IOException {
        final int length = readInt();
        return UTFUtils.readUTFBytes(this, length);
    }

    public void start(final ByteInput byteInput) throws IOException {
        super.start(byteInput);
        int version = readUnsignedByte();
        if (version < MIN_VERSION || version > configuredVersion || version > MAX_VERSION) {
            throw new IOException("Unsupported protocol version " + version);
        }
        this.version = version;
    }

    protected Object doReadNewObject(final int streamClassType, final boolean unshared) throws ClassNotFoundException, IOException {
        final ClassDescriptor descriptor = doReadClassDescriptor(streamClassType, true);
        try {
            final int classType = descriptor.getTypeID();
            final List<Object> instanceCache = this.instanceCache;
            switch (classType) {
                case ID_PROXY_CLASS: {
                    final Class<?> type = descriptor.getType();
                    final Object obj = registry.lookup(type).callNonInitConstructor();
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
                    final SerializableClass serializableClass = serializableClassDescriptor.getSerializableClass();
                    final Object obj = serializableClass.callNonInitConstructor();
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
                    final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
                    final Externalizable obj;
                    if (serializableClass.hasObjectInputConstructor()) {
                        obj = (Externalizable) serializableClass.callObjectInputConstructor(blockUnmarshaller);
                    } else if (serializableClass.hasNoArgConstructor()) {
                        obj = (Externalizable) serializableClass.callNoArgConstructor();
                    } else {
                        throw new InvalidClassException(type.getName(), "Class is non-public or has no public no-arg constructor");
                    }
                    final int idx = instanceCache.size();
                    instanceCache.add(obj);
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
                    obj = externalizer.createExternal(type, blockUnmarshaller, DEFAULT_CREATOR);
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
                    final ClassDescriptor nestedDescriptor = doReadClassDescriptor(readUnsignedByte(), true);
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
        int v;
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
                return doReadObjectArray(cnt, doReadClassDescriptor(leadByte, true).getType(), unshared);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private static Enum resolveEnumConstant(final ClassDescriptor descriptor, final String name) {
        return Enum.valueOf((Class<? extends Enum>)descriptor.getType(), name);
    }

    private void doInitSerializable(final Object obj, final SerializableClassDescriptor descriptor) throws IOException, ClassNotFoundException {
        final Class<?> type = descriptor.getType();
        final ClassDescriptor superDescriptor = descriptor.getSuperClassDescriptor();
        if (superDescriptor instanceof SerializableClassDescriptor) {
            final SerializableClassDescriptor serializableSuperDescriptor = (SerializableClassDescriptor) superDescriptor;
            doInitSerializable(obj, serializableSuperDescriptor);
        }
        final int typeId = descriptor.getTypeID();
        final BlockUnmarshaller blockUnmarshaller = getBlockUnmarshaller();
        if (type == null) {
            if (descriptor instanceof SerializableGapClassDescriptor) {
                // skip
                return;
            }
            // consume this class' data silently
            discardFields(descriptor);
            if (typeId == ID_WRITE_OBJECT_CLASS) {
                blockUnmarshaller.readToEndBlockData();
                blockUnmarshaller.unblock();
            }
            return;
        }
        final SerializableClass info = registry.lookup(type);
        if (descriptor instanceof SerializableGapClassDescriptor) {
            if (info.hasReadObjectNoData()) {
                info.callReadObjectNoData(obj);
            }
        } else if (info.hasReadObject()) {
            final RiverObjectInputStream objectInputStream = getObjectInputStream();
            final SerializableClassDescriptor oldDescriptor = objectInputStream.swapClass(descriptor);
            final Object oldObj = objectInputStream.swapCurrent(obj);
            final int restoreState = objectInputStream.start();
            boolean ok = false;
            try {
                if (typeId == ID_WRITE_OBJECT_CLASS) {
                    // read fields
                    info.callReadObject(obj, objectInputStream);
                    objectInputStream.finish(restoreState);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                } else { // typeid == ID_SERIALIZABLE_CLASS
                    // no user data to read - mark the OIS so that it calls endOfBlock after reading fields!
                    objectInputStream.noCustomData();
                    info.callReadObject(obj, objectInputStream);
                    objectInputStream.finish(restoreState);
                    blockUnmarshaller.restore(objectInputStream.getRestoreIdx());
                }
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

    protected void discardFields(final SerializableClassDescriptor descriptor) throws IOException {
        for (SerializableField serializableField : descriptor.getFields()) {
            try {
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
            } catch (IOException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
            } catch (ClassNotFoundException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw new IOException("Failed to discard field data", e);
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
    
    private Object replace(Object object) {
        return objectPreResolver.readResolve(object);
    }
}
