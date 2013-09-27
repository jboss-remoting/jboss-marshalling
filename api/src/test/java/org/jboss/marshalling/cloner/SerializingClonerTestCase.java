/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling.cloner;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import java.util.Date;
import org.jboss.marshalling.Pair;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test
public final class SerializingClonerTestCase {
    public static class ExtTest implements Externalizable {
        private int foo;

        public ExtTest() {
        }

        public ExtTest(final int foo) {
            this.foo = foo;
        }

        public int getFoo() {
            return foo;
        }

        public void setFoo(final int foo) {
            this.foo = foo;
        }

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(foo);
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            foo = in.readInt();
        }

        public boolean equals(final Object obj) {
            return obj instanceof ExtTest && equals((ExtTest) obj);
        }

        public boolean equals(final ExtTest obj) {
            return this == obj || obj != null && obj.foo == foo;
        }

        public int hashCode() {
            return foo;
        }
    }

    public static class ExtTest2 implements Externalizable {
        private int foo;
        private ExtTest one;

        public ExtTest2() {
            one = new ExtTest(1234);
        }

        public ExtTest2(final int foo) {
            this();
            this.foo = foo;
        }

        public int getFoo() {
            return foo;
        }

        public void setFoo(final int foo) {
            this.foo = foo;
        }

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(foo);
            out.writeObject(one);
            out.writeInt(foo);
            out.writeObject(one);
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            foo = in.readInt();
            one = (ExtTest) in.readObject();
            foo = in.readInt();
            one = (ExtTest) in.readObject();
        }

        public boolean equals(final Object obj) {
            return obj instanceof ExtTest2 && equals((ExtTest2) obj);
        }

        public boolean equals(final ExtTest2 obj) {
            return this == obj || obj != null && obj.foo == foo && one == null ? obj.one == null : one.equals(obj.one);
        }

        public int hashCode() {
            return foo + one.hashCode();
        }
    }

    public static final class BigTest implements Serializable {
        private static final long serialVersionUID = -8115739879471991819L;

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            byte[] baImage = new byte[374301];
            out.writeInt(baImage.length);
            out.writeInt(baImage.length);
            out.writeInt(baImage.length);
            out.writeInt(baImage.length);
            out.write(baImage);
            out.writeObject(null);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            int length = in.readInt();
            byte[] baImage = new byte[length];
            int off = 0;
            int len = baImage.length;
            while (len > 0) {
                int n = in.read(baImage, off, len);
                if (n < 0) {
                    throw new EOFException();
                }
                off += n;
                len -= n;
            }
            in.readObject();
        }

        public boolean equals(final Object obj) {
            return obj instanceof BigTest;
        }
    }

    public void testImmutables() throws Throwable {
        final ObjectCloner objectCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(new ClonerConfiguration());
        final Object[] objects = {
                TimeUnit.NANOSECONDS,
                "Bananananana",
                Boolean.TRUE,
                Integer.valueOf(12),
                new String("Something else"),
                new Integer(1234),
                Enum.class,
                Object.class,
                new Object[0],
                RetentionPolicy.RUNTIME,
        };
        // should not clone an immutable JDK class
        for (Object orig : objects) {
            final Object clone = objectCloner.clone(orig);
            assertSame(clone, orig);
        }
    }

    public void testEquals() throws Throwable {
        final ObjectCloner objectCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(new ClonerConfiguration());
        final Object[] objects = {
                Pair.create("First", "Second"),
                Arrays.asList("One", Integer.valueOf(2), Boolean.TRUE, "Shoe"),
                new DateFieldType(new Date(), true),
                new ExtTest(12345),
                new ExtTest2(12345),
                new BigTest(),
        };
        for (Object orig : objects) {
            final Object clone = objectCloner.clone(orig);
            assertEquals(clone, orig);
        }
    }

    public void testRegulars() throws Throwable {
        final ClonerConfiguration configuration = new ClonerConfiguration();
        configuration.setClassCloner(new ClassCloner() {
            public Class<?> clone(final Class<?> original) throws IOException, ClassNotFoundException {
                return original == ClonerTestException.class ? ClonerTestException2.class : original;
            }

            public Class<?> cloneProxy(final Class<?> proxyClass) throws IOException, ClassNotFoundException {
                return proxyClass;
            }
        });
        final ObjectCloner objectCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(configuration);
        assertNotNull(objectCloner.clone(new ClonerTestException("blah")));
    }

    /**
     * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
     */
    @SuppressWarnings("serial")
    static class ClonerTestException extends Exception {

        // no serialVersionUID deliberately

        private String reason;

        public ClonerTestException() {
        }

        public ClonerTestException(final String message) {
            super(message);
            reason = message;
        }
    }

    /**
     * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
     */
    @SuppressWarnings("serial")
    static class ClonerTestException2 extends Exception {

        // no serialVersionUID deliberately

        private String reason;

        public ClonerTestException2() {
        }

        public ClonerTestException2(final String message) {
            super(message);
            reason = message;
        }
    }
}
