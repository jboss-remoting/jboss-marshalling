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
package org.jboss.marshalling.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An integer-keyed map, optimized for fast copying.  Based on {@code FastCopyHashMap} by Jason T. Greene.
 *
 * @author Jason T. Greene
 * @author David M. Lloyd
 */
public final class IntKeyMap<V> implements Cloneable, Serializable, Iterable<IntKeyMap.Entry<V>> {

    /**
     * Same default as HashMap, must be a power of 2
     */
    private static final int DEFAULT_CAPACITY = 8;

    /**
     * MAX_INT - 1
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 67%, just like IdentityHashMap
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.67f;

    /**
     * The open-addressed table
     */
    private transient Entry<V>[] table;

    /**
     * The current number of key-value pairs
     */
    private transient int size;

    /**
     * The next resize
     */
    private transient int threshold;

    /**
     * The user defined load factor which defines when to resize
     */
    private final float loadFactor;

    private static final long serialVersionUID = -6864280848239317243L;

    public IntKeyMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Can not have a negative size table!");
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (!(loadFactor > 0F && loadFactor <= 1F))
            throw new IllegalArgumentException("Load factor must be greater than 0 and less than or equal to 1");
        this.loadFactor = loadFactor;
        init(initialCapacity, loadFactor);
    }

    @SuppressWarnings("unchecked")
    public IntKeyMap(IntKeyMap<? extends V> map) {
        table = (Entry<V>[]) map.table.clone();
        loadFactor = map.loadFactor;
        size = map.size;
        threshold = map.threshold;
    }

    @SuppressWarnings("unchecked")
    private void init(int initialCapacity, float loadFactor) {
        int c = 1;
        for (; c < initialCapacity; c <<= 1) ;
        threshold = (int) (c * loadFactor);
        // Include the load factor when sizing the table for the first time
        if (initialCapacity > threshold && c < MAXIMUM_CAPACITY) {
            c <<= 1;
            threshold = (int) (c * loadFactor);
        }
        table = (Entry<V>[]) new Entry[c];
    }

    public IntKeyMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public IntKeyMap() {
        this(DEFAULT_CAPACITY);
    }

    private int nextIndex(int index, int length) {
        index = (index >= length - 1) ? 0 : index + 1;
        return index;
    }

    private static boolean eq(Object o1, Object o2) {
        return o1 == o2 || (o1 != null && o1.equals(o2));
    }

    private static int index(int hashCode, int length) {
        return hashCode & (length - 1);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public V get(int key) {
        int length = table.length;
        int index = index(key, length);
        for (int start = index; ;) {
            Entry<V> e = table[index];
            if (e == null)
                return null;
            if (e.key == key)
                return e.value;
            index = nextIndex(index, length);
            if (index == start) // Full table
                return null;
        }
    }

    public boolean containsKey(int key) {
        int length = table.length;
        int index = index(key, length);
        for (int start = index; ;) {
            Entry<V> e = table[index];
            if (e == null)
                return false;
            if (e.key == key)
                return true;
            index = nextIndex(index, length);
            if (index == start) // Full table
                return false;
        }
    }

    public boolean containsValue(Object value) {
        for (Entry<V> e : table)
            if (e != null && eq(value, e.value))
                return true;
        return false;
    }

    public V put(int key, V value) {
        Entry<V>[] table = this.table;
        int hash = key;
        int length = table.length;
        int index = index(hash, length);
        for (int start = index; ;) {
            Entry<V> e = table[index];
            if (e == null)
                break;
            if (e.key == key) {
                table[index] = new Entry<V>(key, value);
                return e.value;
            }
            index = nextIndex(index, length);
            if (index == start)
                throw new IllegalStateException("Table is full!");
        }
        table[index] = new Entry<V>(key, value);
        if (++size >= threshold)
            resize(length);
        return null;
    }

    @SuppressWarnings("unchecked")
    private void resize(int from) {
        int newLength = from << 1;
        // Can't get any bigger
        if (newLength > MAXIMUM_CAPACITY || newLength <= from)
            return;
        Entry<V>[] newTable = new Entry[newLength];
        Entry<V>[] old = table;
        for (Entry<V> e : old) {
            if (e == null)
                continue;
            int index = index(e.key, newLength);
            while (newTable[index] != null)
                index = nextIndex(index, newLength);
            newTable[index] = e;
        }
        threshold = (int) (loadFactor * newLength);
        table = newTable;
    }

    public V remove(int key) {
        Entry<V>[] table = this.table;
        int length = table.length;
        int start = index(key, length);
        for (int index = start; ;) {
            Entry<V> e = table[index];
            if (e == null)
                return null;
            if (e.key == key) {
                table[index] = null;
                relocate(index);
                size--;
                return e.value;
            }
            index = nextIndex(index, length);
            if (index == start)
                return null;
        }
    }

    private void relocate(int start) {
        Entry<V>[] table = this.table;
        int length = table.length;
        int current = nextIndex(start, length);
        for (; ;) {
            Entry<V> e = table[current];
            if (e == null)
                return;
            // A Doug Lea variant of Knuth's Section 6.4 Algorithm R.
            // This provides a non-recursive method of relocating
            // entries to their optimal positions once a gap is created.
            int prefer = index(e.key, length);
            if ((current < prefer && (prefer <= start || start <= current))
                    || (prefer <= start && start <= current)) {
                table[start] = e;
                table[current] = null;
                start = current;
            }
            current = nextIndex(current, length);
        }
    }

    public void clear() {
        Entry<V>[] table = this.table;
        for (int i = 0; i < table.length; i++)
            table[i] = null;
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public IntKeyMap<V> clone() {
        try {
            IntKeyMap<V> clone = (IntKeyMap<V>) super.clone();
            clone.table = table.clone();
            return clone;
        }
        catch (CloneNotSupportedException e) {
            // should never happen
            throw new IllegalStateException(e);
        }
    }

    public void printDebugStats() {
        int optimal = 0;
        int total = 0;
        int totalSkew = 0;
        int maxSkew = 0;
        for (int i = 0; i < table.length; i++) {
            Entry<V> e = table[i];
            if (e != null) {
                total++;
                int target = index(e.key, table.length);
                if (i == target)
                    optimal++;
                else {
                    int skew = Math.abs(i - target);
                    if (skew > maxSkew) maxSkew = skew;
                    totalSkew += skew;
                }
            }
        }
        System.out.println(" Size:             " + size);
        System.out.println(" Real Size:        " + total);
        System.out.println(" Optimal:          " + optimal + " (" + (float) optimal * 100 / total + "%)");
        System.out.println(" Average Distance: " + ((float) totalSkew / (total - optimal)));
        System.out.println(" Max Distance:     " + maxSkew);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        int size = s.readInt();
        init(size, loadFactor);
        for (int i = 0; i < size; i++) {
            int key = s.readInt();
            V value = (V) s.readObject();
            putForCreate(key, value);
        }
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    private void putForCreate(int key, V value) {
        Entry<V>[] table = this.table;
        int length = table.length;
        int index = index(key, length);
        Entry<V> e = table[index];
        while (e != null) {
            index = nextIndex(index, length);
            e = table[index];
        }
        table[index] = new Entry<V>(key, value);
    }

    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(size);
        for (Entry<V> e : table) {
            if (e != null) {
                s.writeInt(e.key);
                s.writeObject(e.value);
            }
        }
    }

    /**
     * Iterate over the entries.  Read-only operation.
     *
     * @return the entry iterator
     */
    public Iterator<Entry<V>> iterator() {
        return new Iterator<Entry<V>>() {
            int i = 0;

            public boolean hasNext() {
                final Entry<V>[] table = IntKeyMap.this.table;
                final int len = table.length;
                if (i == len) {
                    return false;
                }
                while (table[i] == null) {
                    if (++i == len) {
                        return false;
                    }
                }
                return false;
            }

            public Entry<V> next() {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
                return table[i++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * A map entry.
     *
     * @param <V> the value type
     */
    public static final class Entry<V> {

        private final int key;
        private final V value;

        private Entry(int key, V value) {
            this.key = key;
            this.value = value;
        }

        public int getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
