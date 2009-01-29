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

package org.jboss.marshalling;

import java.util.Iterator;
import java.util.Collection;

/**
 * An object resolver which runs a sequence of object resolvers.  On write, the resolvers are run in order from first
 * to last.  On read, the resolvers are run in reverse order, from last to first.
 */
public class ChainingObjectResolver implements ObjectResolver {
    private final ObjectResolver[] resolvers;

    /**
     * Construct a new instance.
     *
     * @param resolvers the resolvers to use
     */
    public ChainingObjectResolver(final ObjectResolver[] resolvers) {
        if (resolvers == null) {
            throw new NullPointerException("resolvers is null");
        }
        this.resolvers = resolvers.clone();
    }

    /**
     * Construct a new instance.
     *
     * @param resolvers the resolvers to use
     */
    public ChainingObjectResolver(final Iterator<ObjectResolver> resolvers) {
        if (resolvers == null) {
            throw new NullPointerException("resolvers is null");
        }
        this.resolvers = unroll(resolvers, 0);
    }

    /**
     * Construct a new instance.
     *
     * @param resolvers the resolvers to use
     */
    public ChainingObjectResolver(final Iterable<ObjectResolver> resolvers) {
        this(resolvers.iterator());
    }

    /**
     * Construct a new instance.
     *
     * @param resolvers the resolvers to use
     */
    public ChainingObjectResolver(final Collection<ObjectResolver> resolvers) {
        if (resolvers == null) {
            throw new NullPointerException("resolvers is null");
        }
        this.resolvers = resolvers.toArray(new ObjectResolver[resolvers.size()]);
    }

    private static ObjectResolver[] unroll(final Iterator<ObjectResolver> iterator, final int i) {
        if (iterator.hasNext()) {
            final ObjectResolver factory = iterator.next();
            final ObjectResolver[] array = unroll(iterator, i + 1);
            array[i] = factory;
            return array;
        } else {
            return new ObjectResolver[i];
        }
    }

    /** {@inheritDoc} */
    public Object readResolve(final Object replacement) {
        Object o = replacement;
        for (int i = resolvers.length - 1; i >= 0; i--) {
            ObjectResolver resolver = resolvers[i];
            o = resolver.readResolve(o);
        }
        return o;
    }

    /** {@inheritDoc} */
    public Object writeReplace(final Object original) {
        Object o = original;
        for (ObjectResolver resolver : resolvers) {
            o = resolver.writeReplace(o);
        }
        return o;
    }
}
