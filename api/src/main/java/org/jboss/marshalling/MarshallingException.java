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

import java.io.Serializable;

/**
 * A special {@code Throwable} which holds information about the cause of a marshalling problem.
 */
public final class MarshallingException extends Throwable {

    private static final long serialVersionUID = 7163010203765763875L;

    private Info info = null;

    public static void addFieldInformation(Throwable t, String fieldName) {
        if (t instanceof MarshallingException) {
            final MarshallingException me = (MarshallingException) t;
            final Info oldInfo = me.info;
            me.info = new FieldInfo(oldInfo, fieldName);
        } else {
            Throwable c = t.getCause();
            if (c == null) {
                t.initCause(c = new MarshallingException());
            }
            addObjectInformation(c, fieldName);
        }
    }

    public static void addObjectInformation(Throwable t, Object targetObject) {
        if (t instanceof MarshallingException) {
            final MarshallingException me = (MarshallingException) t;
            final String targetClassName = targetObject.getClass().getName();
            final int targetHashCode = targetObject.hashCode();
            final Info oldInfo = me.info;
            me.info = new ObjectInfo(oldInfo, targetClassName, targetHashCode);
        } else {
            Throwable c = t.getCause();
            if (c == null) {
                t.initCause(c = new MarshallingException());
            }
            addObjectInformation(c, targetObject);
        }
    }

    public Throwable fillInStackTrace() {
        // no operation
        return this;
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("Marshalling exception occurred:");
        info.toString(builder);
        return builder.toString();
    }

    public void setStackTrace(final StackTraceElement[] stackTrace) {
        // nothing
    }

    /**
     * Information about the circumstances surrounding marshalling.
     */
    public static abstract class Info implements Serializable {

        private static final long serialVersionUID = -5600603940887712730L;

        private final Info cause;

        private Info(final Info cause) {
            this.cause = cause;
        }

        public Info getCause() {
            return cause;
        }

        abstract void toString(StringBuilder builder);

        public final String toString() {
            final StringBuilder builder = new StringBuilder(256);
            toString(builder);
            return builder.toString();
        }
    }

    /**
     * Information of an object which was being marshalled at the time an exception occurred.
     */
    public static final class ObjectInfo extends Info implements Serializable {

        private static final long serialVersionUID = -8580895864558204394L;

        private final String targetClassName;
        private final int targetHashCode;

        public ObjectInfo(final Info cause, final String targetClassName, final int targetHashCode) {
            super(cause);
            this.targetClassName = targetClassName;
            this.targetHashCode = targetHashCode;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

        public int getTargetHashCode() {
            return targetHashCode;
        }

        void toString(StringBuilder builder) {
            final Info cause = this.cause;
            if (cause != null) {
                cause.toString(builder);
            }
            builder.append("\n\tfrom object ").append(targetClassName).append('@').append(Integer.toHexString(targetHashCode));
        }
    }

    /**
     * Information of a field which was being marshalled at the time an exception occurred.
     */
    public static final class FieldInfo extends Info implements Serializable {

        private static final long serialVersionUID = -7502908990208699055L;

        private final String fieldName;

        public FieldInfo(final Info cause, final String fieldName) {
            super(cause);
            this.fieldName = fieldName;
        }

        void toString(final StringBuilder builder) {
            final Info cause = this.cause;
            if (cause != null) {
                cause.toString(builder);
            }
            builder.append("\n\t\tfield ").append(fieldName);
        }
    }
}
