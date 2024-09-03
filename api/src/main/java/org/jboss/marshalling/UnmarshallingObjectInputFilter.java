/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

/**
 * Filter classes, array lengths, and graph metrics during deserialization.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@FunctionalInterface
public interface UnmarshallingObjectInputFilter {

    /**
     * The status of a check on the class, array length, number of references,
     * depth, and stream size.
     */
    enum Status {
        /**
         * The status is allowed.
         */
        ALLOWED,
        /**
         * The status is rejected.
         */
        REJECTED,
        /**
         * The status is undecided, not allowed and not rejected.
         */
        UNDECIDED
    }

    /**
     * FilterInfo provides access to information about the current object
     * being deserialized and the status of the {@link Unmarshaller}
     */
    interface FilterInfo {

        /**
         * Gets the class of an object being deserialized.
         * For arrays, it is the array type.
         * For example, the array class name of a 2 dimensional array of strings is
         * "{@code [[Ljava.lang.String;}".
         * To check the array's element type, iteratively use
         * {@link Class#getComponentType() Class.getComponentType} while the result
         * is an array and then check the class.
         * The {@code serialClass is null} in the case where a new object is not being
         * created and to give the filter a chance to check the depth, number of
         * references to existing objects, and the stream size.
         *
         * @return class of an object being deserialized; may be null
         */
        Class<?> getUnmarshalledClass();

        /**
         * Returns the number of array elements when deserializing an array of the class.
         *
         * @return the non-negative number of array elements when deserializing
         * an array of the class, otherwise -1
         */
        long getArrayLength();

        /**
         * Returns the current depth.
         * The depth starts at {@code 1} and increases for each nested object and
         * decrements when each nested object returns.
         *
         * @return the current depth
         */
        long getDepth();

        /**
         * Returns the current number of object references.
         *
         * @return the non-negative current number of object references
         */
        long getReferences();

        /**
         * Returns the current number of bytes consumed.
         *
         * @return the non-negative current number of bytes consumed
         */
        long getStreamBytes();

    }

    /**
     * Check the class, array length, number of object references, depth,
     * stream size, and other available filtering information.
     * Implementations of this method check the contents of the object graph being created
     * during deserialization. The filter returns {@link Status#ALLOWED FilterResponse.ACCEPT},
     * {@link Status#REJECTED FilterResponse.REJECT}, or {@link Status#UNDECIDED FilterResponse.UNDECIDED}.
     *
     * @param filterInfo provides information about the current object being deserialized,
     *             if any, and the status of the {@link Unmarshaller}
     * @return  {@link Status#ALLOWED Status.ACCEPT} if accepted,
     *          {@link Status#REJECTED Status.REJECT} if rejected,
     *          {@link Status#UNDECIDED Status.UNDECIDED} if undecided.
     */
    Status checkInput(FilterInfo filterInfo);

    /**
     * Unmarshalling filter that accepts everything.
     */
    UnmarshallingObjectInputFilter ACCEPTING = input -> Status.ALLOWED;
    /**
     * Unmarshalling filter that rejects everything.
     */
    UnmarshallingObjectInputFilter REJECTING = input -> Status.REJECTED;

    /**
     * Factory for creating unmarshalling filters that are configured by a JEPS 290 style {@code filterSpec}
     * string provided to the factory methods. The created unmarshalling filters will return
     * {@link Status#REJECTED} if the given input is not acceptable.
     */
    final class Factory {

        /**
         * Forbid instantiation.
         */
        private Factory() {
        }

        /**
         * Creates unmarshalling filters that are configured by a JEPS 290 style {@code filterSpec} string.
         * @param filterSpec the JEPS 290 style compatible string
         * @return filterSpec string configured unmarshalling filter
         */
        public static UnmarshallingObjectInputFilter createFilter(final String filterSpec) {
            return createFilter(filterSpec, false);
        }

        /**
         * Creates unmarshalling filters that are configured by a JEPS 290 style {@code filterSpec} string.
         * If checkJEPS290ProcessFilter is set to true and static JVM-wide deserialization filter (-DserialFilter=...) is available
         * then both configured unmarshalling filter and JVM-wide deserialization filter will be chained.
         * @param filterSpec the JEPS 290 style compatible string
         * @return filterSpec string configured unmarshalling filter potentially chained with JVM-wide deserialization filter
         */
        public static UnmarshallingObjectInputFilter createFilter(final String filterSpec, boolean checkJEPS290ProcessFilter) {
            UnmarshallingObjectInputFilter result = new UnmarshallingObjectInputFilterImpl(filterSpec);
            if (checkJEPS290ProcessFilter) {
                UnmarshallingObjectInputFilter processWide = JDKSpecific.getJEPS290ProcessWideFilter();
                if (processWide != null) {
                    result = new ChainedUnmarshallingObjectInputFilter(processWide, result);
                }
            }
            return result;
        }
    }

}
