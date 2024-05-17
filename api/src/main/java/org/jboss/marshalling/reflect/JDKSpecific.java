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

/**
 * JDK-specific classes which are replaced for different JDK major versions.  This one is for Java 9 only.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKSpecific {

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
     * @throws java.io.IOException Some error
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
     * @throws java.io.IOException
     */
    static Object invokeRecordCanonicalConstructor(Class<?> recordType, SerializableField[] fields, Object[] args) {
        throw new UnsupportedOperationException("Records not supported in this version of java");
    }
}
