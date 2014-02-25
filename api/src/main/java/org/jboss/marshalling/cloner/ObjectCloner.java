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
