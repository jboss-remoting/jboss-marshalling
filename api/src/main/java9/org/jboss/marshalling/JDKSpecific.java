/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.io.OptionalDataException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import sun.reflect.ReflectionFactory;

/**
 */
final class JDKSpecific {
    private JDKSpecific() {}

    private static final ReflectionFactory reflectionFactory = AccessController.doPrivileged(new PrivilegedAction<ReflectionFactory>() {
        public ReflectionFactory run() { return ReflectionFactory.getReflectionFactory(); }
    });

    static OptionalDataException createOptionalDataException(final int length) {
        final OptionalDataException optionalDataException = createOptionalDataException(false);
        optionalDataException.length = length;
        return optionalDataException;
    }

    static OptionalDataException createOptionalDataException(final boolean eof) {
        return reflectionFactory.newOptionalDataExceptionForSerialization(eof);
    }

    private static final StackWalker stackWalker = AccessController.doPrivileged(new PrivilegedAction<StackWalker>() {
        public StackWalker run() {
            return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        }
    });

    private static final Function<Stream<StackWalker.StackFrame>, Class<?>> callerFinder = new Function<Stream<StackWalker.StackFrame>, Class<?>>() {
        public Class<?> apply(final Stream<StackWalker.StackFrame> stream) {
            final Iterator<StackWalker.StackFrame> iterator = stream.iterator();
            StackWalker.StackFrame frame;
            do {
                if (! iterator.hasNext()) {
                    throw new IllegalStateException();
                }
                frame = iterator.next();
            } while (frame.getDeclaringClass() != JDKSpecific.class);
            if (! iterator.hasNext()) {
                throw new IllegalStateException();
            }
            // caller of JDKSpecific.getMyCaller
            iterator.next();
            if (! iterator.hasNext()) {
                throw new IllegalStateException();
            }
            // caller of the caller of JDKSpecific.getMyCaller
            return iterator.next().getDeclaringClass();
        }
    };

    static Class<?> getMyCaller() {
        return stackWalker.walk(callerFinder);
    }
}
