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

import java.io.Serializable;

/**
 * A checker to determine whether an object class should be treated as serializable.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface SerializabilityChecker {

    /**
     * Determine whether an object class is serializable.
     *
     * @param clazz the object class to test
     * @return {@code true} if the object class is serializable, {@code false} otherwise
     */
    boolean isSerializable(final Class<?> clazz);

    /**
     * The default serializability checker.  Returns {@code true} for any class which implements {@link Serializable}.
     */
    SerializabilityChecker DEFAULT = new SerializabilityChecker() {
        public boolean isSerializable(final Class<?> clazz) {
            return Serializable.class.isAssignableFrom(clazz);
        }
    };
}
