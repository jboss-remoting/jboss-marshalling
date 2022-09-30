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

import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;
import static sun.reflect.ReflectionFactory.getReflectionFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jboss.marshalling._private.GetReflectionFactoryAction;
import org.jboss.marshalling._private.SetAccessibleAction;
import sun.reflect.ReflectionFactory;

/**
 * JDK-specific classes which are replaced for different JDK major versions. This one is for Java 8 only.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKSpecific {
    private JDKSpecific() {}

    static final SerializableClassRegistry REGISTRY = SerializableClassRegistry.getInstanceUnchecked();

    private static final ReflectionFactory reflectionFactory = getSecurityManager() == null ? getReflectionFactory() : doPrivileged(GetReflectionFactoryAction.INSTANCE);

    static Constructor<?> newConstructorForSerialization(Class<?> classToInstantiate, Constructor<?> constructorToCall) {
        final Constructor<?> serCtor = reflectionFactory.newConstructorForSerialization(classToInstantiate, constructorToCall);
        if (! serCtor.isAccessible()) {
            // older JDK 8...
            if (getSecurityManager() == null) {
                serCtor.setAccessible(true);
            } else {
                doPrivileged(new SetAccessibleAction(serCtor));
            }
        }
        return serCtor;
    }

    /**
     * Return if this class is a record type.
     * @param type The class type
     * @return true if the class is a record, false otherwise
     */
    static boolean isRecord(Class<?> type) {
        // in java previous 16 is false
        return false;
    }

    /**
     * Returns an ordered array of the record components for the given record
     * class.
     * @param type The record class
     * @return The record components array for this class
     */
    static SerializableField.RecordComponent[] getRecordComponents(Class<?> type) {
        throw new UnsupportedOperationException("Records not supported in this version of java");
    }

    /**
     * Retrieves the value of the record component for the given record object.
     * @param recordObject The record Object
     * @param name The record component name
     * @param type The record component class type
     * @return The record component for this record object
     */
    static Object getRecordComponentValue(Object recordObject, String name, Class<?> type) {
        throw new UnsupportedOperationException("Records not supported in this version of java");
    }

    /**
     * Invokes the canonical constructor of a record class with the
     * given argument values.
     * @param recordType The record class
     * @param fields The fields of the record class
     * @param args The arguments for the constructor
     * @return The instantiated object
     */
    static Object invokeRecordCanonicalConstructor(Class<?> recordType, SerializableField[] fields, Object[] args) {
        throw new UnsupportedOperationException("Records not supported in this version of java");
    }

    static final class SerMethods {
        private final Method readObject;
        private final Method readObjectNoData;
        private final Method writeObject;
        private final Method readResolve;
        private final Method writeReplace;
        private final Constructor<?> noArgConstructor;
        private final Constructor<?> objectInputConstructor;

        SerMethods(Class<?> clazz) {
            Method writeObject = null;
            Method readObject = null;
            Method readObjectNoData = null;
            Method writeReplace = null;
            Method readResolve = null;
            for (Method method : clazz.getDeclaredMethods()) {
                final int modifiers = method.getModifiers();
                final String methodName = method.getName();
                final Class<?> methodReturnType = method.getReturnType();
                if (! Modifier.isStatic(modifiers)) {
                    if (Modifier.isPrivate(modifiers) && methodReturnType == void.class) {
                        if (methodName.equals("writeObject")) {
                            final Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 1 && parameterTypes[0] == ObjectOutputStream.class) {
                                writeObject = method;
                                writeObject.setAccessible(true);
                            }
                        } else if (methodName.equals("readObject")) {
                            final Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 1 && parameterTypes[0] == ObjectInputStream.class) {
                                readObject = method;
                                readObject.setAccessible(true);
                            }
                        } else if (methodName.equals("readObjectNoData")) {
                            final Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 0) {
                                readObjectNoData = method;
                                readObjectNoData.setAccessible(true);
                            }
                        }
                    } else if (methodReturnType == Object.class) {
                        // inheritable
                        if (methodName.equals("writeReplace")) {
                            final Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 0) {
                                writeReplace = method;
                                writeReplace.setAccessible(true);
                            }
                        } else if (methodName.equals("readResolve")) {
                            final Class<?>[] parameterTypes = method.getParameterTypes();
                            if (parameterTypes.length == 0) {
                                readResolve = method;
                                readResolve.setAccessible(true);
                            }
                        }
                    }
                }
            }
            if (readResolve == null || writeReplace == null) {
                final Class<?> superclass = clazz.getSuperclass();
                if (superclass != null) {
                    final SerializableClass superInfo = REGISTRY.lookup(superclass);
                    final Method otherReadResolve = superInfo.getSerMethods().readResolve;
                    if (readResolve == null && otherReadResolve != null && ! Modifier.isPrivate(otherReadResolve.getModifiers())) {
                        readResolve = otherReadResolve;
                    }
                    final Method otherWriteReplace = superInfo.getSerMethods().writeReplace;
                    if (writeReplace == null && otherWriteReplace != null && ! Modifier.isPrivate(otherWriteReplace.getModifiers())) {
                        writeReplace = otherWriteReplace;
                    }
                }
            }
            this.readObject = readObject;
            this.readObjectNoData = readObjectNoData;
            this.writeObject = writeObject;
            this.readResolve = readResolve;
            this.writeReplace = writeReplace;
            Constructor<?> noArgConstructor = null;
            Constructor<?> objectInputConstructor = null;
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                final Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 0) {
                    noArgConstructor = constructor;
                    noArgConstructor.setAccessible(true);
                } else if (parameterTypes.length == 1 && parameterTypes[0] == ObjectInput.class) {
                    objectInputConstructor = constructor;
                    objectInputConstructor.setAccessible(true);
                }
            }
            this.noArgConstructor = noArgConstructor;
            this.objectInputConstructor = objectInputConstructor;
        }

        boolean hasWriteObject() {
            return writeObject != null;
        }

        void callWriteObject(Object object, ObjectOutputStream outputStream) throws IOException {
            try {
                writeObject.invoke(object, outputStream);
            } catch (InvocationTargetException e) {
                final Throwable te = e.getTargetException();
                if (te instanceof IOException) {
                    throw (IOException)te;
                } else if (te instanceof RuntimeException) {
                    throw (RuntimeException)te;
                } else if (te instanceof Error) {
                    throw (Error)te;
                } else {
                    throw new IllegalStateException("Unexpected exception", te);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Method is unexpectedly inaccessible");
            }
        }

        boolean hasReadObject() {
            return readObject != null;
        }

        void callReadObject(Object object, ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            try {
                readObject.invoke(object, inputStream);
            } catch (InvocationTargetException e) {
                final Throwable te = e.getTargetException();
                if (te instanceof IOException) {
                    throw (IOException)te;
                } else if (te instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException)te;
                } else if (te instanceof RuntimeException) {
                    throw (RuntimeException)te;
                } else if (te instanceof Error) {
                    throw (Error)te;
                } else {
                    throw new IllegalStateException("Unexpected exception", te);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Method is unexpectedly inaccessible");
            }
        }

        boolean hasReadObjectNoData() {
            return readObjectNoData != null;
        }

        void callReadObjectNoData(Object object) throws ObjectStreamException {
            try {
                readObjectNoData.invoke(object);
            } catch (InvocationTargetException e) {
                final Throwable te = e.getTargetException();
                if (te instanceof ObjectStreamException) {
                    throw (ObjectStreamException)te;
                } else if (te instanceof RuntimeException) {
                    throw (RuntimeException)te;
                } else if (te instanceof Error) {
                    throw (Error)te;
                } else {
                    throw new IllegalStateException("Unexpected exception", te);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Method is unexpectedly inaccessible");
            }
        }

        boolean hasWriteReplace() {
            return writeReplace != null;
        }

        Object callWriteReplace(Object object) throws ObjectStreamException {
            try {
                return writeReplace.invoke(object);
            } catch (InvocationTargetException e) {
                final Throwable te = e.getTargetException();
                if (te instanceof ObjectStreamException) {
                    throw (ObjectStreamException)te;
                } else if (te instanceof RuntimeException) {
                    throw (RuntimeException)te;
                } else if (te instanceof Error) {
                    throw (Error)te;
                } else {
                    throw new IllegalStateException("Unexpected exception", te);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Method is unexpectedly inaccessible");
            }
        }

        boolean hasReadResolve() {
            return readResolve != null;
        }

        Object callReadResolve(Object object) throws ObjectStreamException {
            try {
                return readResolve.invoke(object);
            } catch (InvocationTargetException e) {
                final Throwable te = e.getTargetException();
                if (te instanceof ObjectStreamException) {
                    throw (ObjectStreamException)te;
                } else if (te instanceof RuntimeException) {
                    throw (RuntimeException)te;
                } else if (te instanceof Error) {
                    throw (Error)te;
                } else {
                    throw new IllegalStateException("Unexpected exception", te);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Method is unexpectedly inaccessible");
            }
        }

        Constructor<?> getNoArgConstructor() {
            return noArgConstructor;
        }

        Constructor<?> getObjectInputConstructor() {
            return objectInputConstructor;
        }
    }
}
