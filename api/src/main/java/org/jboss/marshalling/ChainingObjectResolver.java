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
