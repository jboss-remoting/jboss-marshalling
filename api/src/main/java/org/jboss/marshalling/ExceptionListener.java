/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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
 * A listener for exceptions which occur during marshalling or unmarshalling.  Not all protocols will support all
 * methods.  These methods are intended for the purpose of interjecting additional debug information into the stack
 * trace by way of the {@link TraceInformation} class.  The appropriate callback will be called in the event of an exception,
 * at every level of recursion into the marshalling or unmarshalling process.
 * @apiviz.exclude
 */
public interface ExceptionListener {

    /**
     * Handle a problem marshalling the given object.
     *
     * @param problem the problem
     * @param subject the object being marshalled
     */
    void handleMarshallingException(Throwable problem, Object subject);

    /**
     * Handle a problem unmarshalling an object of the given class.
     *
     * @param problem the problem
     * @param subjectClass the class being marshalled
     */
    void handleUnmarshallingException(Throwable problem, Class<?> subjectClass);

    /**
     * Handle a problem unmarshalling an object whose class cannot be determined.
     *
     * @param problem the problem
     */
    void handleUnmarshallingException(Throwable problem);

    /**
     * An exception listener which does nothing.
     */
    ExceptionListener NO_OP = new ExceptionListener() {
        public void handleMarshallingException(final Throwable problem, final Object subject) {
        }

        public void handleUnmarshallingException(final Throwable problem, final Class<?> subjectClass) {
        }

        public void handleUnmarshallingException(final Throwable problem) {
        }
    };
}
