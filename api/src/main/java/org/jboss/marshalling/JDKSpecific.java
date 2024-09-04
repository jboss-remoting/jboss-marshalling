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

import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;
import static sun.reflect.ReflectionFactory.getReflectionFactory;

import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.FilterInfo;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import sun.reflect.ReflectionFactory;

/**
 */
final class JDKSpecific {

    private static final Logger LOG = Logger.getLogger(JDKSpecific.class.getName());

    private JDKSpecific() {}

    private static final ReflectionFactory reflectionFactory = getSecurityManager() == null ? getReflectionFactory() : doPrivileged(new PrivilegedAction<ReflectionFactory>() {
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

    private static final StackWalker stackWalker = getSecurityManager() == null
	? StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
	: doPrivileged(new PrivilegedAction<StackWalker>() {
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

    /**
     * Creates an ObjectInputFilter adapter to given UnmarshallingFilter, and sets the filter to given
     * ObjectInputStream.
     * <p>
     * This essentially delegates the filtering functionality to underlying ObjectInputStream.
     *
     * @param ois ObjectInputStream instance to set the filter to.
     * @param filter UnmarshallingFilter instance to delegate filtering decisions to.
     */
    static void setObjectInputStreamFilter(ObjectInputStream ois, UnmarshallingObjectInputFilter filter) {
        LOG.finer(String.format("Setting UnmarshallingFilter %s to ObjectInputStream %s", filter, ois));
        ois.setObjectInputFilter(new ObjectInputFilterAdapter(filter));
    }

    /**
     * Returns an adapter instance for the static JVM-wide deserialization filter (-DserialFilter=...) or null.
     */
    static UnmarshallingObjectInputFilter getJEPS290ProcessWideFilter() {
        ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
        if (serialFilter != null) {
            return new UnmarshallingObjectInputFilterAdapter(serialFilter);
        }
        return null;
    }
}
