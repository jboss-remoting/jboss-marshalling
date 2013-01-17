package org.jboss.marshalling.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class FlatNavigableSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final Comparator<? super E> comparator;
    private final List<E> entries = new ArrayList<E>();

    public FlatNavigableSet(final Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    public Iterator<E> iterator() {
        return entries.iterator();
    }

    public int size() {
        return entries.size();
    }

    public boolean add(final E e) {
        return entries.add(e);
    }

    public E lower(final E e) {
        throw new UnsupportedOperationException();
    }

    public E floor(final E e) {
        throw new UnsupportedOperationException();
    }

    public E ceiling(final E e) {
        throw new UnsupportedOperationException();
    }

    public E higher(final E e) {
        throw new UnsupportedOperationException();
    }

    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<E> descendingSet() {
        throw new UnsupportedOperationException();
    }

    public Iterator<E> descendingIterator() {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        throw new UnsupportedOperationException();
    }

    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        throw new UnsupportedOperationException();
    }

    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        throw new UnsupportedOperationException();
    }

    public SortedSet<E> headSet(final E toElement) {
        throw new UnsupportedOperationException();
    }

    public SortedSet<E> tailSet(final E fromElement) {
        throw new UnsupportedOperationException();
    }

    public E first() {
        throw new UnsupportedOperationException();
    }

    public E last() {
        throw new UnsupportedOperationException();
    }
}
