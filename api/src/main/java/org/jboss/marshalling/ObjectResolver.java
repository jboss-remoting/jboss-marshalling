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

/**
 * Resolver for substituting objects in a stream.  The resolver is invoked on write before any serialization
 * takes place.
 */
public interface ObjectResolver {
    /**
     * Get the original object for a replacement object read from a stream.
     *
     * @param replacement the replacement object
     * @return the original
     */
    Object readResolve(Object replacement);

    /**
     * Get a replacement for an object being written to a stream.
     *
     * @param original the original object
     * @return the replacement
     */
    Object writeReplace(Object original);
}
