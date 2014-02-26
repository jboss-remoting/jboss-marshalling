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

import java.util.Arrays;

/**
 * An efficient object set.
 */
public final class IntSet<T> implements Cloneable {
    private Object[] keys;
    private int count;
    private int resizeCount;

    /**
     * Construct a new instance with the given initial capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor the load factor
     */
    public IntSet(int initialCapacity, final float loadFactor) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity must be > 0");
        }
        if (loadFactor <= 0.0f || loadFactor >= 1.0f) {
            throw new IllegalArgumentException("loadFactor must be > 0.0 and < 1.0");
        }
        if (initialCapacity < 16) {
            initialCapacity = 16;
        } else {
            // round up
            final int c = Integer.highestOneBit(initialCapacity) - 1;
            initialCapacity = Integer.highestOneBit(initialCapacity + c);
        }
        keys = new Object[initialCapacity];
        resizeCount = (int) ((double) initialCapacity * (double) loadFactor);
    }

    /**
     * Clone this set.
     *
     * @return a cloned set
     */
    @SuppressWarnings({ "unchecked" })
    public IntSet<T> clone() {
        try {
            final IntSet<T> clone = (IntSet<T>) super.clone();
            clone.keys = keys.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException();
        }
    }

    /**
     * Construct a new instance with the given load factor and an initial capacity of 64.
     *
     * @param loadFactor the load factor
     */
    public IntSet(final float loadFactor) {
        this(64, loadFactor);
    }

    /**
     * Construct a new instance with the given initial capacity and a load factor of {@code 0.5}.
     *
     * @param initialCapacity the initial capacity
     */
    public IntSet(final int initialCapacity) {
        this(initialCapacity, 0.5f);
    }

    /**
     * Construct a new instance with an initial capacity of 64 and a load factor of {@code 0.5}.
     */
    public IntSet() {
        this(0.5f);
    }

    /**
     * Check to see if this set contains a value.
     *
     * @param key the key
     * @return {@code true} if the object is present in the set
     */
    public boolean contains(T key) {
        final Object[] keys = this.keys;
        final int mask = keys.length - 1;
        int hc = key.hashCode() & mask;
        Object v;
        for (;;) {
            v = keys[hc];
            if (key.equals(v)) {
                return true;
            }
            if (v == null) {
                // not found
                return false;
            }
            hc = (hc + 1) & mask;
        }
    }

    /**
     * Add a value into the set, if it's not already in there.
     *
     * @param key the key
     * @return {@code true} if the object was added, or {@code false} if it was already in the set
     */
    public boolean add(T key) {
        final Object[] keys = this.keys;
        final int mask = keys.length - 1;
        Object v;
        int hc = key.hashCode() & mask;
        for (int idx = hc;; idx = hc++ & mask) {
            v = keys[idx];
            if (v == null) {
                keys[idx] = key;
                if (++count > resizeCount) {
                    resize();
                }
                return true;
            }
            if (key.equals(v)) {
                return false;
            }
        }
    }

    private void resize() {
        final Object[] oldKeys = keys;
        final int oldsize = oldKeys.length;
        if (oldsize >= 0x40000000) {
            throw new IllegalStateException("Table full");
        }
        final int newsize = oldsize << 1;
        final int mask = newsize - 1;
        final Object[] newKeys = new Object[newsize];
        keys = newKeys;
        if ((resizeCount <<= 1) == 0) {
            resizeCount = Integer.MAX_VALUE;
        }
        for (int oi = 0; oi < oldsize; oi ++) {
            final Object key = oldKeys[oi];
            if (key != null) {
                int ni = key.hashCode() & mask;
                for (;;) {
                    final Object v = newKeys[ni];
                    if (v == null) {
                        // found
                        newKeys[ni] = key;
                        break;
                    }
                    ni = (ni + 1) & mask;
                }
            }
        }
    }

    public void clear() {
        Arrays.fill(keys, null);
        count = 0;
    }

    /**
     * Get a string summary representation of this map.
     *
     * @return a string representation
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Set length = ").append(keys.length).append(", count = ").append(count).append(", resize count = ").append(resizeCount).append('\n');
        for (int i = 0; i < keys.length; i ++) {
            builder.append('[').append(i).append("] = ");
            if (keys[i] != null) {
                final int hc = keys[i].hashCode();
                builder.append("{ ").append(keys[i]).append(" (hash ").append(hc).append(", modulus ").append(hc % keys.length).append(") }");
            } else {
                builder.append("(blank)");
            }
            builder.append('\n');
        }
        return builder.toString();
    }
}