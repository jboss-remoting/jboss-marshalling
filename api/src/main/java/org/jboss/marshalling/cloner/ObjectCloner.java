/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling.cloner;

import java.io.IOException;

/**
 * An object cloner.  Creates a (possibly deep) clone of an object.  Unlike Marshallers and Unmarshallers, ObjectCloners
 * are thread-safe and can be used to clone object graphs concurrently.
 */
public interface ObjectCloner {

    /**
     * Clear the cloner state and any caches.
     */
    void reset();

    /**
     * Create a deep clone of the given object.
     *
     * @param orig the original object
     * @return the deep clone
     * @throws IOException if a serialization error occurs
     * @throws ClassNotFoundException if a class cannot be loaded during the cloning process
     */
    Object clone(Object orig) throws IOException, ClassNotFoundException;

    /**
     * The identity object cloner.  Always returns the same object it is given.
     */
    ObjectCloner IDENTITY = new ObjectCloner() {
        public void reset() {
        }

        public Object clone(final Object orig) {
            return orig;
        }
    };
}
