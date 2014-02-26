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

package org.jboss.test.marshalling;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Pair;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 *
 */
@Test(dataProvider = "singleObjectProvider")
public final class SingleObjectMarshallerTestFactory {

    @DataProvider(name = "singleObjectProvider")
    @SuppressWarnings({ "ZeroLengthArrayAllocation" })
    public static Object[][] parameters() {
        final List<Object[]> newList = new ArrayList<Object[]>();
        final List<Object> testObjects = new ArrayList<Object>();
        populate(testObjects);
        for (Object[] objects : SimpleMarshallerTestFactory.parameters()) {
            for (Object object : testObjects) {
                final Object[] params = new Object[4];
                System.arraycopy(objects, 0, params, 0, 3);
                params[3] = object;
                newList.add(params);
            }
        }
        return newList.toArray(new Object[newList.size()][]);
    }

    private static Map<Integer, Object> populateMap(int size, Map<Integer, Object> map) {
        for (int i = 0; i < size; i ++) {
            map.put(Integer.valueOf(i), Long.valueOf(i));
        }
        return map;
    }

    private static Collection<Integer> populateCollection(int size, Collection<Integer> collection) {
        for (int i = 0; i < size; i++) {
            collection.add(Integer.valueOf(i));
        }
        return collection;
    }

    private static void populateAllMapSizes(List<Object> list, Maker<Map<Integer,Object>> mapMaker) {
        list.add(populateMap(0, mapMaker.make()));
        list.add(populateMap(1, mapMaker.make()));
        list.add(populateMap(80, mapMaker.make()));
        list.add(populateMap(256, mapMaker.make()));
        list.add(populateMap(257, mapMaker.make()));
        list.add(populateMap(8000, mapMaker.make()));
//        list.add(populateMap(65536, mapMaker.make())); // too slow
//        list.add(populateMap(65537, mapMaker.make())); // too slow
//        list.add(populateMap(800000, mapMaker.make())); // too slow
    }

    private static void populateAllCollectionSizes(List<Object> list, Maker<Collection<Integer>> collectionMaker) {
        list.add(populateCollection(0, collectionMaker.make()));
        list.add(populateCollection(1, collectionMaker.make()));
        list.add(populateCollection(80, collectionMaker.make()));
        list.add(populateCollection(256, collectionMaker.make()));
        list.add(populateCollection(257, collectionMaker.make()));
        list.add(populateCollection(8000, collectionMaker.make()));
//        list.add(populateCollection(65536, collectionMaker.make())); // too slow
//        list.add(populateCollection(65537, collectionMaker.make())); // too slow
//        list.add(populateCollection(800000, collectionMaker.make())); // too slow
    }

    private static void populateAllMaps(List<Object> list) {
        populateAllMapSizes(list, hashMapMaker);
        populateAllMapSizes(list, concurrentHashMapMaker);
        populateAllMapSizes(list, linkedHashMapMaker);
        populateAllMapSizes(list, identityHashMapMaker);
        populateAllMapSizes(list, treeMapMaker);
        populateAllMapSizes(list, treeMapCompMaker);
        populateAllMapSizes(list, lruMapMaker);
    }

    private static void populateAllCollections(List<Object> list) {
        populateAllCollectionSizes(list, arrayListMaker);
        populateAllCollectionSizes(list, linkedListMaker);
        populateAllCollectionSizes(list, hashSetMaker);
        populateAllCollectionSizes(list, linkedHashSetMaker);
        populateAllCollectionSizes(list, treeSetMaker);
        populateAllCollectionSizes(list, treeSetCompMaker);
        populateAllCollectionSizes(list, treeSetComp2Maker);
        populateAllCollectionSizes(list, arrayDequeMaker);
    }

    private static void populate(List<Object> list) {
        populateAllMaps(list);
        populateAllCollections(list);
        list.add(Collections.emptySet());
        list.add(Collections.emptyList());
        list.add(Collections.emptyMap());
        list.add(Collections.singleton(Integer.valueOf(1234)));
        list.add(Collections.singletonList(Integer.valueOf(1234)));
        list.add(Collections.singletonMap(Integer.valueOf(1234), Long.valueOf(54321L)));
        list.add(Collections.reverseOrder());
        list.add(Boolean.TRUE);
        list.add(Boolean.FALSE);
        list.add(Pair.create(Pair.class, Boolean.TRUE));
        list.add(null);
        list.add(Short.valueOf((short) 153));
        list.add(Byte.valueOf((byte) 18));
        list.add(Character.valueOf('X'));
        list.add(Float.valueOf(0.12f));
        list.add(Double.valueOf(0.12));
        list.add("This is a string");
        list.add(new TestComplexObject(true, (byte)5, 'c', (short)8192, 294902, 319203219042L, 21.125f, 42.625, "TestString", new HashSet<Object>(Arrays.asList("Hello", Boolean.TRUE, Integer.valueOf(12345)))));
        list.add(new TestComplexExternalizableObject(true, (byte)5, 'c', (short)8192, 294902, 319203219042L, 21.125f, 42.625, "TestString", new HashSet<Object>(Arrays.asList("Hello", Boolean.TRUE, Integer.valueOf(12345)))));
        list.add(Collections.unmodifiableMap(new HashMap<Object, Object>()));
        list.add(Collections.unmodifiableSet(new HashSet<Object>()));
        list.add(Collections.unmodifiableCollection(new HashSet<Object>()));
        list.add(Collections.unmodifiableList(new ArrayList<Object>()));
        list.add(Collections.unmodifiableSortedMap(new TreeMap<Object, Object>()));
        list.add(Collections.unmodifiableSortedSet(new TreeSet<Object>()));
        list.add(EnumSet.noneOf(Thread.State.class));
        list.add(EnumSet.allOf(Thread.State.class));
        list.add(new EnumMap<TimeUnit, String>(TimeUnit.class));
        list.add(new TimeoutException());
        list.add(new TestArrayList());
        list.add(new TestCollectionHolder());
        list.add(Collections.nCopies(123, "This is a test!"));
        list.add(new StringBuffer().append("This is a test!"));
        list.add(new StringBuilder().append("This is a test!"));
        // classes to verify
        list.add(HashMap.class);
        list.add(ConcurrentHashMap.class);
        list.add(LinkedHashMap.class);
        list.add(IdentityHashMap.class);
        list.add(TreeMap.class);
        list.add(ArrayList.class);
        list.add(LinkedList.class);
        list.add(HashSet.class);
        list.add(LinkedHashSet.class);
        list.add(TreeSet.class);
        list.add(EnumSet.class);
        list.add(EnumSet.noneOf(Thread.State.class).getClass());
        list.add(EnumMap.class);
        list.add(Hashtable.class);
        list.add(Vector.class);
        list.add(Stack.class);
        list.add(Collections.emptySet().getClass());
        list.add(Collections.emptyList().getClass());
        list.add(Collections.emptyMap().getClass());
        list.add(Collections.singleton(null).getClass());
        list.add(Collections.singletonList(null).getClass());
        list.add(Collections.singletonMap(null, null).getClass());
        list.add(Collections.reverseOrder().getClass());
        list.add(Collections.unmodifiableMap(new HashMap<Object, Object>()).getClass());
        list.add(Collections.unmodifiableSet(new HashSet<Object>()).getClass());
        list.add(Collections.unmodifiableCollection(new HashSet<Object>()).getClass());
        list.add(Collections.unmodifiableList(new ArrayList<Object>()).getClass());
        list.add(Collections.unmodifiableSortedMap(new TreeMap<Object, Object>()).getClass());
        list.add(Collections.unmodifiableSortedSet(new TreeSet<Object>()).getClass());
        list.add(Collections.synchronizedCollection(Collections.emptySet()).getClass());
        list.add(Collections.synchronizedSet(Collections.emptySet()).getClass());
        list.add(Collections.synchronizedList(Collections.emptyList()).getClass());
        list.add(Collections.synchronizedMap(Collections.emptyMap()).getClass());
        list.add(Collections.synchronizedSortedMap(new TreeMap<Object, Object>()).getClass());
        list.add(Collections.synchronizedSortedSet(new TreeSet<Object>()).getClass());
        list.add(Boolean.class);
        list.add(Short.class);
        list.add(Integer.class);
        list.add(Long.class);
        list.add(Float.class);
        list.add(Double.class);
        list.add(Void.class);
        list.add(boolean.class);
        list.add(short.class);
        list.add(int.class);
        list.add(long.class);
        list.add(float.class);
        list.add(double.class);
        list.add(void.class);
        list.add(Object.class);
        list.add(String.class);
        list.add(Collection.class);
        list.add(List.class);
        list.add(Queue.class);
        list.add(Set.class);
        list.add(SortedSet.class);
        list.add(Map.class);
        list.add(SortedMap.class);
        list.add(RandomAccess.class);
        list.add(Pair.class);
        list.add(ArrayDeque.class);
        list.add(Collections.nCopies(5, null).getClass());
    }

    @Factory(dataProvider = "singleObjectProvider")
    public Object[] getTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration, Object subject) {
        return new Object[] { new SingleObjectMarshallerTests(testMarshallerProvider, testUnmarshallerProvider, configuration, subject) };
    }

    private static final Maker<Map<Integer,Object>> hashMapMaker = new Maker<Map<Integer,Object>>() {
        public Map<Integer, Object> make() {
            return new HashMap<Integer, Object>();
        }
    };

    private static final Maker<Map<Integer,Object>> concurrentHashMapMaker = new Maker<Map<Integer,Object>>() {
        public Map<Integer, Object> make() {
            return new HashMap<Integer, Object>();
        }
    };

    private static final Maker<Map<Integer,Object>> treeMapMaker = new Maker<Map<Integer,Object>>() {
        public Map<Integer, Object> make() {
            return new TreeMap<Integer, Object>();
        }
    };

    private static final Maker<Map<Integer,Object>> treeMapCompMaker = new Maker<Map<Integer,Object>>() {
        public Map<Integer, Object> make() {
            return new TreeMap<Integer, Object>(new IntComp());
        }
    };

    private static final Maker<Map<Integer,Object>> linkedHashMapMaker = new Maker<Map<Integer,Object>>() {
        public Map<Integer, Object> make() {
            return new LinkedHashMap<Integer, Object>();
        }
    };

    private static final Maker<Map<Integer,Object>> lruMapMaker = new Maker<Map<Integer, Object>>() {
        public Map<Integer, Object> make() {
            return new LRUMap<Integer, Object>(60);
        }
    };

    private static final Maker<Map<Integer,Object>> identityHashMapMaker = new Maker<Map<Integer,Object>>() {
        public Map<Integer, Object> make() {
            return new IdentityHashMap<Integer, Object>();
        }
    };

    private static final Maker<Collection<Integer>> arrayListMaker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new ArrayList<Integer>();
        }
    };

    private static final Maker<Collection<Integer>> linkedListMaker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new LinkedList<Integer>();
        }
    };

    private static final Maker<Collection<Integer>> hashSetMaker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new HashSet<Integer>();
        }
    };

    private static final Maker<Collection<Integer>> linkedHashSetMaker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new HashSet<Integer>();
        }
    };

    private static final Maker<Collection<Integer>> treeSetMaker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new TreeSet<Integer>();
        }
    };

    private static final Maker<Collection<Integer>> treeSetCompMaker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new TreeSet<Integer>(new IntComp());
        }
    };

    private static final Maker<Collection<Integer>> treeSetComp2Maker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new TreeSet<Integer>(Collections.reverseOrder(new IntComp()));
        }
    };

    private static final Maker<Collection<Integer>> arrayDequeMaker = new Maker<Collection<Integer>>() {
        public Collection<Integer> make() {
            return new EqualableArrayDeque<Integer>();
        }
    };

    private static final class IntComp implements Comparator<Integer>, Serializable {
        private static final long serialVersionUID = 1L;

        public int compare(Integer i1, Integer i2) {
            return i2 == null ? 1 : i1.compareTo(i2);
        }
    }

    private interface Maker<T> {
        T make();
    }
}
