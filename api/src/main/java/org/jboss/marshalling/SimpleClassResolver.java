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
 * A class resolver which uses a predefined classloader.
 */
public class SimpleClassResolver extends AbstractClassResolver {
    private final ClassLoader classLoader;

    /**
     * Construct a new instance, specifying a classloader.
     *
     * @param classLoader the classloader to use
     */
    public SimpleClassResolver(final ClassLoader classLoader) {
        this(false, classLoader);
    }

    /**
     * Construct a new instance, specifying a classloader and a flag which determines whether {@code serialVersionUID}
     * matching will be enforced.
     *
     * @param enforceSerialVersionUid {@code true} to throw an exception on unmatched {@code serialVersionUID}
     * @param classLoader the classloader to use
     */
    public SimpleClassResolver(final boolean enforceSerialVersionUid, final ClassLoader classLoader) {
        super(enforceSerialVersionUid);
        this.classLoader = classLoader;
    }

    /** {@inheritDoc} */
    protected ClassLoader getClassLoader() {
        return classLoader;
    }
}
