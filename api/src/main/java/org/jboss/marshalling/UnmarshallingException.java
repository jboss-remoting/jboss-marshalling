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
 * A special {@code Throwable} which holds information about the cause of an unmarshalling problem.
 */
public final class UnmarshallingException extends Throwable {

    private static final long serialVersionUID = 7163010203765763875L;

    private Info info = null;

    /**
     * Add information about an index in an array or collection which was being unmarshalled.
     *
     * @param t the throwable to update
     * @param index the index to log
     * @param size the size, or -1 if unknown
     * @param kind
     */
    public static void addIndexInformation(Throwable t, int index, int size, final IndexInfo.Kind kind) {
        if (t instanceof UnmarshallingException) {
            final UnmarshallingException me = (UnmarshallingException) t;
            final Info oldInfo = me.info;
            me.info = new IndexInfo(oldInfo, index, size, kind);
        } else {
            Throwable c = t.getCause();
            if (c == null) {
                t.initCause(c = new UnmarshallingException());
            }
            addIndexInformation(c, index, size, kind);
        }
    }

    /**
     * Add information about a field which was being unmarshalled.
     *
     * @param t the throwable to update
     * @param fieldName the field name being unmarshalled
     */
    public static void addFieldInformation(Throwable t, String fieldName) {
        if (t instanceof UnmarshallingException) {
            final UnmarshallingException me = (UnmarshallingException) t;
            final Info oldInfo = me.info;
            me.info = new FieldInfo(oldInfo, fieldName);
        } else {
            Throwable c = t.getCause();
            if (c == null) {
                t.initCause(c = new UnmarshallingException());
            }
            addFieldInformation(c, fieldName);
        }
    }

    /**
     * Add information about an object which was being unmarshalled.
     *
     * @param t the throwable to update
     * @param targetClassName the name of the target class which was being unmarshalled
     */
    public static void addObjectInformation(Throwable t, String targetClassName) {
        if (t instanceof UnmarshallingException) {
            final UnmarshallingException me = (UnmarshallingException) t;
            final Info oldInfo = me.info;
            me.info = new ObjectInfo(oldInfo, targetClassName);
        } else {
            Throwable c = t.getCause();
            if (c == null) {
                t.initCause(c = new UnmarshallingException());
            }
            addObjectInformation(c, targetClassName);
        }
    }

    public Throwable fillInStackTrace() {
        // no operation
        return this;
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("an exception while unmarshalling...");
        final Info info = this.info;
        if (info != null) info.toString(builder);
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

        Info(final Info cause) {
            this.cause = cause;
        }

        public Info getCause() {
            return cause;
        }

        void toString(StringBuilder builder) {
            final Info cause = getCause();
            if (cause != null) {
                cause.toString(builder);
            }
        }

        public final String toString() {
            final StringBuilder builder = new StringBuilder(256);
            toString(builder);
            return builder.toString();
        }
    }

    /**
     * Information about an object which was being marshalled at the time an exception occurred.
     */
    public static final class ObjectInfo extends Info implements Serializable {

        private static final long serialVersionUID = -8580895864558204394L;

        private final String targetClassName;

        public ObjectInfo(final Info cause, final String targetClassName) {
            super(cause);
            this.targetClassName = targetClassName;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

        void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\tin object of type ").append(targetClassName);
        }
    }

    /**
     * Information about a field which was being marshalled at the time an exception occurred.
     */
    public static final class FieldInfo extends Info implements Serializable {

        private static final long serialVersionUID = -7502908990208699055L;

        private final String fieldName;

        public FieldInfo(final Info cause, final String fieldName) {
            super(cause);
            this.fieldName = fieldName;
        }

        void toString(final StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\tin field \"").append(fieldName).append('"');
        }
    }

    /**
     * Information about an index in an array or collection.
     */
    public static final class IndexInfo extends Info implements Serializable {

        private static final long serialVersionUID = -5402179533085530855L;

        private final int idx;
        private final int size;
        private final Kind kind;

        public IndexInfo(final Info cause, final int idx, final int size, final Kind kind) {
            super(cause);
            this.idx = idx;
            this.size = size;
            this.kind = kind;
            if (kind == null) {
                throw new NullPointerException("kind is null");
            }
        }

        public enum Kind {
            MAP_KEY,
            MAP_VALUE,
            ELEMENT,
        }

        void toString(final StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\tin ");
            switch (kind) {
                case MAP_KEY: builder.append("map key"); break;
                case MAP_VALUE: builder.append("map value"); break;
                default: builder.append("element"); break;
            }
            builder.append(" at index [").append(idx).append(']');
            if (size >= 0) {
                builder.append(" of size [");
                if (size == Integer.MAX_VALUE) {
                    builder.append("MAX_VALUE");
                } else {
                    builder.append(size);
                }
                builder.append(']');
            }
        }
    }
}