/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.reflect.ReflectionFactory;

/**
 * JDK-specific classes which are replaced for different JDK major versions. This one is for Java 8 only.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKSpecific {

    private static final ReflectionFactory reflectionFactory = AccessController.doPrivileged(new PrivilegedAction<ReflectionFactory>() {
        public ReflectionFactory run() { return ReflectionFactory.getReflectionFactory(); }
    });

    static Constructor<?> newConstructorForSerialization(Class<?> classToInstantiate, Constructor<?> constructorToCall) {
        return reflectionFactory.newConstructorForSerialization(classToInstantiate, constructorToCall);
    }

}
