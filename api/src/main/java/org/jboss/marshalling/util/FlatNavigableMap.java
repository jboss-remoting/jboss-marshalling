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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class FlatNavigableMap<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
    private final Comparator<? super K> comparator;
    private final List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>();
    private final Set<Entry<K, V>> entrySet = new AbstractSet<Entry<K, V>>() {
        public Iterator<Entry<K, V>> iterator() {
            return entries.iterator();
        }

        public int size() {
            return entries.size();
        }
    };

    public V put(final K key, final V value) {
        entries.add(new EntryImpl<K, V>(key, value));
        return null;
    }

    public FlatNavigableMap(final Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }

    public Entry<K, V> lowerEntry(final K key) {
        throw new UnsupportedOperationException();
    }

    public K lowerKey(final K key) {
        throw new UnsupportedOperationException();
    }

    public Entry<K, V> floorEntry(final K key) {
        throw new UnsupportedOperationException();
    }

    public K floorKey(final K key) {
        throw new UnsupportedOperationException();
    }

    public Entry<K, V> ceilingEntry(final K key) {
        throw new UnsupportedOperationException();
    }

    public K ceilingKey(final K key) {
        throw new UnsupportedOperationException();
    }

    public Entry<K, V> higherEntry(final K key) {
        throw new UnsupportedOperationException();
    }

    public K higherKey(final K key) {
        throw new UnsupportedOperationException();
    }

    public Entry<K, V> firstEntry() {
        throw new UnsupportedOperationException();
    }

    public Entry<K, V> lastEntry() {
        throw new UnsupportedOperationException();
    }

    public Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    public Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    public NavigableMap<K, V> descendingMap() {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<K> navigableKeySet() {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<K> descendingKeySet() {
        throw new UnsupportedOperationException();
    }

    public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
        throw new UnsupportedOperationException();
    }

    public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
        throw new UnsupportedOperationException();
    }

    public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
        throw new UnsupportedOperationException();
    }

    public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
        throw new UnsupportedOperationException();
    }

    public SortedMap<K, V> headMap(final K toKey) {
        throw new UnsupportedOperationException();
    }

    public SortedMap<K, V> tailMap(final K fromKey) {
        throw new UnsupportedOperationException();
    }

    public K firstKey() {
        throw new UnsupportedOperationException();
    }

    public K lastKey() {
        throw new UnsupportedOperationException();
    }

    static final class EntryImpl<K, V> implements Entry<K, V> {
        private final K key;
        private V value;

        EntryImpl(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(final V value) {
            try {
                return this.value;
            } finally {
                this.value = value;
            }
        }

        private static int hashOf(Object obj) {
            return obj == null ? 0 : obj.hashCode();
        }

        private static boolean equals(Object a, Object b) {
            return a == null ? b == null : a.equals(b);
        }

        public final int hashCode() {
            return hashOf(getKey()) ^ hashOf(getValue());
        }

        public final boolean equals(final Object obj) {
            return obj instanceof Entry && equals((Entry<?, ?>) obj);
        }

        public final boolean equals(final Entry<?, ?> obj) {
            return obj != null && equals(getKey(), obj.getKey()) && equals(getValue(), obj.getValue());
        }

        public String toString() {
            return String.format("{%s=>%s}", getKey(), getValue());
        }
    }
}
