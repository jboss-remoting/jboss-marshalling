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
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * A serialiable pair of values.  There is also a specified externalizer as well, to support more efficient I/O.
 *
 * @param <A> the first value type
 * @param <B> the second value type
 */
@Externalize(Pair.Externalizer.class)
public final class Pair<A, B> implements Serializable {

    private static final long serialVersionUID = 7331975617882473974L;

    private final A a;
    private final B b;

    /**
     * Create a new instance.
     *
     * @param a the first value
     * @param b the second value
     */
    public Pair(final A a, final B b) {
        this.a = a;
        this.b = b;
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
     * Create a new instance.
     *
     * @param a the first value
     * @param b the second value
     * @param <A> the first value type
     * @param <B> the second value type
     * @return the new instance
     */
    public static <A, B> Pair<A, B> create(A a, B b) {
        return new Pair<A,B>(a, b);
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
