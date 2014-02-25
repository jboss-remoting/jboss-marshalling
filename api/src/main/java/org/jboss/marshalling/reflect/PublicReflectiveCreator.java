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

import java.io.InvalidClassException;
import java.lang.reflect.InvocationTargetException;
import org.jboss.marshalling.Creator;

/**
 * A creator that simply uses reflection to locate and invoke a public, zero-argument constructor.
 */
public class PublicReflectiveCreator implements Creator {

    /** {@inheritDoc} */
    public <T> T create(final Class<T> clazz) throws InvalidClassException {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InvocationTargetException e) {
            final InvalidClassException ice = new InvalidClassException(clazz.getName(), "Constructor threw an exception");
            ice.initCause(e);
            throw ice;
        } catch (IllegalAccessException e) {
            throw new InvalidClassException(clazz.getName(), "Illegal access exception occurred accessing the constructor: " + String.valueOf(e));
        } catch (InstantiationException e) {
            throw new InvalidClassException(clazz.getName(), "Instantiation exception: " + String.valueOf(e));
        } catch (NoSuchMethodException e) {
            throw new InvalidClassException(clazz.getName(), "No public, zero-arg constructor");
        }
    }
}