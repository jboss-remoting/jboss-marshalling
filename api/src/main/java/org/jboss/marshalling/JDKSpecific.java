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

import static java.security.AccessController.doPrivileged;

import java.io.OptionalDataException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 */
final class JDKSpecific {

    private JDKSpecific() {}

    static OptionalDataException createOptionalDataException(final int length) {
        final OptionalDataException optionalDataException = createOptionalDataException();
        optionalDataException.length = length;
        return optionalDataException;
    }

    static OptionalDataException createOptionalDataException(final boolean eof) {
        final OptionalDataException optionalDataException = createOptionalDataException();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final StackTraceElement[] copyStackTrace = new StackTraceElement[stackTrace.length - 1];
        System.arraycopy(stackTrace, 1, copyStackTrace, 0, copyStackTrace.length);
        optionalDataException.setStackTrace(copyStackTrace);
        optionalDataException.eof = eof;
        return optionalDataException;
    }

    static OptionalDataException createOptionalDataException() {
        return doPrivileged(OptionalDataExceptionCreateAction.INSTANCE);
    }

    static Class<?> getMyCaller() {
        return Holder.STACK_TRACE_READER.getClassContext()[3];
    }

    static final class OptionalDataExceptionCreateAction implements PrivilegedAction<OptionalDataException> {

        static final OptionalDataExceptionCreateAction INSTANCE = new OptionalDataExceptionCreateAction();

        private final Constructor<OptionalDataException> constructor;

        OptionalDataExceptionCreateAction() {
            constructor = doPrivileged(new PrivilegedAction<Constructor<OptionalDataException>>() {
                public Constructor<OptionalDataException> run() {
                    try {
                        final Constructor<OptionalDataException> constructor = OptionalDataException.class.getDeclaredConstructor(boolean.class);
                        constructor.setAccessible(true);
                        return constructor;
                    } catch (NoSuchMethodException e) {
                        throw new NoSuchMethodError(e.getMessage());
                    }
                }
            });
        }

        public OptionalDataException run() {
            try {
                return constructor.newInstance(Boolean.FALSE);
            } catch (InstantiationException e) {
                throw new InstantiationError(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Error invoking constructor", e);
            }
        }
    }

    static final class Holder {

        static final Holder.StackTraceReader STACK_TRACE_READER;

        static {
            STACK_TRACE_READER = AccessController.doPrivileged(new PrivilegedAction<Holder.StackTraceReader>() {
                public Holder.StackTraceReader run() {
                    return new Holder.StackTraceReader();
                }
            });
        }

        private Holder() {
        }

        static final class StackTraceReader extends SecurityManager {
            StackTraceReader() {
            }

            protected Class[] getClassContext() {
                return super.getClassContext();
            }
        }
    }
}
