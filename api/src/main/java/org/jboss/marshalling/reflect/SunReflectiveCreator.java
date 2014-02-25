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

package org.jboss.marshalling.reflect;

import java.lang.reflect.Constructor;

/**
 * An object creator that uses methods only found in certain JVMs to create a new constructor if needed.
 *
 * @deprecated This creator is no longer used and will be removed in a future version.
 */
@Deprecated
public class SunReflectiveCreator extends ReflectiveCreator {

    private static SerializableClassRegistry registry = SerializableClassRegistry.getInstanceUnchecked();

    /**
     * {@inheritDoc}  This implementation will attempt to create a new constructor if one is not available.
     */
    protected <T> Constructor<T> getNewConstructor(final Class<T> clazz) {
        return registry.lookup(clazz).getNoInitConstructor();
    }
}
