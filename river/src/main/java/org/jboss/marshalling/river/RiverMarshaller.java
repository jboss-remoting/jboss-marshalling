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
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.EnumMap;
import java.util.EnumSet;
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
import org.jboss.marshalling.AbstractMarshaller;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.ClassExternalizerFactory;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Pair;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.TraceInformation;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.util.IdentityIntMap;
import org.jboss.marshalling.util.Kind;

import static org.jboss.marshalling.river.Protocol.*;

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
        final int configuredVersion = this.configuredVersion;
        if (configuredVersion < MIN_VERSION || configuredVersion > MAX_VERSION) {
            throw new IOException("Unsupported protocol version " + configuredVersion);
        }
        this.registry = registry;
        final float loadFactor = 0x0.5p0f;
        instanceCache = new IdentityIntMap<Object>((int) ((double)configuration.getInstanceCount() / (double)loadFactor), loadFactor);
        classCache = new IdentityIntMap<Class<?>>((int) ((double)configuration.getClassCount() / (double)loadFactor), loadFactor);
        externalizers = new IdentityHashMap<Class<?>, Externalizer>(configuration.getClassCount());
    }

    protected void doWriteObject(final Object original, final boolean unshared) throws IOException {
        final ClassExternalizerFactory classExternalizerFactory = this.classExternalizerFactory;
        final ObjectResolver objectResolver = this.objectResolver;
        final ObjectResolver objectPreResolver = this.objectPreResolver;
        Object obj = original;
        Class<?> objClass;
        int id;
        boolean isArray, isEnum;
        SerializableClass info;
        boolean unreplaced = true;
        final int configuredVersion = this.configuredVersion;
        try {
            for (;;) {
                if (obj == null) {
                    write(ID_NULL);
                    return;
                }
                final int rid;
                if (! unshared && (rid = instanceCache.get(obj, -1)) != -1) {
                    final int diff = rid - instanceSeq;
                    if (diff >= -256) {
                        write(ID_REPEAT_OBJECT_NEAR);
                        write(diff);
                    } else if (diff >= -65536) {
                        write(ID_REPEAT_OBJECT_NEARISH);
                        writeShort(diff);
                    } else {
                        write(ID_REPEAT_OBJECT_FAR);
                        writeInt(rid);
                    }
                    return;
                }
                // Check for a global pre replacement, before any user replacement is called
                obj = objectPreResolver.writeReplace(obj);
                final ObjectTable.Writer objectTableWriter;
                if (! unshared && (objectTableWriter = objectTable.getObjectWriter(obj)) != null) {
                    write(ID_PREDEFINED_OBJECT);
                    if (configuredVersion == 1) {
                        objectTableWriter.writeObject(getBlockMarshaller(), obj);
                        writeEndBlock();
                    } else {
                        objectTableWriter.writeObject(this, obj);
                    }
                    return;
                }
                objClass = obj.getClass();
                id = getBasicClasses(configuredVersion).get(objClass, -1);
                // First, non-replaceable classes
                if (id == ID_CLASS_CLASS) {
                    final Class<?> classObj = (Class<?>) obj;
                    // If a class is one we have an entry for, we just write that byte directly.
                    // These guys can't be written directly though, otherwise they'll get confused with the objects
                    // of the corresponding type.
                    final int cid = BASIC_CLASSES_V2.get(classObj, -1);
                    switch (cid) {
                        case -1:
                        case ID_SINGLETON_MAP_OBJECT:
                        case ID_SINGLETON_SET_OBJECT:
                        case ID_SINGLETON_LIST_OBJECT:
                        case ID_EMPTY_MAP_OBJECT:
                        case ID_EMPTY_SET_OBJECT:
                        case ID_EMPTY_LIST_OBJECT: {
                            // If the class is one of the above special object types, then we write a
                            // full NEW_OBJECT+CLASS_CLASS header followed by the class byte, or if there is none, write
                            // the full class descriptor.
                            write(ID_NEW_OBJECT);
                            writeClassClass(classObj);
                            return;
                        }

                        default: {
                            write(cid);
                            return;
                        }
                    }
                    // not reached
                }
                isEnum = obj instanceof Enum;
                isArray = objClass.isArray();
                // objects with id != -1 will never make use of the "info" param in *any* way
                info = isArray || isEnum || id != -1 ? null : registry.lookup(objClass);
                // replace once - objects with id != -1 will not have replacement methods but might be globally replaced
                if (unreplaced) {
                    if (info != null) {
                        // check for a user replacement
                        if (info.hasWriteReplace()) {
                            obj = info.callWriteReplace(obj);
                        }
                    }
                    // Check for a global replacement
                    obj = objectResolver.writeReplace(obj);
                    if (obj != original) {
                        unreplaced = false;
                        continue;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            if (isEnum) {
                // objClass cannot equal Enum.class because it is abstract
                final Enum<?> theEnum = (Enum<?>) obj;
                // enums are always shared
                write(ID_NEW_OBJECT);
                writeEnumClass(theEnum.getDeclaringClass());
                writeString(theEnum.name());
                instanceCache.put(obj, instanceSeq++);
                return;
            }
            // Now replaceable classes
            switch (id) {
                case ID_BYTE_CLASS: {
                    write(ID_BYTE_OBJECT);
                    writeByte(((Byte) obj).byteValue());
                    return;
                }
                case ID_BOOLEAN_CLASS: {
                    write(((Boolean) obj).booleanValue() ? ID_BOOLEAN_OBJECT_TRUE : ID_BOOLEAN_OBJECT_FALSE);
                    return;
                }
                case ID_CHARACTER_CLASS: {
                    write(ID_CHARACTER_OBJECT);
                    writeChar(((Character) obj).charValue());
                    return;
                }
                case ID_DOUBLE_CLASS: {
                    write(ID_DOUBLE_OBJECT);
                    writeDouble(((Double) obj).doubleValue());
                    return;
                }
                case ID_FLOAT_CLASS: {
                    write(ID_FLOAT_OBJECT);
                    writeFloat(((Float) obj).floatValue());
                    return;
                }
                case ID_INTEGER_CLASS: {
                    write(ID_INTEGER_OBJECT);
                    writeInt(((Integer) obj).intValue());
                    return;
                }
                case ID_LONG_CLASS: {
                    write(ID_LONG_OBJECT);
                    writeLong(((Long) obj).longValue());
                    return;
                }
                case ID_SHORT_CLASS: {
                    write(ID_SHORT_OBJECT);
                    writeShort(((Short) obj).shortValue());
                    return;
                }
                case ID_STRING_CLASS: {
                    final String string = (String) obj;
                    final int len = string.length();
                    if (len == 0) {
                        write(ID_STRING_EMPTY);
                        // don't cache empty strings
                        return;
                    } else if (len <= 0x100) {
                        write(ID_STRING_SMALL);
                        write(len);
                    } else if (len <= 0x10000) {
                        write(ID_STRING_MEDIUM);
                        writeShort(len);
                    } else {
                        write(ID_STRING_LARGE);
                        writeInt(len);
                    }
                    shallowFlush();
                    UTFUtils.writeUTFBytes(byteOutput, string);
                    if (unshared) {
                        instanceCache.put(obj, -1);
                        instanceSeq++;
                    } else {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    return;
                }
                case ID_BYTE_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final byte[] bytes = (byte[]) obj;
                    final int len = bytes.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_BYTE);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_BYTE);
                        write(bytes, 0, len);
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_BYTE);
                        write(bytes, 0, len);
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_BYTE);
                        write(bytes, 0, len);
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_BOOLEAN_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final boolean[] booleans = (boolean[]) obj;
                    final int len = booleans.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_BOOLEAN);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_BOOLEAN);
                        writeBooleanArray(booleans);
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_BOOLEAN);
                        writeBooleanArray(booleans);
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_BOOLEAN);
                        writeBooleanArray(booleans);
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_CHAR_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final char[] chars = (char[]) obj;
                    final int len = chars.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_CHAR);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_CHAR);
                        for (int i = 0; i < len; i ++) {
                            writeChar(chars[i]);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_CHAR);
                        for (int i = 0; i < len; i ++) {
                            writeChar(chars[i]);
                        }
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_CHAR);
                        for (int i = 0; i < len; i ++) {
                            writeChar(chars[i]);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_SHORT_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final short[] shorts = (short[]) obj;
                    final int len = shorts.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_SHORT);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_SHORT);
                        for (int i = 0; i < len; i ++) {
                            writeShort(shorts[i]);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_SHORT);
                        for (int i = 0; i < len; i ++) {
                            writeShort(shorts[i]);
                        }
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_SHORT);
                        for (int i = 0; i < len; i ++) {
                            writeShort(shorts[i]);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_INT_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final int[] ints = (int[]) obj;
                    final int len = ints.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_INT);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_INT);
                        for (int i = 0; i < len; i ++) {
                            writeInt(ints[i]);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_INT);
                        for (int i = 0; i < len; i ++) {
                            writeInt(ints[i]);
                        }
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_INT);
                        for (int i = 0; i < len; i ++) {
                            writeInt(ints[i]);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_LONG_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final long[] longs = (long[]) obj;
                    final int len = longs.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_LONG);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_LONG);
                        for (int i = 0; i < len; i ++) {
                            writeLong(longs[i]);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_LONG);
                        for (int i = 0; i < len; i ++) {
                            writeLong(longs[i]);
                        }
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_LONG);
                        for (int i = 0; i < len; i ++) {
                            writeLong(longs[i]);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_FLOAT_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final float[] floats = (float[]) obj;
                    final int len = floats.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_FLOAT);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_FLOAT);
                        for (int i = 0; i < len; i ++) {
                            writeFloat(floats[i]);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_FLOAT);
                        for (int i = 0; i < len; i ++) {
                            writeFloat(floats[i]);
                        }
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_FLOAT);
                        for (int i = 0; i < len; i ++) {
                            writeFloat(floats[i]);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_DOUBLE_ARRAY_CLASS: {
                    if (! unshared) {
                        instanceCache.put(obj, instanceSeq++);
                    }
                    final double[] doubles = (double[]) obj;
                    final int len = doubles.length;
                    if (len == 0) {
                        write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                        write(ID_PRIM_DOUBLE);
                    } else if (len <= 256) {
                        write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                        write(len);
                        write(ID_PRIM_DOUBLE);
                        for (int i = 0; i < len; i ++) {
                            writeDouble(doubles[i]);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                        writeShort(len);
                        write(ID_PRIM_DOUBLE);
                        for (int i = 0; i < len; i ++) {
                            writeDouble(doubles[i]);
                        }
                    } else {
                        write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                        writeInt(len);
                        write(ID_PRIM_DOUBLE);
                        for (int i = 0; i < len; i ++) {
                            writeDouble(doubles[i]);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_CC_HASH_SET:
                case ID_CC_LINKED_HASH_SET:
                case ID_CC_TREE_SET:
                case ID_CC_ARRAY_LIST:
                case ID_CC_LINKED_LIST:
                case ID_CC_VECTOR:
                case ID_CC_STACK:
                case ID_CC_ARRAY_DEQUE: {
                    instanceCache.put(obj, instanceSeq++);
                    final Collection<?> collection = (Collection<?>) obj;
                    final int len = collection.size();
                    if (len == 0) {
                        write(unshared ? ID_COLLECTION_EMPTY_UNSHARED : ID_COLLECTION_EMPTY);
                        write(id);
                        if (id == ID_CC_TREE_SET) {
                            doWriteObject(((TreeSet)collection).comparator(), false);
                        }
                    } else if (len <= 256) {
                        write(unshared ? ID_COLLECTION_SMALL_UNSHARED : ID_COLLECTION_SMALL);
                        write(len);
                        write(id);
                        if (id == ID_CC_TREE_SET) {
                            doWriteObject(((TreeSet)collection).comparator(), false);
                        }
                        for (Object o : collection) {
                            doWriteObject(o, false);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_COLLECTION_MEDIUM_UNSHARED : ID_COLLECTION_MEDIUM);
                        writeShort(len);
                        write(id);
                        if (id == ID_CC_TREE_SET) {
                            doWriteObject(((TreeSet)collection).comparator(), false);
                        }
                        for (Object o : collection) {
                            doWriteObject(o, false);
                        }
                    } else {
                        write(unshared ? ID_COLLECTION_LARGE_UNSHARED : ID_COLLECTION_LARGE);
                        writeInt(len);
                        write(id);
                        if (id == ID_CC_TREE_SET) {
                            doWriteObject(((TreeSet)collection).comparator(), false);
                        }
                        for (Object o : collection) {
                            doWriteObject(o, false);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_CC_ENUM_SET_PROXY: {
                    final Enum[] elements = getEnumSetElements(obj);
                    final int len = elements.length;
                    if (len == 0) {
                        write(unshared ? ID_COLLECTION_EMPTY_UNSHARED : ID_COLLECTION_EMPTY);
                        write(id);
                        writeClass(getEnumSetElementType(obj));
                        instanceCache.put(obj, instanceSeq++);
                    } else if (len <= 256) {
                        write(unshared ? ID_COLLECTION_SMALL_UNSHARED : ID_COLLECTION_SMALL);
                        write(len);
                        write(id);
                        writeClass(getEnumSetElementType(obj));
                        instanceCache.put(obj, instanceSeq++);
                        for (Object o : elements) {
                            doWriteObject(o, false);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_COLLECTION_MEDIUM_UNSHARED : ID_COLLECTION_MEDIUM);
                        writeShort(len);
                        write(id);
                        writeClass(getEnumSetElementType(obj));
                        instanceCache.put(obj, instanceSeq++);
                        for (Object o : elements) {
                            doWriteObject(o, false);
                        }
                    } else {
                        write(unshared ? ID_COLLECTION_LARGE_UNSHARED : ID_COLLECTION_LARGE);
                        writeInt(len);
                        write(id);
                        writeClass(getEnumSetElementType(obj));
                        instanceCache.put(obj, instanceSeq++);
                        for (Object o : elements) {
                            doWriteObject(o, false);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_CC_HASH_MAP:
                case ID_CC_HASHTABLE:
                case ID_CC_IDENTITY_HASH_MAP:
                case ID_CC_LINKED_HASH_MAP:
                case ID_CC_TREE_MAP:
                case ID_CC_ENUM_MAP: {
                    instanceCache.put(obj, instanceSeq++);
                    final Map<?, ?> map = (Map<?, ?>) obj;
                    final int len = map.size();
                    if (len == 0) {
                        write(unshared ? ID_COLLECTION_EMPTY_UNSHARED : ID_COLLECTION_EMPTY);
                        write(id);
                        switch (id) {
                            case ID_CC_TREE_MAP: doWriteObject(((TreeMap)map).comparator(), false); break;
                            case ID_CC_ENUM_MAP: writeClass(getEnumMapKeyType(obj)); break;
                        }
                    } else if (len <= 256) {
                        write(unshared ? ID_COLLECTION_SMALL_UNSHARED : ID_COLLECTION_SMALL);
                        write(len);
                        write(id);
                        switch (id) {
                            case ID_CC_TREE_MAP: doWriteObject(((TreeMap)map).comparator(), false); break;
                            case ID_CC_ENUM_MAP: writeClass(getEnumMapKeyType(obj)); break;
                        }
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            doWriteObject(entry.getKey(), false);
                            doWriteObject(entry.getValue(), false);
                        }
                    } else if (len <= 65536) {
                        write(unshared ? ID_COLLECTION_MEDIUM_UNSHARED : ID_COLLECTION_MEDIUM);
                        writeShort(len);
                        write(id);
                        switch (id) {
                            case ID_CC_TREE_MAP: doWriteObject(((TreeMap)map).comparator(), false); break;
                            case ID_CC_ENUM_MAP: writeClass(getEnumMapKeyType(obj)); break;
                        }
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            doWriteObject(entry.getKey(), false);
                            doWriteObject(entry.getValue(), false);
                        }
                    } else {
                        write(unshared ? ID_COLLECTION_LARGE_UNSHARED : ID_COLLECTION_LARGE);
                        writeInt(len);
                        write(id);
                        switch (id) {
                            case ID_CC_TREE_MAP: doWriteObject(((TreeMap)map).comparator(), false); break;
                            case ID_CC_ENUM_MAP: writeClass(getEnumMapKeyType(obj)); break;
                        }
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            doWriteObject(entry.getKey(), false);
                            doWriteObject(entry.getValue(), false);
                        }
                    }
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }

                case ID_EMPTY_MAP_OBJECT:
                case ID_EMPTY_SET_OBJECT:
                case ID_EMPTY_LIST_OBJECT:
                case ID_REVERSE_ORDER_OBJECT: {
                    write(id);
                    return;
                }
                case ID_SINGLETON_MAP_OBJECT: {
                    instanceCache.put(obj, instanceSeq++);
                    write(id);
                    final Map.Entry entry = (Map.Entry) ((Map) obj).entrySet().iterator().next();
                    doWriteObject(entry.getKey(), false);
                    doWriteObject(entry.getValue(), false);
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_SINGLETON_LIST_OBJECT:
                case ID_SINGLETON_SET_OBJECT: {
                    instanceCache.put(obj, instanceSeq++);
                    write(id);
                    doWriteObject(((Collection)obj).iterator().next(), false);
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_REVERSE_ORDER2_OBJECT: {
                    instanceCache.put(obj, instanceSeq++);
                    write(id);
                    try {
                        doWriteObject(Protocol.reverseOrder2Field.get(obj), false);
                    } catch (IllegalAccessException e) {
                        throw new InvalidObjectException("Cannot access standard field for reverse-order comparator");
                    }
                    return;
                }
                case ID_CC_CONCURRENT_HASH_MAP:
                case ID_CC_COPY_ON_WRITE_ARRAY_LIST:
                case ID_CC_COPY_ON_WRITE_ARRAY_SET: {
                    info = registry.lookup(objClass);
                    break;
                }
                case ID_PAIR: {
                    instanceCache.put(obj, instanceSeq++);
                    write(id);
                    Pair<?, ?> pair = (Pair<?, ?>) obj;
                    doWriteObject(pair.getA(), unshared);
                    doWriteObject(pair.getB(), unshared);
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case ID_CC_NCOPIES: {
                    List<?> list = (List<?>) obj;
                    int size = list.size();
                    if (size == 0) {
                        write(ID_EMPTY_LIST_OBJECT);
                        return;
                    }
                    instanceCache.put(obj, instanceSeq++);
                    if (size <= 256) {
                        write(unshared ? ID_COLLECTION_SMALL_UNSHARED : ID_COLLECTION_SMALL);
                        write(size);
                    } else if (size <= 65536) {
                        write(unshared ? ID_COLLECTION_MEDIUM_UNSHARED : ID_COLLECTION_MEDIUM);
                        writeShort(size);
                    } else {
                        write(unshared ? ID_COLLECTION_LARGE_UNSHARED : ID_COLLECTION_LARGE);
                        writeInt(size);
                    }
                    write(id);
                    doWriteObject(list.iterator().next(), false);
                    if (unshared) {
                        instanceCache.put(obj, -1);
                    }
                    return;
                }
                case -1: break;
                default: throw new NotSerializableException(objClass.getName());
            }
            if (isArray) {
                final Object[] objects = (Object[]) obj;
                final int len = objects.length;
                if (len == 0) {
                    write(unshared ? ID_ARRAY_EMPTY_UNSHARED : ID_ARRAY_EMPTY);
                    writeClass(objClass.getComponentType());
                    instanceCache.put(obj, instanceSeq++);
                } else if (len <= 256) {
                    write(unshared ? ID_ARRAY_SMALL_UNSHARED : ID_ARRAY_SMALL);
                    write(len);
                    writeClass(objClass.getComponentType());
                    instanceCache.put(obj, instanceSeq++);
                    for (int i = 0; i < len; i++) {
                        doWriteObject(objects[i], unshared);
                    }
                } else if (len <= 65536) {
                    write(unshared ? ID_ARRAY_MEDIUM_UNSHARED : ID_ARRAY_MEDIUM);
                    writeShort(len);
                    writeClass(objClass.getComponentType());
                    instanceCache.put(obj, instanceSeq++);
                    for (int i = 0; i < len; i++) {
                        doWriteObject(objects[i], unshared);
                    }
                } else {
                    write(unshared ? ID_ARRAY_LARGE_UNSHARED : ID_ARRAY_LARGE);
                    writeInt(len);
                    writeClass(objClass.getComponentType());
                    instanceCache.put(obj, instanceSeq++);
                    for (int i = 0; i < len; i++) {
                        doWriteObject(objects[i], unshared);
                    }
                }
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            // serialize proxies efficiently
            if (obj instanceof Proxy) {
                write(unshared ? ID_NEW_OBJECT_UNSHARED : ID_NEW_OBJECT);
                writeProxyClass(objClass);
                instanceCache.put(obj, instanceSeq++);
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
                externalizers.put(objClass, externalizer);
            }
            if (externalizer != null) {
                write(unshared ? ID_NEW_OBJECT_UNSHARED : ID_NEW_OBJECT);
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
                write(unshared ? ID_NEW_OBJECT_UNSHARED : ID_NEW_OBJECT);
                final Externalizable ext = (Externalizable) obj;
                final ObjectOutput objectOutput = getObjectOutput();
                writeExternalizableClass(objClass);
                instanceCache.put(obj, instanceSeq++);
                ext.writeExternal(objectOutput);
                writeEndBlock();
                if (unshared) {
                    instanceCache.put(obj, -1);
                }
                return;
            }
            // user type #3: serializable
            if (serializabilityChecker.isSerializable(objClass)) {
                write(unshared ? ID_NEW_OBJECT_UNSHARED : ID_NEW_OBJECT);
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

    private static IdentityIntMap<Class<?>> getBasicClasses(final int configuredVersion) {
        return configuredVersion == 2 ? BASIC_CLASSES_V2 : BASIC_CLASSES_V3;
    }

    private static Class<? extends Enum> getEnumMapKeyType(final Object obj) {
        return getAccessibleEnumFieldValue(ENUM_MAP_KEY_TYPE_FIELD, obj);
    }

    private static Class<? extends Enum> getEnumSetElementType(final Object obj) {
        return getAccessibleEnumFieldValue(ENUM_SET_ELEMENT_TYPE_FIELD, obj);
    }

    private static Enum[] getEnumSetElements(final Object obj) {
        try {
            return (Enum[]) ENUM_SET_VALUES_FIELD.get(obj);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new IllegalStateException("Unexpected state", e);
        }
    }

    private static Class<? extends Enum> getAccessibleEnumFieldValue(final Field field, final Object obj) {
        try {
            return ((Class<?>) field.get(obj)).asSubclass(Enum.class);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new IllegalStateException("Unexpected state", e);
        }
    }

    private void writeBooleanArray(final boolean[] booleans) throws IOException {
        final int len = booleans.length;
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
    }

    private void writeEndBlock() throws IOException {
        final BlockMarshaller blockMarshaller = this.blockMarshaller;
        if (blockMarshaller != null) {
            blockMarshaller.flush();
            writeByte(ID_END_BLOCK_DATA);
        }
    }

    protected ObjectOutput getObjectOutput() {
        final ObjectOutput output = objectOutput;
        return output == null ? (objectOutput = getBlockMarshaller()) : output;
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
            return new RiverObjectOutputStream(getBlockMarshaller(), RiverMarshaller.this);
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
        if (superclass != null && serializabilityChecker.isSerializable(superclass)) {
            doWriteSerializableObject(registry.lookup(superclass), obj, superclass);
        }
        if (info.hasWriteObject()) {
            final RiverObjectOutputStream objectOutputStream = getObjectOutputStream();
            final SerializableClass oldInfo = objectOutputStream.swapClass(info);
            final Object oldObj = objectOutputStream.swapCurrent(obj);
            final int restoreState = objectOutputStream.start();
            boolean ok = false;
            try {
                info.callWriteObject(obj, objectOutputStream);
                objectOutputStream.finish(restoreState);
                writeEndBlock();
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
                try {
                    final Field field = serializableField.getField();
                    switch (serializableField.getKind()) {
                        case BOOLEAN: {
                            writeBoolean(field == null ? false : field.getBoolean(obj));
                            break;
                        }
                        case BYTE: {
                            writeByte(field == null ? 0 : field.getByte(obj));
                            break;
                        }
                        case SHORT: {
                            writeShort(field == null ? 0 : field.getShort(obj));
                            break;
                        }
                        case INT: {
                            writeInt(field == null ? 0 : field.getInt(obj));
                            break;
                        }
                        case CHAR: {
                            writeChar(field == null ? 0 : field.getChar(obj));
                            break;
                        }
                        case LONG: {
                            writeLong(field == null ? 0 : field.getLong(obj));
                            break;
                        }
                        case DOUBLE: {
                            writeDouble(field == null ? 0 : field.getDouble(obj));
                            break;
                        }
                        case FLOAT: {
                            writeFloat(field == null ? 0 : field.getFloat(obj));
                            break;
                        }
                        case OBJECT: {
                            doWriteObject(field == null ? null : field.get(obj), serializableField.isUnshared());
                            break;
                        }
                    }
                } catch (IllegalAccessException e) {
                    final InvalidObjectException ioe = new InvalidObjectException("Unexpected illegal access exception");
                    ioe.initCause(e);
                    throw ioe;
                }
            } catch (IOException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
            } catch (RuntimeException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
            }
        }
    }

    protected void doWriteEmptyFields(final SerializableClass info) throws IOException {
        final SerializableField[] serializableFields = info.getFields();
        for (SerializableField serializableField : serializableFields) {
            try {
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        writeBoolean(false);
                        break;
                    }
                    case BYTE: {
                        writeByte(0);
                        break;
                    }
                    case SHORT: {
                        writeShort(0);
                        break;
                    }
                    case INT: {
                        writeInt(0);
                        break;
                    }
                    case CHAR: {
                        writeChar(0);
                        break;
                    }
                    case LONG: {
                        writeLong(0L);
                        break;
                    }
                    case DOUBLE: {
                        writeDouble(0.0);
                        break;
                    }
                    case FLOAT: {
                        writeFloat(0.0f);
                        break;
                    }
                    case OBJECT: {
                        writeObject(null);
                        break;
                    }
                }
            } catch (IOException e){
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
            } catch (RuntimeException e) {
                TraceInformation.addFieldInformation(e, serializableField.getName());
                throw e;
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
            write(ID_PREDEFINED_PROXY_CLASS);
            classCache.put(objClass, classSeq++);
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(ID_PROXY_CLASS);
            final String[] names = classResolver.getProxyInterfaces(objClass);
            writeInt(names.length);
            for (String name : names) {
                writeString(name);
            }
            classCache.put(objClass, classSeq++);
            if (configuredVersion == 1) {
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
            write(ID_PREDEFINED_ENUM_TYPE_CLASS);
            classCache.put(objClass, classSeq++);
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(ID_ENUM_TYPE_CLASS);
            writeString(classResolver.getClassName(objClass));
            classCache.put(objClass, classSeq++);
            classResolver.annotateClass(this, objClass);
        }
    }

    protected void writeClassClass(final Class<?> classObj) throws IOException {
        write(ID_CLASS_CLASS);
        writeClass(classObj);
        // not cached
    }

    protected void writeObjectArrayClass(final Class<?> objClass) throws IOException {
        write(ID_OBJECT_ARRAY_TYPE_CLASS);
        writeClass(objClass.getComponentType());
        classCache.put(objClass, classSeq++);
    }

    protected void writeClass(final Class<?> objClass) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewClass(objClass);
        }
    }

    private static final IdentityIntMap<Class<?>> BASIC_CLASSES_V2;
    private static final IdentityIntMap<Class<?>> BASIC_CLASSES_V3;

    private static final Field ENUM_SET_ELEMENT_TYPE_FIELD;
    private static final Field ENUM_SET_VALUES_FIELD;
    private static final Field ENUM_MAP_KEY_TYPE_FIELD;

    static {
        final IdentityIntMap<Class<?>> map = new IdentityIntMap<Class<?>>(0x0.6p0f);

        map.put(byte.class, ID_PRIM_BYTE);
        map.put(boolean.class, ID_PRIM_BOOLEAN);
        map.put(char.class, ID_PRIM_CHAR);
        map.put(double.class, ID_PRIM_DOUBLE);
        map.put(float.class, ID_PRIM_FLOAT);
        map.put(int.class, ID_PRIM_INT);
        map.put(long.class, ID_PRIM_LONG);
        map.put(short.class, ID_PRIM_SHORT);

        map.put(void.class, ID_VOID);

        map.put(Byte.class, ID_BYTE_CLASS);
        map.put(Boolean.class, ID_BOOLEAN_CLASS);
        map.put(Character.class, ID_CHARACTER_CLASS);
        map.put(Double.class, ID_DOUBLE_CLASS);
        map.put(Float.class, ID_FLOAT_CLASS);
        map.put(Integer.class, ID_INTEGER_CLASS);
        map.put(Long.class, ID_LONG_CLASS);
        map.put(Short.class, ID_SHORT_CLASS);

        map.put(Void.class, ID_VOID_CLASS);

        map.put(Object.class, ID_OBJECT_CLASS);
        map.put(Class.class, ID_CLASS_CLASS);
        map.put(String.class, ID_STRING_CLASS);
        map.put(Enum.class, ID_ENUM_CLASS);

        map.put(byte[].class, ID_BYTE_ARRAY_CLASS);
        map.put(boolean[].class, ID_BOOLEAN_ARRAY_CLASS);
        map.put(char[].class, ID_CHAR_ARRAY_CLASS);
        map.put(double[].class, ID_DOUBLE_ARRAY_CLASS);
        map.put(float[].class, ID_FLOAT_ARRAY_CLASS);
        map.put(int[].class, ID_INT_ARRAY_CLASS);
        map.put(long[].class, ID_LONG_ARRAY_CLASS);
        map.put(short[].class, ID_SHORT_ARRAY_CLASS);

        map.put(ArrayList.class, ID_CC_ARRAY_LIST);
        map.put(LinkedList.class, ID_CC_LINKED_LIST);

        map.put(HashSet.class, ID_CC_HASH_SET);
        map.put(LinkedHashSet.class, ID_CC_LINKED_HASH_SET);
        map.put(TreeSet.class, ID_CC_TREE_SET);

        map.put(IdentityHashMap.class, ID_CC_IDENTITY_HASH_MAP);
        map.put(HashMap.class, ID_CC_HASH_MAP);
        map.put(Hashtable.class, ID_CC_HASHTABLE);
        map.put(LinkedHashMap.class, ID_CC_LINKED_HASH_MAP);
        map.put(TreeMap.class, ID_CC_TREE_MAP);

        map.put(AbstractCollection.class, ID_ABSTRACT_COLLECTION);
        map.put(AbstractList.class, ID_ABSTRACT_LIST);
        map.put(AbstractQueue.class, ID_ABSTRACT_QUEUE);
        map.put(AbstractSequentialList.class, ID_ABSTRACT_SEQUENTIAL_LIST);
        map.put(AbstractSet.class, ID_ABSTRACT_SET);

        map.put(ConcurrentHashMap.class, ID_CC_CONCURRENT_HASH_MAP);
        map.put(CopyOnWriteArrayList.class, ID_CC_COPY_ON_WRITE_ARRAY_LIST);
        map.put(CopyOnWriteArraySet.class, ID_CC_COPY_ON_WRITE_ARRAY_SET);
        map.put(Vector.class, ID_CC_VECTOR);
        map.put(Stack.class, ID_CC_STACK);

        map.put(emptyListClass, ID_EMPTY_LIST_OBJECT); // special case
        map.put(singletonListClass, ID_SINGLETON_LIST_OBJECT); // special case

        map.put(emptySetClass, ID_EMPTY_SET_OBJECT); // special case
        map.put(singletonSetClass, ID_SINGLETON_SET_OBJECT); // special case

        map.put(emptyMapClass, ID_EMPTY_MAP_OBJECT); // special case
        map.put(singletonMapClass, ID_SINGLETON_MAP_OBJECT); // special case

        map.put(EnumMap.class, ID_CC_ENUM_MAP);
        map.put(EnumSet.class, ID_CC_ENUM_SET);
        map.put(enumSetProxyClass, ID_CC_ENUM_SET_PROXY); // special case

        BASIC_CLASSES_V2 = map.clone();

        map.put(Pair.class, ID_PAIR);
        map.put(ArrayDeque.class, ID_CC_ARRAY_DEQUE);
        map.put(reverseOrderClass, ID_REVERSE_ORDER_OBJECT); // special case
        map.put(reverseOrder2Class, ID_REVERSE_ORDER2_OBJECT); // special case
        map.put(nCopiesClass, ID_CC_NCOPIES);

        BASIC_CLASSES_V3 = map;

        // this solution will work for any JDK which conforms to the serialization spec of Enum; unless they
        // do something tricky involving ObjectStreamField anyway...
        ENUM_SET_VALUES_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    final Field field = enumSetProxyClass.getDeclaredField("elements");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Cannot locate the elements field on EnumSet's serialization proxy!");
                }
            }
        });
        ENUM_SET_ELEMENT_TYPE_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    final Field field = enumSetProxyClass.getDeclaredField("elementType");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Cannot locate the elementType field on EnumSet's serialization proxy!");
                }
            }
        });
        ENUM_MAP_KEY_TYPE_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    final Field field = EnumMap.class.getDeclaredField("keyType");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Cannot locate the keyType field on EnumMap!");
                }
            }
        });
    }

    protected void writeNewClass(final Class<?> objClass) throws IOException {
        if (objClass.isEnum()) {
            writeNewEnumClass(objClass.asSubclass(Enum.class));
        } else if (Proxy.class.isAssignableFrom(objClass)) {
            writeNewProxyClass(objClass);
        } else if (objClass.isArray()) {
            writeObjectArrayClass(objClass);
        } else if (! objClass.isInterface() && serializabilityChecker.isSerializable(objClass)) {
            if (Externalizable.class.isAssignableFrom(objClass)) {
                writeNewExternalizableClass(objClass);
            } else {
                writeNewSerializableClass(objClass);
            }
        } else {
            ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
            if (classTableWriter != null) {
                write(ID_PREDEFINED_PLAIN_CLASS);
                classCache.put(objClass, classSeq++);
                writeClassTableData(objClass, classTableWriter);
            } else {
                write(ID_PLAIN_CLASS);
                writeString(classResolver.getClassName(objClass));
                classResolver.annotateClass(this, objClass);
                classCache.put(objClass, classSeq++);
            }
        }
    }

    private void writeClassTableData(final Class<?> objClass, final ClassTable.Writer classTableWriter) throws IOException {
        if (configuredVersion == 1) {
            classTableWriter.writeClass(getBlockMarshaller(), objClass);
            writeEndBlock();
        } else {
            classTableWriter.writeClass(this, objClass);
        }
    }

    protected boolean writeKnownClass(final Class<?> objClass) throws IOException {
        final int configuredVersion = this.configuredVersion;
        int i = getBasicClasses(configuredVersion).get(objClass, -1);
        if (i != -1) {
            write(i);
            return true;
        }
        i = classCache.get(objClass, -1);
        if (i != -1) {
            final int diff = i - classSeq;
            if (diff >= -256) {
                write(ID_REPEAT_CLASS_NEAR);
                write(diff);
            } else if (diff >= -65536) {
                write(ID_REPEAT_CLASS_NEARISH);
                writeShort(diff);
            } else {
                write(ID_REPEAT_CLASS_FAR);
                writeInt(i);
            }
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
            write(ID_PREDEFINED_SERIALIZABLE_CLASS);
            classCache.put(objClass, classSeq++);
            writeClassTableData(objClass, classTableWriter);
        } else {
            final SerializableClass info = registry.lookup(objClass);
            if (info.hasWriteObject()) {
                write(ID_WRITE_OBJECT_CLASS);
            } else {
                write(ID_SERIALIZABLE_CLASS);
            }
            writeString(classResolver.getClassName(objClass));
            writeLong(info.getEffectiveSerialVersionUID());
            classCache.put(objClass, classSeq++);
            classResolver.annotateClass(this, objClass);
            final SerializableField[] fields = info.getFields();
            final int cnt = fields.length;
            writeInt(cnt);
            for (int i = 0; i < cnt; i++) {
                SerializableField field = fields[i];
                writeUTF(field.getName());
                try {
                    writeClass(field.getKind() == Kind.OBJECT ? Object.class : field.getType());
                } catch (ClassNotFoundException e) {
                    throw new InvalidClassException("Class of field was unloaded");
                }
                writeBoolean(field.isUnshared());
            }
        }
        Class<?> sc = objClass.getSuperclass();
        if (! serializabilityChecker.isSerializable(sc)) {
            write(ID_OBJECT_CLASS);
            return;
        }
        writeClass(sc);
    }

    protected void writeExternalizableClass(final Class<?> objClass) throws IOException {
        if (! writeKnownClass(objClass)) {
            writeNewExternalizableClass(objClass);
        }
    }

    protected void writeNewExternalizableClass(final Class<?> objClass) throws IOException {
        ClassTable.Writer classTableWriter = classTable.getClassWriter(objClass);
        if (classTableWriter != null) {
            write(ID_PREDEFINED_EXTERNALIZABLE_CLASS);
            classCache.put(objClass, classSeq++);
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(ID_EXTERNALIZABLE_CLASS);
            writeString(classResolver.getClassName(objClass));
            writeLong(registry.lookup(objClass).getEffectiveSerialVersionUID());
            classCache.put(objClass, classSeq++);
            classResolver.annotateClass(this, objClass);
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
            write(ID_PREDEFINED_EXTERNALIZER_CLASS);
            classCache.put(objClass, classSeq++);
            writeClassTableData(objClass, classTableWriter);
        } else {
            write(ID_EXTERNALIZER_CLASS);
            writeString(classResolver.getClassName(objClass));
            classCache.put(objClass, classSeq++);
            classResolver.annotateClass(this, objClass);
        }
        writeObject(externalizer);
    }

    public void clearInstanceCache() throws IOException {
        instanceCache.clear();
        instanceSeq = 0;
        if (byteOutput != null) {
            write(ID_CLEAR_INSTANCE_CACHE);
        }
    }

    public void clearClassCache() throws IOException {
        classCache.clear();
        externalizers.clear();
        classSeq = 0;
        instanceCache.clear();
        instanceSeq = 0;
        if (byteOutput != null) {
            write(ID_CLEAR_CLASS_CACHE);
        }
    }

    public void start(final ByteOutput byteOutput) throws IOException {
        super.start(byteOutput);
        writeByte(configuredVersion);
    }

    private void writeString(String string) throws IOException {
        writeInt(string.length());
        shallowFlush();
        UTFUtils.writeUTFBytes(byteOutput, string);
    }

    // Replace writeUTF with a faster, non-scanning version

    public void writeUTF(final String string) throws IOException {
        writeInt(string.length());
        shallowFlush();
        UTFUtils.writeUTFBytes(byteOutput, string);
    }
}
