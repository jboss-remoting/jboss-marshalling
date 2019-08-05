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

import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

/**
 * A class resolver which uses the context classloader to resolve classes.
 */
public class ContextClassResolver extends AbstractClassResolver {

    private static final PrivilegedAction<ClassLoader> GET_TCCL_ACTION = new PrivilegedAction<ClassLoader>() {
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    };

    /**
     * Construct a new instance.
     */
    public ContextClassResolver() {
    }

    /**
     * Construct a new instance.
     *
     * @param enforceSerialVersionUid {@code true} if an exception should be thrown on an incorrect serialVersionUID
     */
    public ContextClassResolver(final boolean enforceSerialVersionUid) {
        super(enforceSerialVersionUid);
    }

    /** {@inheritDoc} */
    protected ClassLoader getClassLoader() {
        if (getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return doPrivileged(GET_TCCL_ACTION);
        }
    }
}
