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
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import java.util.Date;
import org.jboss.marshalling.Pair;
import org.testng.SkipException;
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

    @Test
    public void testObjectWithSerializationTimeVariable() throws Throwable {
        final ObjectClonerFactory clonerFactory = ObjectCloners.getSerializingObjectClonerFactory();
        final ClonerConfiguration configuration = new ClonerConfiguration();
        final ObjectCloner cloner = clonerFactory.createCloner(configuration);

        CloneTesterWithSerializeOnlyVariable original = new CloneTesterWithSerializeOnlyVariable("foo");
        CloneTesterWithSerializeOnlyVariable clone = (CloneTesterWithSerializeOnlyVariable) cloner.clone(original);

        assertNull(original.tmp);
        assertEquals(clone.tmp, "foo");
        assertEquals(clone.test, "foo");
    }

    @Test
    public void testObjectWithSelectiveSerialization() throws Throwable {
        final ObjectClonerFactory clonerFactory = ObjectCloners.getSerializingObjectClonerFactory();
        final ClonerConfiguration configuration = new ClonerConfiguration();
        final ObjectCloner cloner = clonerFactory.createCloner(configuration);
        CloneTestWithSelectiveSerialization clone = (CloneTestWithSelectiveSerialization) cloner.clone(new CloneTestWithSelectiveSerialization("foo"));
        assertNull(clone.tmp);
        assertEquals(clone.test, "foo");
    }

    @Test
    public void testRecordCloning() throws Throwable {
        if (Runtime.version().compareTo(Runtime.Version.parse("16")) < 0) {
            throw new SkipException("Skipped when JDK < 16");
        }
        // define a class for it
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        /*  package org.jboss.marshalling.cloner;

            public record MyRecord(String stringVal,
                                   char charVal,
                                   long longVal,
                                   int intVal,
                                   short shortVal,
                                   byte byteVal,
                                   float floatVal,
                                   double doubleVal,
                                   boolean booleanVal
                                   ) {
            } */
        @SuppressWarnings("SpellCheckingInspection")
        byte[] recordBytes = Base64.getDecoder().decode("yv66vgAAADwAaAoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvUmVjb3JkAQAGPGluaXQ+AQADKClWCQAIAAkHAAoMAAsADAEAJW9yZy9qYm9zcy9tYXJzaGFsbGluZy9jbG9uZXIvTXlSZWNvcmQBAAlzdHJpbmdWYWwBABJMamF2YS9sYW5nL1N0cmluZzsJAAgADgwADwAQAQAHY2hhclZhbAEAAUMJAAgAEgwAEwAUAQAHbG9uZ1ZhbAEAAUoJAAgAFgwAFwAYAQAGaW50VmFsAQABSQkACAAaDAAbABwBAAhzaG9ydFZhbAEAAVMJAAgAHgwAHwAgAQAHYnl0ZVZhbAEAAUIJAAgAIgwAIwAkAQAIZmxvYXRWYWwBAAFGCQAIACYMACcAKAEACWRvdWJsZVZhbAEAAUQJAAgAKgwAKwAsAQAKYm9vbGVhblZhbAEAAVoSAAAALgwALwAwAQAIdG9TdHJpbmcBADsoTG9yZy9qYm9zcy9tYXJzaGFsbGluZy9jbG9uZXIvTXlSZWNvcmQ7KUxqYXZhL2xhbmcvU3RyaW5nOxIAAAAyDAAzADQBAAhoYXNoQ29kZQEAKihMb3JnL2pib3NzL21hcnNoYWxsaW5nL2Nsb25lci9NeVJlY29yZDspSRIAAAA2DAA3ADgBAAZlcXVhbHMBADwoTG9yZy9qYm9zcy9tYXJzaGFsbGluZy9jbG9uZXIvTXlSZWNvcmQ7TGphdmEvbGFuZy9PYmplY3Q7KVoBAB0oTGphdmEvbGFuZy9TdHJpbmc7Q0pJU0JGRFopVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBABJMb2NhbFZhcmlhYmxlVGFibGUBAAR0aGlzAQAnTG9yZy9qYm9zcy9tYXJzaGFsbGluZy9jbG9uZXIvTXlSZWNvcmQ7AQAQTWV0aG9kUGFyYW1ldGVycwEAFCgpTGphdmEvbGFuZy9TdHJpbmc7AQADKClJAQAVKExqYXZhL2xhbmcvT2JqZWN0OylaAQABbwEAEkxqYXZhL2xhbmcvT2JqZWN0OwEAAygpQwEAAygpSgEAAygpUwEAAygpQgEAAygpRgEAAygpRAEAAygpWgEAClNvdXJjZUZpbGUBAA1NeVJlY29yZC5qYXZhAQAGUmVjb3JkAQAQQm9vdHN0cmFwTWV0aG9kcw8GAFEK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");
        Class<?> myRecordClass = lookup.defineClass(recordBytes);
        assert myRecordClass.getName().equals("org.jboss.marshalling.cloner.MyRecord");
        Constructor<?> ctor = myRecordClass.getConstructor(String.class, char.class, long.class, int.class, short.class, byte.class, float.class, double.class, boolean.class);
        Object instance = ctor.newInstance("String", 'X', 1234L, 6543, (short) 123, (byte) 12, 1.4f, 2.6, true);
        final ObjectClonerFactory clonerFactory = ObjectCloners.getSerializingObjectClonerFactory();
        final ClonerConfiguration configuration = new ClonerConfiguration();
        final ObjectCloner cloner = clonerFactory.createCloner(configuration);
        Object clone = cloner.clone(instance);
        assertNotNull(clone);
        assertEquals(clone, instance);
    }

    public static class CloneTesterWithSerializeOnlyVariable implements Serializable {
        private String test;
        private String tmp;

        public CloneTesterWithSerializeOnlyVariable() {

        }

        public CloneTesterWithSerializeOnlyVariable(String value) {
            this.test = value;
        }


        // copy the variable 'test' in 'tmp' during serialization. After deserialization both values should be populated.
        private void writeObject(java.io.ObjectOutputStream s) throws IOException {
            try {
                tmp = test;
                s.defaultWriteObject();
            } finally {
                tmp = null;
            }
        }
    }

    public static class CloneTestWithSelectiveSerialization implements Serializable {
        private String test;
        private String tmp;

        public CloneTestWithSelectiveSerialization() {}

        public CloneTestWithSelectiveSerialization(String value) {
            this.test=value;
            this.tmp =value;
        }

        // serialize 'test' field only. After deserialization 'tmp' should be empty
        private void writeObject(java.io.ObjectOutputStream s) throws IOException {
            ObjectOutputStream.PutField putField = s.putFields();
            putField.put("test", this.test);
            s.writeFields();
        }

        private void readObject(java.io.ObjectInputStream s) throws IOException {
            try {
                this.test = (String) s.readFields().get("test", null);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
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
