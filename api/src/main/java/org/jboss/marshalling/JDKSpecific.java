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

import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("rawtypes")
final class JDKSpecific {

    private static final Logger LOG = Logger.getLogger(JDKSpecific.class.getName());



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
        return getSecurityManager() == null ? OptionalDataExceptionCreateAction.INSTANCE.run() : doPrivileged(OptionalDataExceptionCreateAction.INSTANCE);
    }

    static Class<?> getMyCaller() {
        return Holder.STACK_TRACE_READER.getClassContext()[3];
    }

    static final class OptionalDataExceptionCreateAction implements PrivilegedAction<OptionalDataException> {

        static final OptionalDataExceptionCreateAction INSTANCE = new OptionalDataExceptionCreateAction();

        private final Constructor<OptionalDataException> constructor;

        OptionalDataExceptionCreateAction() {
            if (getSecurityManager() == null) {
                try {
                    constructor = OptionalDataException.class.getDeclaredConstructor(boolean.class);
                    constructor.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    throw new NoSuchMethodError(e.getMessage());
                }
            } else {
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
            STACK_TRACE_READER = getSecurityManager() == null ? new Holder.StackTraceReader() : doPrivileged(new PrivilegedAction<Holder.StackTraceReader>() {
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

    /**
     * Creates an ObjectInputFilter adapter to given UnmarshallingFilter, and sets the filter to given
     * ObjectInputStream.
     * <p>
     * This essentially delegates the filtering functionality to underlying ObjectInputStream.
     *
     * @param ois ObjectInputStream instance to set the filter to.
     * @param delegate UnmarshallingFilter instance to delegate filtering decisions to.
     */
    static void setObjectInputStreamFilter(final ObjectInputStream ois, final UnmarshallingFilter delegate) {
        try {

            if (_oifReflectionException != null) {
                // Wrap it to get a stack trace and handle in the catch block
                throw new IllegalStateException(_oifReflectionException);
            }

            // Create an ObjectInputFilter instance proxy
            Object objectInputFilterProxy = Proxy.newProxyInstance(null, new Class[]{_ObjectInputFilter}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // This handles invocation of the ObjectInputFilter.checkInput(FilterInfo) method

                    assert "checkInput".equals(method.getName());
                    assert args.length == 1 && args[0] != null;
                    assert _FilterInfo.isAssignableFrom(args[0].getClass());
                    Object filterInfo = args[0];

                    // Obtain the values from the FilterInfo instance
                    final Class<?> serialClass = (Class<?>) _serialClass.invoke(filterInfo);
                    final long arrayLength = (long) _arrayLength.invoke(filterInfo);
                    final long depth = (long) _depth.invoke(filterInfo);
                    final long references = (long) _references.invoke(filterInfo);
                    final long streamBytes = (long) _streamBytes.invoke(filterInfo);

                    // Call the delegate UnmarshallingFilter to make a filtering decision
                    UnmarshallingFilter.FilterResponse response = delegate.checkInput(new UnmarshallingFilter.FilterInput() {
                        @Override
                        public Class<?> getUnmarshalledClass() {
                            return serialClass;
                        }

                        @Override
                        public long getArrayLength() {
                            return arrayLength;
                        }

                        @Override
                        public long getDepth() {
                            return depth;
                        }

                        @Override
                        public long getReferences() {
                            return references;
                        }

                        @Override
                        public long getStreamBytes() {
                            return streamBytes;
                        }
                    });

                    // Convert result UnmarshallingFilter.FilterResponse to ObjectInputFilter.Status and return it
                    Object status;
                    switch (response) {
                        case ACCEPT:
                            status = _allowedResult;
                            break;
                        case REJECT:
                            status = _rejectedResult;
                            break;
                        case UNDECIDED:
                            status = _undecidedResult;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected unmarshalling filter result: " + response);
                    }

                    // status could be an exception if the initial static initializer mapping failed
                    // if so, throw it
                    if (status instanceof Exception) {
                        throw (Exception) status;
                    }
                    return status;
                }
            });

            // Call ObjectInputFilter.Config.setObjectInputFilter(ois, objectInputFilterProxy)
            if (delegate != null) {
                _setObjectInputFilter.invoke(null, ois, objectInputFilterProxy);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> "Unmarshaller failed to set ObjectInputFilter to underlying ObjectInputStream.");
        }
    }

    /**
     * Returns an adapter instance for the static JVM-wide deserialization filter (set via `-Djdk.serialFilter=...`) or null.
     */
    static UnmarshallingFilter getJEPS290ProcessWideFilter() {
        try {

            if (_fiReflectionException != null) {
                // Wrap it to get a stack trace and handle in the catch block
                throw new IllegalStateException(_fiReflectionException);
            }

            // Call sun.misc.ObjectInputFilter.Config.getSerialFilter() to obtain a JVM-wide serial filter
            Object serialFilter = _getSerialFilter.invoke(null);
            // If serial filter is null, return null
            if (serialFilter == null) {
                return null;
            }
            // Return an UnmarshallingFilter instance that delegate decisions to retrieved JVM-wise serial filter
            return new UnmarshallingFilter() {
                @Override
                public FilterResponse checkInput(final FilterInput input) {
                    // Create a FilterInfo proxy instance, which hands over values from given FilterInfo instance
                    Object filterInfo = Proxy.newProxyInstance(null, new Class[]{_FilterInfo}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            assert args == null || args.length == 0;

                            switch (method.getName()) {
                                case "serialClass":
                                    return input.getUnmarshalledClass();
                                case "arrayLength":
                                    return input.getArrayLength();
                                case "depth":
                                    return input.getDepth();
                                case "references":
                                    return input.getReferences();
                                case "streamBytes":
                                    return input.getStreamBytes();
                            }
                            throw new IllegalStateException("Unknown method " + method.getName());
                        }
                    });

                    try {
                        // Call JVM-wise serial filter to make a filtering decision
                        Enum status = (Enum) _checkInput.invoke(serialFilter, filterInfo);
                        // Convert result to a FilterResponse enum
                        switch (status.name()) {
                            case "ALLOWED":
                                return FilterResponse.ACCEPT;
                            case "REJECTED":
                                return FilterResponse.REJECT;
                            case "UNDECIDED":
                                return FilterResponse.UNDECIDED;
                        }
                        throw new IllegalStateException("Unexpected filtering decision: " + status);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to build adapter for the static JVM-wide deserialization filter.", e);
            return null;
        }
    }

    // Statically cached reflection objects used by the JEPS 290 integration

    // These classes are only available in JDK 8, should be available via the boostrap class loader.
    private static final Class<?> _ObjectInputFilter;
    private static final Class<?> _FilterInfo;

    // Getters of the ObjectInputFilter.FilterInfo class
    private static final Method _serialClass;
    private static final Method _arrayLength;
    private static final Method _depth;
    private static final Method _references;
    private static final Method _streamBytes;
    private static final Method _setObjectInputFilter;

    // Values of the ObjectInputFilter.Status enum
    private static final Object _allowedResult;
    private static final Object _rejectedResult;
    private static final Object _undecidedResult;

    // Exception caught when initializing the above fields
    private static final Exception _oifReflectionException;

    // Getter of the ObjectInputFilter.Config class
    private static final Method _getSerialFilter;

    // Method of the ObjectInputFilter class
    private static final Method _checkInput;

    // Exception caught when initializing the above fields
    private static final Exception _fiReflectionException;

    static {
        Exception oifReflectionException = null;
        Exception fiReflectionException = null;
        Class objectInputFilter = null;
        Class filterInfo = null;
        Method serialClass = null;
        Method arrayLength = null;
        Method depth = null;
        Method references = null;
        Method streamBytes = null;
        Method setObjectInputFilter = null;
        Method getSerialFilter = null;
        Method checkInput = null;
        Object allowedResult = null;
        Object rejectedResult = null;
        Object undecidedResult = null;
        try {
            // First load classes used across in multiple methods
            objectInputFilter = Class.forName("sun.misc.ObjectInputFilter");
            Class config = Class.forName("sun.misc.ObjectInputFilter$Config");

            // Next find reflection objects used by setObjectInputStreamFilter
            try {
                filterInfo = Class.forName("sun.misc.ObjectInputFilter$FilterInfo");
                //noinspection unchecked
                Class<Enum> status = (Class<Enum>) Class.forName("sun.misc.ObjectInputFilter$Status");
                //noinspection unchecked
                serialClass = filterInfo.getMethod("serialClass");
                //noinspection unchecked
                arrayLength = filterInfo.getMethod("arrayLength");
                //noinspection unchecked
                depth = filterInfo.getMethod("depth");
                //noinspection unchecked
                references = filterInfo.getMethod("references");
                //noinspection unchecked
                streamBytes = filterInfo.getMethod("streamBytes");
                //noinspection unchecked
                setObjectInputFilter = config.getMethod("setObjectInputFilter", ObjectInputStream.class, objectInputFilter);

                // Convert result UnmarshallingFilter.FilterResponse to ObjectInputFilter.Status and return it
                Map<String, Enum> statusMap = Arrays.stream((status.getEnumConstants()))
                        .collect(Collectors.toMap(Enum::name, c -> c));
                Function<String, Enum> absentExceptionSupplier = name -> {
                    throw new IllegalStateException(String.format("Failed to map FilterResponse %s to ObjectInputFilter.Status", name));
                };
                allowedResult = statusMap.computeIfAbsent("ALLOWED", absentExceptionSupplier);
                rejectedResult = statusMap.computeIfAbsent("REJECTED", absentExceptionSupplier);
                undecidedResult = statusMap.computeIfAbsent("UNDECIDED", absentExceptionSupplier);
            } catch (Exception e) {
                oifReflectionException = e;
            }

            // Next find reflection objects used by getJEPS290ProcessWideFilter
            try {
                //noinspection unchecked
                getSerialFilter = config.getMethod("getSerialFilter");
                //noinspection unchecked
                checkInput = objectInputFilter.getMethod("checkInput", filterInfo);
            } catch (Exception e) {
                fiReflectionException = e;
            }
        } catch (Exception e) {
            oifReflectionException = e;
            fiReflectionException = e;
        }

        _ObjectInputFilter = objectInputFilter;
        _FilterInfo = filterInfo;
        _serialClass = serialClass;
        _arrayLength = arrayLength;
        _depth = depth;
        _references = references;
        _streamBytes = streamBytes;
        _setObjectInputFilter = setObjectInputFilter;
        _getSerialFilter = getSerialFilter;
        _checkInput = checkInput;
        _oifReflectionException = oifReflectionException;
        _allowedResult = allowedResult;
        _rejectedResult = rejectedResult;
        _undecidedResult = undecidedResult;
        _fiReflectionException = fiReflectionException;
    }
}
