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

import java.util.Collection;
import java.util.Iterator;

/**
 * A class externalizer factory that tries each delegate externalizer factory in sequence, returning the first match.
 */
public class ChainingClassExternalizerFactory implements ClassExternalizerFactory {
    private final ClassExternalizerFactory[] externalizerFactories;

    /**
     * Construct a new instance.
     *
     * @param factories a collection of factories to use
     */
    public ChainingClassExternalizerFactory(final Collection<ClassExternalizerFactory> factories) {
        externalizerFactories = factories.toArray(new ClassExternalizerFactory[factories.size()]);
    }

    /**
     * Construct a new instance.
     *
     * @param factories a collection of factories to use
     */
    public ChainingClassExternalizerFactory(final Iterable<ClassExternalizerFactory> factories) {
        this(factories.iterator());
    }

    /**
     * Construct a new instance.
     *
     * @param factories a sequence of factories to use
     */
    public ChainingClassExternalizerFactory(final Iterator<ClassExternalizerFactory> factories) {
        externalizerFactories = unroll(factories, 0);
    }

    /**
     * Construct a new instance.
     *
     * @param factories an array of factories to use
     */
    public ChainingClassExternalizerFactory(final ClassExternalizerFactory[] factories) {
        externalizerFactories = factories.clone();
    }

    private static ClassExternalizerFactory[] unroll(final Iterator<ClassExternalizerFactory> iterator, final int i) {
        if (iterator.hasNext()) {
            final ClassExternalizerFactory factory = iterator.next();
            final ClassExternalizerFactory[] array = unroll(iterator, i + 1);
            array[i] = factory;
            return array;
        } else {
            return new ClassExternalizerFactory[i];
        }
    }

    /** {@inheritDoc}  This implementation tries each nested externalizer factory in order until a match is found. */
    public Externalizer getExternalizer(final Class<?> type) {
        for (ClassExternalizerFactory externalizerFactory : externalizerFactories) {
            final Externalizer externalizer = externalizerFactory.getExternalizer(type);
            if (externalizer != null) {
                return externalizer;
            }
        }
        return null;
    }
}
