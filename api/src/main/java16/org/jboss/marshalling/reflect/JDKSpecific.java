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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedAction;
import sun.reflect.ReflectionFactory;

/**
 * JDK-specific classes which are replaced for different JDK major versions.  This one is for Java 16 only.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKSpecific {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ReflectionFactory reflectionFactory = getSecurityManager() == null ? getReflectionFactory() : doPrivileged(new PrivilegedAction<ReflectionFactory>() {
        public ReflectionFactory run() { return ReflectionFactory.getReflectionFactory(); }
    });

    /**
     * Return if this class is a record type.
     * @param type The class type
     * @return true if the class is a record, false otherwise
     */
    static boolean isRecord(Class<?> type) {
        return type.isRecord();
    }

    /**
     * Returns an ordered array of the record components for the given record
     * class.
     * @param type The record class
     * @return The record components array for this class
     */
    static SerializableField.RecordComponent[] getRecordComponents(Class<?> type) {
        RecordComponent[] recordComponents = type.getRecordComponents();
        SerializableField.RecordComponent[] result = new SerializableField.RecordComponent[recordComponents.length];
        for (int i = 0; i < recordComponents.length; i++) {
            result[i] = new SerializableField.RecordComponent(recordComponents[i].getName(), recordComponents[i].getType(), i);
        }
        return result;
    }

    /**
     * Retrieves the value of the record component for the given record object.
     * @param recordObject The record Object
     * @param name The record component name
     * @param type The record component class type
     * @return The record component for this record object
     */
    static Object getRecordComponentValue(Object recordObject, String name, Class<?> type) {
        try {
            MethodHandle methodHandle = LOOKUP.findVirtual(
                    recordObject.getClass(), name, MethodType.methodType(type));
            return (Object) methodHandle.invoke(recordObject);
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot invoke record getter " + name + " in object " + recordObject, e);
        }
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
        try {
            Class<?>[] paramTypes = new Class<?>[fields.length];
            for (SerializableField field : fields) {
                paramTypes[field.getRecordComponentIndex()] = field.getType();
            }

            MethodHandle constructorHandle = MethodHandles.privateLookupIn(recordType, LOOKUP).findConstructor(recordType, MethodType.methodType(void.class, paramTypes))
                    .asType(MethodType.methodType(Object.class, paramTypes));
            return constructorHandle.invokeWithArguments(args);
        } catch (Throwable e) {
            throw new IllegalStateException("Error calling onstructor on record class " + recordType, e);
        }
    }

    static Constructor<?> newConstructorForSerialization(Class<?> classToInstantiate, Constructor<?> constructorToCall) {
        return reflectionFactory.newConstructorForSerialization(classToInstantiate, constructorToCall);
    }

    static final class SerMethods {
        private final MethodHandle readObject;
        private final MethodHandle readObjectNoData;
        private final MethodHandle writeObject;
        private final MethodHandle readResolve;
        private final MethodHandle writeReplace;
        private final Constructor<?> noArgConstructor;
        private final Constructor<?> objectInputConstructor;

        SerMethods(Class<?> clazz) {
            readObject = reflectionFactory.readObjectForSerialization(clazz);
            readObjectNoData = reflectionFactory.readObjectNoDataForSerialization(clazz);
            writeObject = reflectionFactory.writeObjectForSerialization(clazz);
            readResolve = reflectionFactory.readResolveForSerialization(clazz);
            writeReplace = reflectionFactory.writeReplaceForSerialization(clazz);
            Constructor<?> ctor;
            Constructor<?> noArgConstructor = null;
            try {
                ctor = clazz.getDeclaredConstructor();
                noArgConstructor = reflectionFactory.newConstructorForSerialization(clazz, ctor);
            } catch (NoSuchMethodException ignored) {
            }
            this.noArgConstructor = noArgConstructor;
            Constructor<?> objectInputConstructor = null;
            try {
                ctor = clazz.getDeclaredConstructor(ObjectInput.class);
                objectInputConstructor = reflectionFactory.newConstructorForSerialization(clazz, ctor);
            } catch (NoSuchMethodException ignored) {
            }
            this.objectInputConstructor = objectInputConstructor;
        }

        boolean hasWriteObject() {
            return writeObject != null;
        }

        void callWriteObject(Object object, ObjectOutputStream outputStream) throws IOException {
            try {
                writeObject.invoke(object, outputStream);
            } catch (IOException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        boolean hasReadObject() {
            return readObject != null;
        }

        void callReadObject(Object object, ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
            try {
                readObject.invoke(object, inputStream);
            } catch (IOException | ClassNotFoundException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        boolean hasReadObjectNoData() {
            return readObjectNoData != null;
        }

        void callReadObjectNoData(Object object) throws ObjectStreamException {
            try {
                readObjectNoData.invoke(object);
            } catch (ObjectStreamException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        boolean hasWriteReplace() {
            return writeReplace != null;
        }

        Object callWriteReplace(Object object) throws ObjectStreamException {
            try {
                return writeReplace.invoke(object);
            } catch (ObjectStreamException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        boolean hasReadResolve() {
            return readResolve != null;
        }

        Object callReadResolve(Object object) throws ObjectStreamException {
            try {
                return readResolve.invoke(object);
            } catch (ObjectStreamException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
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
