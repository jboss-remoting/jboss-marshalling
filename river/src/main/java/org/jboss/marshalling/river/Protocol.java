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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jboss.marshalling._private.GetDeclaredConstructorAction;
import org.jboss.marshalling._private.GetDeclaredFieldsAction;
import org.jboss.marshalling._private.GetReflectionFactoryAction;
import org.jboss.marshalling._private.GetUnsafeAction;
import org.jboss.marshalling._private.SetAccessibleAction;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

/**
 *
 */
final class Protocol {
    public static final int MIN_VERSION = 2;
    public static final int MAX_VERSION = 4;

    public static final int ID_NULL                     = 0x01;
    public static final int ID_REPEAT_OBJECT_FAR        = 0x02;
    public static final int ID_PREDEFINED_OBJECT        = 0x03;
    public static final int ID_NEW_OBJECT               = 0x04;
    public static final int ID_NEW_OBJECT_UNSHARED      = 0x05;

    public static final int ID_REPEAT_CLASS_FAR         = 0x06;

    public static final int ID_PLAIN_CLASS              = 0x07;
    public static final int ID_PROXY_CLASS              = 0x08; // Proxy.isProxy(?) == true
    public static final int ID_SERIALIZABLE_CLASS       = 0x09; // ? extends Serializable.class
    public static final int ID_EXTERNALIZABLE_CLASS     = 0x0a; // ? extends Externalizable.class
    public static final int ID_EXTERNALIZER_CLASS       = 0x0b;
    public static final int ID_ENUM_TYPE_CLASS          = 0x0c; // ? extends Enum.class
    public static final int ID_OBJECT_ARRAY_TYPE_CLASS  = 0x0d; // ? extends Object[].class

    public static final int ID_PREDEFINED_PLAIN_CLASS               = 0x0e;
    public static final int ID_PREDEFINED_PROXY_CLASS               = 0x0f;
    public static final int ID_PREDEFINED_SERIALIZABLE_CLASS        = 0x10;
    public static final int ID_PREDEFINED_EXTERNALIZABLE_CLASS      = 0x11;
    public static final int ID_PREDEFINED_EXTERNALIZER_CLASS        = 0x12;
    public static final int ID_PREDEFINED_ENUM_TYPE_CLASS           = 0x13;

    // the following are never cached
    public static final int ID_STRING_CLASS             = 0x14; // String.class
    public static final int ID_CLASS_CLASS              = 0x15; // Class.class
    public static final int ID_OBJECT_CLASS             = 0x16; // Object.class
    public static final int ID_ENUM_CLASS               = 0x17; // Enum.class

    public static final int ID_BOOLEAN_ARRAY_CLASS      = 0x18; // boolean[].class
    public static final int ID_BYTE_ARRAY_CLASS         = 0x19; // ..etc..
    public static final int ID_SHORT_ARRAY_CLASS        = 0x1a;
    public static final int ID_INT_ARRAY_CLASS          = 0x1b;
    public static final int ID_LONG_ARRAY_CLASS         = 0x1c;
    public static final int ID_CHAR_ARRAY_CLASS         = 0x1d;
    public static final int ID_FLOAT_ARRAY_CLASS        = 0x1e;
    public static final int ID_DOUBLE_ARRAY_CLASS       = 0x1f;

    public static final int ID_PRIM_BOOLEAN             = 0x20; // boolean.class
    public static final int ID_PRIM_BYTE                = 0x21; // ..etc..
    public static final int ID_PRIM_SHORT               = 0x22;
    public static final int ID_PRIM_INT                 = 0x23;
    public static final int ID_PRIM_LONG                = 0x24;
    public static final int ID_PRIM_CHAR                = 0x25;
    public static final int ID_PRIM_FLOAT               = 0x26;
    public static final int ID_PRIM_DOUBLE              = 0x27;

    public static final int ID_VOID                     = 0x28; // void.class

    public static final int ID_BOOLEAN_CLASS            = 0x29; // Boolean.class
    public static final int ID_BYTE_CLASS               = 0x2a; // ..etc..
    public static final int ID_SHORT_CLASS              = 0x2b;
    public static final int ID_INTEGER_CLASS            = 0x2c;
    public static final int ID_LONG_CLASS               = 0x2d;
    public static final int ID_CHARACTER_CLASS          = 0x2e;
    public static final int ID_FLOAT_CLASS              = 0x2f;
    public static final int ID_DOUBLE_CLASS             = 0x30;

    public static final int ID_VOID_CLASS               = 0x31; // Void.class

    // protocol version >= 1
    public static final int ID_START_BLOCK_SMALL        = 0x32; // 8 bit size
    public static final int ID_START_BLOCK_MEDIUM       = 0x33; // 16 bit size
    public static final int ID_START_BLOCK_LARGE        = 0x34; // 32 bit size
    public static final int ID_END_BLOCK_DATA           = 0x35;
    public static final int ID_CLEAR_CLASS_CACHE        = 0x36; // implies CLEAR_INSTANCE_CACHE
    public static final int ID_CLEAR_INSTANCE_CACHE     = 0x37;
    public static final int ID_WRITE_OBJECT_CLASS       = 0x38; // ? extends Serializable.class, plus writeObject method

    // protocol version >= 2
    // uncached
    public static final int ID_REPEAT_OBJECT_NEAR       = 0x39; // 8-bit unsigned negative int relative
    public static final int ID_REPEAT_OBJECT_NEARISH    = 0x3a; // 16-bit unsigned negative int relative
    public static final int ID_REPEAT_CLASS_NEAR        = 0x3b; // 8-bit unsigned negative int relative
    public static final int ID_REPEAT_CLASS_NEARISH     = 0x3c; // 16-bit unsigned negative int relative
    public static final int ID_STRING_EMPTY             = 0x3d; // "" (UNCACHED)
    public static final int ID_STRING_SMALL             = 0x3e; // <=0x100 chars
    public static final int ID_STRING_MEDIUM            = 0x3f; // <=0x10000 chars
    public static final int ID_STRING_LARGE             = 0x40; // <0x80000000 chars
    public static final int ID_ARRAY_EMPTY              = 0x41; // zero elements (CACHED)
    public static final int ID_ARRAY_SMALL              = 0x42; // <=0x100 elements (CACHED)
    public static final int ID_ARRAY_MEDIUM             = 0x43; // <=0x10000 elements (CACHED)
    public static final int ID_ARRAY_LARGE              = 0x44; // <0x80000000 elements (CACHED)
    public static final int ID_ARRAY_EMPTY_UNSHARED     = 0x45; // zero elements (CACHED)
    public static final int ID_ARRAY_SMALL_UNSHARED     = 0x46; // <=0x100 elements (CACHED)
    public static final int ID_ARRAY_MEDIUM_UNSHARED    = 0x47; // <=0x10000 elements (CACHED)
    public static final int ID_ARRAY_LARGE_UNSHARED     = 0x48; // <0x80000000 elements (CACHED)
    // prim wrappers (more efficient) (always shared, never cached)
    public static final int ID_BYTE_OBJECT              = 0x49; // (UNCACHED)
    public static final int ID_SHORT_OBJECT             = 0x4a; // ...
    public static final int ID_INTEGER_OBJECT           = 0x4b;
    public static final int ID_LONG_OBJECT              = 0x4c;
    public static final int ID_CHARACTER_OBJECT         = 0x4d;
    public static final int ID_FLOAT_OBJECT             = 0x4e;
    public static final int ID_DOUBLE_OBJECT            = 0x4f;
    public static final int ID_BOOLEAN_OBJECT_TRUE      = 0x50;
    public static final int ID_BOOLEAN_OBJECT_FALSE     = 0x51;
    // ++ collection classes and types ++
    public static final int ID_COLLECTION_EMPTY             = 0x52; // zero members, type follows
    public static final int ID_COLLECTION_SMALL             = 0x53; // <=0x100 members, count then type follows
    public static final int ID_COLLECTION_MEDIUM            = 0x54; // <=0x10000 members, count then type follows
    public static final int ID_COLLECTION_LARGE             = 0x55; // <0x80000000 members, count then type follows
    public static final int ID_COLLECTION_EMPTY_UNSHARED    = 0x56; // zero members, type follows
    public static final int ID_COLLECTION_SMALL_UNSHARED    = 0x57; // <=0x100 members, count then type follows
    public static final int ID_COLLECTION_MEDIUM_UNSHARED   = 0x58; // <=0x10000 members, count then type follows
    public static final int ID_COLLECTION_LARGE_UNSHARED    = 0x59; // <0x80000000 members, count then type follows
    // lists
    public static final int ID_CC_ARRAY_LIST            = 0x5a;
    public static final int ID_CC_LINKED_LIST           = 0x5b;
    public static final int ID_SINGLETON_LIST_OBJECT    = 0x5c;
    public static final int ID_EMPTY_LIST_OBJECT        = 0x5d;
    // sets
    @Deprecated
    public static final int ID_CC_HASH_SET              = 0x5e;
    @Deprecated
    public static final int ID_CC_LINKED_HASH_SET       = 0x5f;
    @Deprecated
    public static final int ID_CC_TREE_SET              = 0x60;
    public static final int ID_SINGLETON_SET_OBJECT     = 0x61;
    public static final int ID_EMPTY_SET_OBJECT         = 0x62;
    // maps
    public static final int ID_CC_IDENTITY_HASH_MAP     = 0x63;
    @Deprecated
    public static final int ID_CC_HASH_MAP              = 0x64;
    @Deprecated
    public static final int ID_CC_HASHTABLE             = 0x65;
    @Deprecated
    public static final int ID_CC_LINKED_HASH_MAP       = 0x66;
    @Deprecated
    public static final int ID_CC_TREE_MAP              = 0x67;
    public static final int ID_SINGLETON_MAP_OBJECT     = 0x68;
    public static final int ID_EMPTY_MAP_OBJECT         = 0x69;
    // enum holders
    public static final int ID_CC_ENUM_SET_PROXY        = 0x6a;
    public static final int ID_CC_ENUM_SET              = 0x6b;
    public static final int ID_CC_ENUM_MAP              = 0x6c;
    // collection/map base classes
    public static final int ID_ABSTRACT_COLLECTION      = 0x6d;
    public static final int ID_ABSTRACT_SET             = 0x6e;
    public static final int ID_ABSTRACT_LIST            = 0x6f;
    public static final int ID_ABSTRACT_QUEUE           = 0x70;
    public static final int ID_ABSTRACT_SEQUENTIAL_LIST = 0x71;
    // concurrent collections and maps
    @Deprecated
    public static final int ID_CC_CONCURRENT_HASH_MAP       = 0x72;
    public static final int ID_CC_COPY_ON_WRITE_ARRAY_LIST  = 0x73;
    public static final int ID_CC_COPY_ON_WRITE_ARRAY_SET   = 0x74;
    public static final int ID_CC_VECTOR                    = 0x75;
    public static final int ID_CC_STACK                     = 0x76;

    // protocol version >= 3
    public static final int ID_PAIR                     = 0x77;
    public static final int ID_CC_ARRAY_DEQUE           = 0x78;
    public static final int ID_REVERSE_ORDER_OBJECT     = 0x79;
    public static final int ID_REVERSE_ORDER2_OBJECT    = 0x7a;
    public static final int ID_CC_NCOPIES = 0x7b;

    // protocol version >= 4
    public static final int ID_UNMODIFIABLE_COLLECTION  = 0x7c;
    public static final int ID_UNMODIFIABLE_LIST        = 0x7d;
    public static final int ID_UNMODIFIABLE_MAP         = 0x7e;
    public static final int ID_UNMODIFIABLE_SET         = 0x7f;
    public static final int ID_UNMODIFIABLE_SORTED_SET  = 0x80;
    public static final int ID_UNMODIFIABLE_SORTED_MAP  = 0x81;

    public static final int ID_UNMODIFIABLE_MAP_ENTRY_SET = 0x82;

    private static final Unsafe unsafe = AccessController.doPrivileged(GetUnsafeAction.INSTANCE);

    static final Class<?> singletonListClass = Collections.singletonList(null).getClass();
    static final Class<?> singletonSetClass = Collections.singleton(null).getClass();
    static final Class<?> singletonMapClass = Collections.singletonMap(null, null).getClass();

    static final Class<?> emptyListClass = Collections.emptyList().getClass();
    static final Class<?> emptySetClass = Collections.emptySet().getClass();
    static final Class<?> emptyMapClass = Collections.emptyMap().getClass();

    static final Class<?> unmodifiableCollectionClass = Collections.unmodifiableCollection(Collections.emptySet()).getClass();
    static final Class<?> unmodifiableListClass = Collections.unmodifiableList(new LinkedList<Object>()).getClass();
    static final Class<?> unmodifiableRandomAccessListClass = Collections.unmodifiableList(new ArrayList<Object>()).getClass();
    static final Class<?> unmodifiableMapClass = Collections.unmodifiableMap(Collections.emptyMap()).getClass();
    static final Class<?> unmodifiableSetClass = Collections.unmodifiableSet(Collections.emptySet()).getClass();
    static final Class<?> unmodifiableSortedSetClass = Collections.unmodifiableSortedSet(new TreeSet<Object>()).getClass();
    static final Class<?> unmodifiableSortedMapClass = Collections.unmodifiableSortedMap(new TreeMap<Object, Object>()).getClass();

    static final Class<?> unmodifiableMapEntrySetClass = Collections.unmodifiableMap(Collections.emptyMap()).entrySet().getClass();

    static final Class<?> reverseOrderClass = Collections.reverseOrder().getClass();
    static final Class<?> reverseOrder2Class = Collections.reverseOrder(new TrivialComparator()).getClass();
    static final Field reverseOrder2Field;

    static final Class<?> nCopiesClass = Collections.nCopies(1, null).getClass();

    static final Class<?> enumSetProxyClass;

    static final Field unmodifiableCollectionField;
    static final Field unmodifiableListField;
    static final Field unmodifiableRandomAccessListField;
    static final Field unmodifiableMapField;
    static final Field unmodifiableSetField;
    static final Field unmodifiableSortedSetField;
    static final Field unmodifiableSortedMapField;

    static final Field unmodifiableMapEntrySetField;
    static final Constructor<?> unmodifiableMapEntrySetCtor;

    static Object readField(Field field, final Object obj) {
        return unsafe.getObject(obj, unsafe.objectFieldOffset(field));
    }

    static Field findUnmodifiableField(final Class<?> search) {
        Class<?> clazz = search;
        final HashSet<String> strings = new HashSet<String>(Arrays.asList("c", "ss", "list", "m"));
        for (;;) {
            if (clazz == Object.class) {
                throw new IllegalStateException("No candidate collection fields found in " + clazz);
            }
            for (Field field : AccessController.doPrivileged(new GetDeclaredFieldsAction(clazz))) {
                if (strings.contains(field.getName())) {
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    static class TrivialComparator implements Comparator<Object> {
        TrivialComparator() {
        }

        public int compare(final Object o1, final Object o2) {
            return 0;
        }
    }

    static {
        try {
            enumSetProxyClass = Class.forName("java.util.EnumSet$SerializationProxy");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No standard serialization proxy found for enum set!");
        }
        Field field = null;
        for (Field declared : AccessController.doPrivileged(new GetDeclaredFieldsAction(reverseOrder2Class))) {
            if (declared.getName().equals("cmp") || declared.getName().equals("comparator")) {
                field = declared;
                break;
            }
        }
        if (field == null) {
            throw new IllegalStateException("No standard field found for reverse order comparator!");
        }
        reverseOrder2Field = field;
        unmodifiableCollectionField = findUnmodifiableField(unmodifiableCollectionClass);
        unmodifiableSetField = findUnmodifiableField(unmodifiableSetClass);
        unmodifiableListField = findUnmodifiableField(unmodifiableListClass);
        unmodifiableRandomAccessListField = findUnmodifiableField(unmodifiableRandomAccessListClass);
        unmodifiableMapField = findUnmodifiableField(unmodifiableMapClass);
        unmodifiableSortedSetField = findUnmodifiableField(unmodifiableSortedSetClass);
        unmodifiableSortedMapField = findUnmodifiableField(unmodifiableSortedMapClass);

        unmodifiableMapEntrySetField = findUnmodifiableField(unmodifiableMapEntrySetClass);
        final ReflectionFactory reflectionFactory = AccessController.doPrivileged(GetReflectionFactoryAction.INSTANCE);
        final Constructor<?> ctor = AccessController.doPrivileged(GetDeclaredConstructorAction.create(unmodifiableMapEntrySetClass, Set.class));
        unmodifiableMapEntrySetCtor = reflectionFactory.newConstructorForSerialization(unmodifiableMapEntrySetClass, ctor);
        if (! unmodifiableMapEntrySetCtor.isAccessible()) {
            // Java 8 doesn't do this, sometimes :(
            AccessController.doPrivileged(new SetAccessibleAction(unmodifiableMapEntrySetCtor));
        }
    }

    private Protocol() {
    }
}
