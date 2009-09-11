/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.marshalling;

/**
 * A listener for exceptions which occur during marshalling or unmarshalling.  Not all protocols will support all
 * methods.  These methods are intended for the purpose of interjecting additional debug information into the stack
 * trace by way of the {@link TraceInformation} class.  The appropriate callback will be called in the event of an exception,
 * at every level of recursion into the marshalling or unmarshalling process.
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
