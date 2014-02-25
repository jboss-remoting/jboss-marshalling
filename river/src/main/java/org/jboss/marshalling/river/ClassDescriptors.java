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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jboss.marshalling.Pair;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassDescriptors {

    public static final ClassDescriptor STRING_DESCRIPTOR = new SimpleClassDescriptor(String.class, Protocol.ID_STRING_CLASS);
    public static final ClassDescriptor CLASS_DESCRIPTOR = new SimpleClassDescriptor(Class.class, Protocol.ID_CLASS_CLASS);
    public static final ClassDescriptor OBJECT_DESCRIPTOR = new SimpleClassDescriptor(Object.class, Protocol.ID_OBJECT_CLASS);
    public static final ClassDescriptor ENUM_DESCRIPTOR = new SimpleClassDescriptor(Enum.class, Protocol.ID_ENUM_CLASS);
    public static final ClassDescriptor BOOLEAN = new SimpleClassDescriptor(boolean.class, Protocol.ID_PRIM_BOOLEAN);
    public static final ClassDescriptor BYTE = new SimpleClassDescriptor(byte.class, Protocol.ID_PRIM_BYTE);
    public static final ClassDescriptor SHORT = new SimpleClassDescriptor(short.class, Protocol.ID_PRIM_SHORT);
    public static final ClassDescriptor INT = new SimpleClassDescriptor(int.class, Protocol.ID_PRIM_INT);
    public static final ClassDescriptor LONG = new SimpleClassDescriptor(long.class, Protocol.ID_PRIM_LONG);
    public static final ClassDescriptor CHAR = new SimpleClassDescriptor(char.class, Protocol.ID_PRIM_CHAR);
    public static final ClassDescriptor FLOAT = new SimpleClassDescriptor(float.class, Protocol.ID_PRIM_FLOAT);
    public static final ClassDescriptor DOUBLE = new SimpleClassDescriptor(double.class, Protocol.ID_PRIM_DOUBLE);
    public static final ClassDescriptor VOID = new SimpleClassDescriptor(void.class, Protocol.ID_VOID);
    public static final ClassDescriptor BOOLEAN_OBJ = new SimpleClassDescriptor(Boolean.class, Protocol.ID_BOOLEAN_CLASS);
    public static final ClassDescriptor BYTE_OBJ = new SimpleClassDescriptor(Byte.class, Protocol.ID_BYTE_CLASS);
    public static final ClassDescriptor SHORT_OBJ = new SimpleClassDescriptor(Short.class, Protocol.ID_SHORT_CLASS);
    public static final ClassDescriptor INTEGER_OBJ = new SimpleClassDescriptor(Integer.class, Protocol.ID_INTEGER_CLASS);
    public static final ClassDescriptor LONG_OBJ = new SimpleClassDescriptor(Long.class, Protocol.ID_LONG_CLASS);
    public static final ClassDescriptor CHARACTER_OBJ = new SimpleClassDescriptor(Character.class, Protocol.ID_CHARACTER_CLASS);
    public static final ClassDescriptor FLOAT_OBJ = new SimpleClassDescriptor(Float.class, Protocol.ID_FLOAT_CLASS);
    public static final ClassDescriptor DOUBLE_OBJ = new SimpleClassDescriptor(Double.class, Protocol.ID_DOUBLE_CLASS);
    public static final ClassDescriptor VOID_OBJ = new SimpleClassDescriptor(Void.class, Protocol.ID_VOID_CLASS);
    public static final ClassDescriptor BOOLEAN_ARRAY = new SimpleClassDescriptor(boolean[].class, Protocol.ID_BOOLEAN_ARRAY_CLASS);
    public static final ClassDescriptor BYTE_ARRAY = new SimpleClassDescriptor(byte[].class, Protocol.ID_BYTE_ARRAY_CLASS);
    public static final ClassDescriptor SHORT_ARRAY = new SimpleClassDescriptor(short[].class, Protocol.ID_SHORT_ARRAY_CLASS);
    public static final ClassDescriptor INT_ARRAY = new SimpleClassDescriptor(int[].class, Protocol.ID_INT_ARRAY_CLASS);
    public static final ClassDescriptor LONG_ARRAY = new SimpleClassDescriptor(long[].class, Protocol.ID_LONG_ARRAY_CLASS);
    public static final ClassDescriptor CHAR_ARRAY = new SimpleClassDescriptor(char[].class, Protocol.ID_CHAR_ARRAY_CLASS);
    public static final ClassDescriptor FLOAT_ARRAY = new SimpleClassDescriptor(float[].class, Protocol.ID_FLOAT_ARRAY_CLASS);
    public static final ClassDescriptor DOUBLE_ARRAY = new SimpleClassDescriptor(double[].class, Protocol.ID_DOUBLE_ARRAY_CLASS);
    public static final ClassDescriptor ABSTRACT_COLLECTION = new SimpleClassDescriptor(AbstractCollection.class, Protocol.ID_ABSTRACT_COLLECTION);
    public static final ClassDescriptor ABSTRACT_LIST = new SimpleClassDescriptor(AbstractList.class, Protocol.ID_ABSTRACT_LIST);
    public static final ClassDescriptor ABSTRACT_QUEUE = new SimpleClassDescriptor(AbstractQueue.class, Protocol.ID_ABSTRACT_QUEUE);
    public static final ClassDescriptor ABSTRACT_SEQUENTIAL_LIST = new SimpleClassDescriptor(AbstractSequentialList.class, Protocol.ID_ABSTRACT_SEQUENTIAL_LIST);
    public static final ClassDescriptor ABSTRACT_SET = new SimpleClassDescriptor(AbstractSet.class, Protocol.ID_ABSTRACT_SET);
    // TODO - this should be a protocol byte
    public static final ClassDescriptor ABSTRACT_MAP =new SimpleClassDescriptor(AbstractMap.class, Protocol.ID_PLAIN_CLASS);
    public static final ClassDescriptor PAIR = new SimpleClassDescriptor(Pair.class, Protocol.ID_PAIR);

    // These classes are final
    static final ClassDescriptor SINGLETON_MAP = getSerializableClassDescriptor(Protocol.singletonMapClass);
    static final ClassDescriptor SINGLETON_SET = getSerializableClassDescriptor(Protocol.singletonSetClass);
    static final ClassDescriptor SINGLETON_LIST = getSerializableClassDescriptor(Protocol.singletonListClass);
    static final ClassDescriptor EMPTY_MAP = getSerializableClassDescriptor(Protocol.emptyMapClass);
    static final ClassDescriptor EMPTY_SET = getSerializableClassDescriptor(Protocol.emptySetClass);
    static final ClassDescriptor EMPTY_LIST = getSerializableClassDescriptor(Protocol.emptyListClass);

    // Non-final classes
    static final ClassDescriptor CC_ARRAY_LIST = getSerializableClassDescriptor(ArrayList.class, ABSTRACT_LIST);
    static final ClassDescriptor CC_LINKED_LIST = getSerializableClassDescriptor(LinkedList.class, ABSTRACT_SEQUENTIAL_LIST);
    static final ClassDescriptor CC_HASH_SET = getSerializableClassDescriptor(HashSet.class, ABSTRACT_SET);
    static final ClassDescriptor CC_LINKED_HASH_SET = getSerializableClassDescriptor(LinkedHashSet.class, CC_HASH_SET);
    static final ClassDescriptor CC_TREE_SET = getSerializableClassDescriptor(TreeSet.class, ABSTRACT_SET);
    static final ClassDescriptor CC_IDENTITY_HASH_MAP = getSerializableClassDescriptor(IdentityHashMap.class, ABSTRACT_MAP);
    static final ClassDescriptor CC_HASH_MAP = getSerializableClassDescriptor(HashMap.class, ABSTRACT_MAP);
    static final ClassDescriptor CC_HASHTABLE = getSerializableClassDescriptor(Hashtable.class, new SimpleClassDescriptor(Dictionary.class, Protocol.ID_PLAIN_CLASS));
    static final ClassDescriptor CC_LINKED_HASH_MAP = getSerializableClassDescriptor(LinkedHashMap.class, CC_HASH_MAP);
    static final ClassDescriptor CC_TREE_MAP = getSerializableClassDescriptor(TreeMap.class, ABSTRACT_MAP);
    static final ClassDescriptor CC_ENUM_SET = getSerializableClassDescriptor(EnumSet.class, ABSTRACT_SET);
    static final ClassDescriptor CC_ENUM_MAP = getSerializableClassDescriptor(EnumMap.class, ABSTRACT_MAP);
    static final ClassDescriptor CONCURRENT_HASH_MAP = getSerializableClassDescriptor(ConcurrentHashMap.class, ABSTRACT_MAP);
    static final ClassDescriptor COPY_ON_WRITE_ARRAY_LIST = getSerializableClassDescriptor(CopyOnWriteArrayList.class, OBJECT_DESCRIPTOR);
    static final ClassDescriptor COPY_ON_WRITE_ARRAY_SET = getSerializableClassDescriptor(CopyOnWriteArraySet.class, ABSTRACT_SET);
    static final ClassDescriptor VECTOR = getSerializableClassDescriptor(Vector.class, ABSTRACT_LIST);
    static final ClassDescriptor STACK = getSerializableClassDescriptor(Stack.class, VECTOR);
    static final ClassDescriptor ARRAY_DEQUE = getSerializableClassDescriptor(ArrayDeque.class, ABSTRACT_COLLECTION);

    // These classes are final
    static final ClassDescriptor REVERSE_ORDER = getSerializableClassDescriptor(Protocol.reverseOrderClass);
    static final ClassDescriptor REVERSE_ORDER2 = getSerializableClassDescriptor(Protocol.reverseOrder2Class);
    static final ClassDescriptor NCOPIES = getSerializableClassDescriptor(Protocol.nCopiesClass);

    private ClassDescriptors() {
    }

    private static SerializableClassDescriptor getSerializableClassDescriptor(final Class<?> subject) {
        return getSerializableClassDescriptor(subject, null);
    }

    private static SerializableClassDescriptor getSerializableClassDescriptor(final Class<?> subject, final ClassDescriptor superDescriptor) {
        return AccessController.doPrivileged(new PrivilegedAction<SerializableClassDescriptor>() {
            public SerializableClassDescriptor run() {
                final SerializableClassRegistry reg = SerializableClassRegistry.getInstance();
                final SerializableClass serializableClass = reg.lookup(subject);
                final SerializableField[] fields = serializableClass.getFields();
                final boolean hasWriteObject = serializableClass.hasWriteObject();
                try {
                    return new BasicSerializableClassDescriptor(serializableClass, superDescriptor, fields, Externalizable.class.isAssignableFrom(subject) ? Protocol.ID_EXTERNALIZABLE_CLASS : hasWriteObject ? Protocol.ID_WRITE_OBJECT_CLASS : Protocol.ID_SERIALIZABLE_CLASS);
                } catch (ClassNotFoundException e) {
                    throw new NoClassDefFoundError(e.getMessage());
                }
            }
        });
    }
}
