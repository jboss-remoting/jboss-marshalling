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

import java.io.Serializable;

/**
 * A facility available to marshalling implementations which allows for detailed stack traces which trace
 * the position in the object graph where a marshalling or unmarshalling problem has occurred.
 *
 * @apiviz.exclude
 */
public final class TraceInformation extends Throwable {

    private static final long serialVersionUID = 9017837359853233891L;

    Info info = null;

    TraceInformation() {
    }

    public Throwable fillInStackTrace() {
        // no operation
        return this;
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("an exception which occurred:");
        final Info info = this.info;
        if (info != null) info.toString(builder);
        return builder.toString();
    }

    public void setStackTrace(final StackTraceElement[] stackTrace) {
        // nothing
    }

    private static TraceInformation getOrAddTraceInformation(Throwable t) {
        if (t == null) {
            throw new NullPointerException("t is null");
        }
        Throwable c;
        while (! (t instanceof TraceInformation)) {
            c = t.getCause();
            if (c == null) try {
                t.initCause(c = new TraceInformation());
            } catch (RuntimeException e) {
                // ignored
            }
            t = c;
        }
        return (TraceInformation) t;
    }

    private static String getNiceClassName(Class<?> clazz) {
        if (clazz.isArray()) {
            return "(array of " + getNiceClassName(clazz.getComponentType()) + ")";
        } else if (clazz.isEnum() && clazz.getSuperclass() != Enum.class) {
            return getNiceClassName(clazz.getSuperclass());
        } else {
            return clazz.getName();
        }
    }

    /**
     * Add user information about problem with marshalling or unmarshalling.
     *
     * @param t the throwable to update
     * @param data the user data
     */
    public static void addUserInformation(Throwable t, Serializable data) {
        final TraceInformation ti = getOrAddTraceInformation(t);
        final Info oldInfo = ti.info;
        ti.info = new UserInfo(oldInfo, data);
    }

    /**
     * Add information about a field which was being marshalled.
     *
     * @param t the throwable to update
     * @param fieldName the field name being (un-)marshalled
     */
    public static void addFieldInformation(Throwable t, String fieldName) {
        final TraceInformation ti = getOrAddTraceInformation(t);
        final Info oldInfo = ti.info;
        ti.info = new FieldInfo(oldInfo, fieldName);
    }

    /**
     * Add information about an object which was being (un-)marshalled.
     *
     * @param t the throwable to update
     * @param targetObject the target object which was being (un-)marshalled
     */
    public static void addObjectInformation(Throwable t, Object targetObject) {
        final TraceInformation ti = getOrAddTraceInformation(t);
        final String targetClassName = getNiceClassName(targetObject.getClass());
        int targetHashCode = 0;
        try {
            targetHashCode = targetObject.hashCode();
        } catch (Throwable ignored) {
            // guess we won't know the hash code!
        }
        final Info oldInfo = ti.info;
        ti.info = new ObjectInfo(oldInfo, targetClassName, targetHashCode);
    }

    /**
     * Add information about an incomplete object which was being unmarshalled.
     *
     * @param t the throwable to update
     * @param targetClass the class of the target object being unmarshalled
     */
    public static void addIncompleteObjectInformation(Throwable t, Class<?> targetClass) {
        addIncompleteObjectInformation(t, getNiceClassName(targetClass));
    }

    /**
     * Add information about an incomplete object which was being unmarshalled.
     *
     * @param t the throwable to update
     * @param targetClassName the class of the target object being unmarshalled
     */
    public static void addIncompleteObjectInformation(Throwable t, String targetClassName) {
        final TraceInformation ti = getOrAddTraceInformation(t);
        final Info oldInfo = ti.info;
        ti.info = new IncompleteObjectInfo(oldInfo, targetClassName);
    }

    /**
     * Add information about an index into a collection which was being (un-)marshalled.
     *
     * @param t the throwable to update
     * @param index the index of the element in question
     * @param size the size of the collection in question
     * @param kind the type of element being processed
     */
    public static void addIndexInformation(Throwable t, int index, int size, IndexType kind) {
        final TraceInformation ti = getOrAddTraceInformation(t);
        final Info oldInfo = ti.info;
        ti.info = new IndexInfo(oldInfo, index, size, kind);
    }

    /**
     * Information about the circumstances surrounding (un)marshalling.
     * @apiviz.exclude
     */
    public abstract static class Info implements Serializable {

        private static final long serialVersionUID = -5600603940887712730L;

        private final Info cause;

        Info(final Info cause) {
            this.cause = cause;
        }

        public Info getCause() {
            return cause;
        }

        protected void toString(StringBuilder builder) {
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
     * Information specific to a method execution.
     * @apiviz.exclude
     */
    public static final class MethodInfo extends Info implements Serializable {

        private static final long serialVersionUID = 646518183715279704L;

        /**
         * The type of method being executed.
         * @apiviz.exclude
         */
        public enum Type {
            READ_OBJECT,
            READ_OBJECT_NO_DATA,
            WRITE_OBJECT,
            EXTERNALIZABLE_READ_OBJECT,
            EXTERNALIZABLE_WRITE_OBJECT,
            EXTERNALIZER_READ_OBJECT,
            EXTERNALIZER_WRITE_OBJECT,
            EXTERNALIZER_CREATE_OBJECT,
        }

        MethodInfo(final Info cause) {
            super(cause);
        }
    }

    /**
     * Information about an object which was being (un-)marshalled at the time an exception occurred.
     * @apiviz.exclude
     */
    public static final class ObjectInfo extends Info implements Serializable {

        private static final long serialVersionUID = -8580895864558204394L;

        private final String targetClassName;
        private final int targetHashCode;

        ObjectInfo(final Info cause, final String targetClassName, final int targetHashCode) {
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

        protected void toString(final StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\tin object ").append(targetClassName).append('@').append(Integer.toHexString(targetHashCode));
        }
    }

    /**
     * Information about a class which was being (un-)marshalled at the time an exception occurred.
     * @apiviz.exclude
     */
    public static final class ClassInfo extends Info implements Serializable {

        private static final long serialVersionUID = -8580895864558204394L;

        private final String targetClassName;

        ClassInfo(final Info cause, final String targetClassName) {
            super(cause);
            this.targetClassName = targetClassName;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

        protected void toString(final StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\tin class ").append(targetClassName);
        }
    }

    /**
     * Information about an incomplete object being unmarshalled.
     * @apiviz.exclude
     */
    public static final class IncompleteObjectInfo extends Info implements Serializable {

        private static final long serialVersionUID = -8580895864558204394L;

        private final String targetClassName;

        public IncompleteObjectInfo(final Info cause, final String targetClassName) {
            super(cause);
            this.targetClassName = targetClassName;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\tin object of type ").append(targetClassName);
        }

    }

    /**
     * Information about a field which was being marshalled at the time an exception occurred.
     * @apiviz.exclude
     */
    public static final class FieldInfo extends Info implements Serializable {

        private static final long serialVersionUID = -7502908990208699055L;

        private final String fieldName;

        FieldInfo(final Info cause, final String fieldName) {
            super(cause);
            this.fieldName = fieldName;
        }

        protected void toString(final StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\tin field ").append(fieldName);
        }
    }

    /**
     * Information about an index in an array or collection.
     * @apiviz.exclude
     */
    public static final class IndexInfo extends Info implements Serializable {

        private static final long serialVersionUID = -5402179533085530855L;

        private final int idx;
        private final int size;
        private final IndexType kind;

        IndexInfo(final Info cause, final int idx, final int size, final IndexType kind) {
            super(cause);
            this.idx = idx;
            this.size = size;
            this.kind = kind;
            if (kind == null) {
                throw new NullPointerException("kind is null");
            }
        }

        protected void toString(final StringBuilder builder) {
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

    /**
     * User information.
     * @apiviz.exclude
     */
    public static final class UserInfo extends Info implements Serializable {

        private static final long serialVersionUID = 5504303963528386454L;

        private final Serializable data;

        UserInfo(final Info cause, final Serializable data) {
            super(cause);
            this.data = data;
        }

        protected void toString(final StringBuilder builder) {
            super.toString(builder);
            builder.append("\n\t\t-> ").append(data);
        }
    }

    /**
     * The type of index for a multi-valued collection or map.
     * @apiviz.exclude
     */
    public enum IndexType {
        MAP_KEY,
        MAP_VALUE,
        ELEMENT,
    }
}
