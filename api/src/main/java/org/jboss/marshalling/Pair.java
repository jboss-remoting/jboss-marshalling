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

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * A serializable pair of values.  There is also a specified externalizer as well, to support more efficient I/O.
 *
 * @param <A> the first value type
 * @param <B> the second value type
 */
@Externalize(Pair.Externalizer.class)
public final class Pair<A, B> implements Serializable {

    private static final long serialVersionUID = 7331975617882473974L;

    private final A a;
    private final B b;
    @SuppressWarnings({ "InstanceVariableMayNotBeInitializedByReadObject" })
    private final transient int hashCode;

    private static final FieldSetter setter = FieldSetter.get(Pair.class, "hashCode");

    /**
     * Create a new instance.
     *
     * @param a the first value
     * @param b the second value
     */
    public Pair(final A a, final B b) {
        this.a = a;
        this.b = b;
        hashCode = hashCode(a, b);
    }

    /**
     * Calculate the combined hash code of two objects.
     *
     * @param a the first object
     * @param b the second object
     * @return the combined hash code
     */
    private static int hashCode(final Object a, final Object b) {
        return (a == null ? 0 : a.hashCode()) * 1319 + (b == null ? 0 : b.hashCode());
    }

    /**
     * Get the first value.
     *
     * @return the first value
     */
    public A getA() {
        return a;
    }

    /**
     * Get the second value.
     *
     * @return the second value
     */
    public B getB() {
        return b;
    }

    /**
     * Reinitialize the object and set the hash code.
     *
     * @param ois the object input stream
     * @throws IOException if an I/O error occurs while reading the default fields
     * @throws ClassNotFoundException if a class isn't found while reading the default fields
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        setter.setInt(this, hashCode(a, b));
    }

    /**
     * Return the combined hash code of the two argument objects.
     *
     * @return the combined hash code
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * Determine if this pair equals another.  A pair is equal to another pair if both members are equal.
     *
     * @param other the other pair
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Object other) {
        return (other instanceof Pair) ? equals((Pair) other) : false;
    }

    /**
     * Determine if this pair equals another.  A pair is equal to another pair if both members are equal.
     *
     * @param other the other pair
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Pair<?, ?> other) {
        if (other == null) {
            return false;
        }
        final Object a = this.a;
        final Object othera = other.a;
        final Object b = this.b;
        final Object otherb = other.b;
        return (a == othera || a != null && a.equals(othera)) &&
                (b == otherb || b != null && b.equals(otherb));
    }

    /**
     * Get a string representation of this pair.
     *
     * @return the string representation
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Pair (").append(a).append(", ").append(b).append(')');
        return builder.toString();
    }

    /**
     * Create a new instance.
     *
     * @param a the first value
     * @param b the second value
     * @param <A> the first value type
     * @param <B> the second value type
     * @return the new instance
     */
    public static <A, B> Pair<A, B> create(A a, B b) {
        return new Pair<A, B>(a, b);
    }

    /**
     * An externalizer for {@link Pair} instances.
     */
    public static final class Externalizer implements org.jboss.marshalling.Externalizer, Externalizable {

        private static final long serialVersionUID = 930391108343329811L;
        private static final Externalizer instance = new Externalizer();

        /**
         * Get the single instance.  Though multiple instances can be created using the public constructor, only a single
         * instance need be used in practice since this class maintains no internal state.
         *
         * @return the instance
         */
        public static Externalizer getInstance() {
            return instance;
        }

        /** {@inheritDoc} */
        public void writeExternal(final Object subject, final ObjectOutput output) throws IOException {
            output.writeObject(((Pair<?, ?>)subject).a);
            output.writeObject(((Pair<?, ?>)subject).b);
        }

        /** {@inheritDoc} */
        public Object createExternal(final Class<?> subjectType, final ObjectInput input, final Creator defaultCreator) throws IOException, ClassNotFoundException {
            return create(input.readObject(), input.readObject());
        }

        /** {@inheritDoc} */
        public void readExternal(final Object subject, final ObjectInput input) {
        }

        /** {@inheritDoc} */
        public void writeExternal(final ObjectOutput out) {
        }

        /** {@inheritDoc} */
        public void readExternal(final ObjectInput in) {
        }

        /**
         * Resolve the object to the single externalizer instance.
         *
         * @return the instance
         */
        protected Object readResolve() {
            return instance;
        }
    }
}
