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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;

/**
 * JDK-specific classes which are replaced for different JDK major versions.  This one is for Java 16 only.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKSpecific {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

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
            MethodHandle methodHandle = MethodHandles.privateLookupIn(recordObject.getClass(), LOOKUP).findVirtual(
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
            throw new IllegalStateException("Error calling constructor on record class " + recordType, e);
        }
    }
}
