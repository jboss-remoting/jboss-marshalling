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

package org.jboss.test.marshalling;

import static org.testng.AssertJUnit.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.marshalling.AnnotationClassExternalizerFactory;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.ClassExternalizerFactory;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalize;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.FieldSetter;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ObjectInputStreamUnmarshaller;
import org.jboss.marshalling.ObjectOutputStreamMarshaller;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.ReflectiveCreator;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import org.jboss.marshalling.river.RiverMarshaller;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.marshalling.river.RiverUnmarshaller;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 *
 */
@SuppressWarnings("ALL")
public final class SimpleMarshallerTests extends TestBase {

    public SimpleMarshallerTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        super(testMarshallerProvider, testUnmarshallerProvider, configuration);
    }

    /**
     * Simple constructor for running one test at a time from an IDE.
     */
    public SimpleMarshallerTests() {
        super(new MarshallerFactoryTestMarshallerProvider(new RiverMarshallerFactory(), 3),
              new MarshallerFactoryTestUnmarshallerProvider(new RiverMarshallerFactory(), 3),
              getOneTestMarshallingConfiguration());
    }

    private static MarshallingConfiguration getOneTestMarshallingConfiguration() {
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setExternalizerCreator(new ReflectiveCreator());
        marshallingConfiguration.setSerializedCreator(new SunReflectiveCreator());
        marshallingConfiguration.setVersion(3);
        return marshallingConfiguration;
    }

    @Test
    public void testNull() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(null);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertNull(unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testUnserializable() throws Throwable {
        try {
            runReadWriteTest(new ReadWriteTest() {
                public void runWrite(final Marshaller marshaller) throws Throwable {
                    marshaller.writeObject(new Object());
                }

                public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                }
            });
        } catch (NotSerializableException e) {
            // ok
            return;
        }
        fail("Missing exception");
    }

    public static final class SerializableWithFinalFields implements Serializable {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings({ "FieldMayBeStatic" })
        private final String blah = "Blah blah!";

        public String getBlah() {
            return blah;
        }
    }

    @Test
    public void testSerializableWithFinalFields() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                final SerializableWithFinalFields obj = new SerializableWithFinalFields();
                marshaller.writeObject(obj);
                marshaller.writeObject(obj);
                final SerializableWithFinalFields obj2 = new SerializableWithFinalFields();
                marshaller.writeObjectUnshared(obj2);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final SerializableWithFinalFields obj = (SerializableWithFinalFields) unmarshaller.readObject();
                assertSame(obj, unmarshaller.readObject());
                final SerializableWithFinalFields obj2 = (SerializableWithFinalFields) unmarshaller.readObjectUnshared();
                assertEquals("Blah blah!", obj2.blah);
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestSerializable implements Serializable {

        private static final long serialVersionUID = -3834685845327229499L;

        private int first = 1234;
        private float second = 13.725f;
        private boolean argh = true;
        private byte[] third = new byte[] { 15, 2, 3 };
        private Object zap = Float.valueOf(5.5f);
        private Void foo = null;

        @SuppressWarnings({ "NonFinalFieldReferenceInEquals" })
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final TestSerializable that = (TestSerializable) o;
            if (argh != that.argh) return false;
            if (first != that.first) return false;
            if (Float.compare(that.second, second) != 0) return false;
            if (foo != null ? !foo.equals(that.foo) : that.foo != null) return false;
            if (!Arrays.equals(third, that.third)) return false;
            if (!zap.equals(that.zap)) return false;
            return true;
        }

        public int hashCode() {
            return 0;
        }
        //        private void writeObject(ObjectOutputStream oos) throws IOException {
        //            final ObjectOutputStream.PutField field = oos.putFields();
        //            field.put("first", first);
        //            field.put("second", second);
        //            field.put("argh", argh);
        //            field.put("third", third);
        //            field.put("zap", zap);
        //            field.put("foo", foo);
        //            oos.writeFields();
        //        }
    }

    @Test
    public void testSimple() throws Throwable {
        final Serializable serializable = new TestSerializable();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestSerializableNoWriteObjectNoReadObject extends TestSerializable {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double fourth = 1.23;
    }

    @Test
    public void testSerializableNoWriteObjectNoReadObject() throws Throwable {
        final Serializable serializable = new TestSerializableNoWriteObjectDefaultReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestSerializableNoWriteObjectDefaultReadObject extends TestSerializable {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double fourth = 1.23;

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
        }
    }

    @Test
    public void testSerializableNoWriteObjectDefaultReadObject() throws Throwable {
        final Serializable serializable = new TestSerializableNoWriteObjectDefaultReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestSerializableNoWriteObjectGetFieldsReadObject extends TestSerializable {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double fifth = 2.34;

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField getField = ois.readFields();
            fifth = (double) getField.get("fifth", 2.34);
        }
    }

    @Test
    public void testSerializableNoWriteObjectGetFieldsReadObject() throws Throwable {
        final Serializable serializable = new TestSerializableNoWriteObjectGetFieldsReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestSerializableDefaultWriteObjectNoReadObject extends TestSerializable {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double sixth = 3.45;

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }
    }

    @Test
    public void testSerializableDefaultWriteObjectNoReadObject() throws Throwable {
        final Serializable serializable = new TestSerializableDefaultWriteObjectNoReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableDefaultWriteObjectDefaultReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double seventh = 4.56;

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
        }
    }

    @Test
    public void testSerializableDefaultWriteObjectDefaultReadObject() throws Throwable {
        final Serializable serializable = new TestSerializableDefaultWriteObjectDefaultReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableEmptyWriteObjectDefaultReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double seventh = 4.56;

        private void writeObject(ObjectOutputStream oos) throws IOException {
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
        }
    }

    @Test
    public void testSerializableEmptyWriteObjectDefaultReadObject() throws Throwable {
        final TestSerializableEmptyWriteObjectDefaultReadObject serializable = new TestSerializableEmptyWriteObjectDefaultReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (! (marshaller instanceof RiverMarshaller)) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableEmptyWriteObjectDefaultReadObject o = unmarshaller.readObject(TestSerializableEmptyWriteObjectDefaultReadObject.class);
                assertEquals(serializable.sixth, o.sixth);
                assertEquals(0.0, o.seventh);
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableEmptyWriteObjectNoReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double seventh = 4.56;

        private void writeObject(ObjectOutputStream oos) throws IOException {
        }
    }

    @Test
    public void testSerializableEmptyWriteObjectNoReadObject() throws Throwable {
        final TestSerializableEmptyWriteObjectNoReadObject serializable = new TestSerializableEmptyWriteObjectNoReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (! (marshaller instanceof RiverMarshaller)) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableEmptyWriteObjectNoReadObject o = unmarshaller.readObject(TestSerializableEmptyWriteObjectNoReadObject.class);
                assertEquals(serializable.sixth, o.sixth);
                assertEquals(0.0, o.seventh);
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableEmptyWriteObjectEmptyReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double seventh = 4.56;

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
        }
    }

    @Test
    public void testSerializableEmptyWriteObjectEmptyReadObject() throws Throwable {
        final TestSerializableEmptyWriteObjectEmptyReadObject serializable = new TestSerializableEmptyWriteObjectEmptyReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (! (marshaller instanceof RiverMarshaller)) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableEmptyWriteObjectEmptyReadObject o = unmarshaller.readObject(TestSerializableEmptyWriteObjectEmptyReadObject.class);
                assertEquals(serializable.sixth, o.sixth);
                assertEquals(0.0, o.seventh);
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableDefaultWriteObjectEmptyReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double seventh = 4.56;

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        }
    }

    @Test
    public void testSerializableDefaultWriteObjectEmptyReadObject() throws Throwable {
        final TestSerializableDefaultWriteObjectEmptyReadObject serializable = new TestSerializableDefaultWriteObjectEmptyReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (! (marshaller instanceof RiverMarshaller)) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableDefaultWriteObjectEmptyReadObject o = unmarshaller.readObject(TestSerializableDefaultWriteObjectEmptyReadObject.class);
                assertEquals(serializable.sixth, o.sixth);
                assertEquals(0.0, o.seventh);
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableNoWriteObjectEmptyReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double seventh = 4.56;

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        }
    }

    @Test
    public void testSerializableNoWriteObjectEmptyReadObject() throws Throwable {
        final TestSerializableNoWriteObjectEmptyReadObject serializable = new TestSerializableNoWriteObjectEmptyReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (! (marshaller instanceof RiverMarshaller)) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableNoWriteObjectEmptyReadObject o = unmarshaller.readObject(TestSerializableNoWriteObjectEmptyReadObject.class);
                assertEquals(serializable.sixth, o.sixth);
                assertEquals(0.0, o.seventh);
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableDefaultWriteObjectGetFieldsReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double eighth = 5.67;

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField getField = ois.readFields();
            eighth = (double) getField.get("eighth", 5.67);
        }
    }

    @Test
    public void testSerializableDefaultWriteObjectGetFieldsReadObject() throws Throwable {
        final Serializable serializable = new TestSerializableDefaultWriteObjectGetFieldsReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableEmptyWriteObjectGetFieldsReadObject extends TestSerializableDefaultWriteObjectNoReadObject {

        private static final long serialVersionUID = 3121360863878480344L;
        protected double eighth = 5.67;

        private void writeObject(ObjectOutputStream oos) throws IOException {
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField getField = ois.readFields();
            eighth = (double) getField.get("eighth", 0.0);
        }
    }

    @Test
    public void testSerializableEmptyWriteObjectGetFieldsReadObject() throws Throwable {
        final Serializable serializable = new TestSerializableEmptyWriteObjectGetFieldsReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (! (marshaller instanceof RiverMarshaller)) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestSerializablePutFieldsWriteObjectNoReadObject extends TestSerializable {

        private static final long serialVersionUID = 1191166362124148545L;
        protected double ninth = 6.78;

        private void writeObject(ObjectOutputStream oos) throws IOException {
            final ObjectOutputStream.PutField field = oos.putFields();
            field.put("ninth", ninth);
            oos.writeFields();
        }
    }

    @Test
    public void testSimplePutFieldsWriteObjectNoReadObject() throws Throwable {
        final Serializable serializable = new TestSerializablePutFieldsWriteObjectNoReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestSerializablePutFieldsWriteObjectEmptyReadObject extends TestSerializable {

        private static final long serialVersionUID = 1191166362124148545L;
        protected double ninth = 6.78;

        private void writeObject(ObjectOutputStream oos) throws IOException {
            final ObjectOutputStream.PutField field = oos.putFields();
            field.put("ninth", ninth);
            oos.writeFields();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        }
    }

    @Test
    public void testSimplePutFieldsWriteObjectEmptyReadObject() throws Throwable {
        final Serializable serializable = new TestSerializablePutFieldsWriteObjectEmptyReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (! (marshaller instanceof RiverMarshaller)) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializablePutFieldsWriteObjectEmptyReadObject o = unmarshaller.readObject(TestSerializablePutFieldsWriteObjectEmptyReadObject.class);
                assertEquals(0.0, o.ninth);
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializablePutFieldsWriteObjectDefaultReadObject extends TestSerializablePutFieldsWriteObjectNoReadObject {

        private static final long serialVersionUID = 1191166362124148545L;
        protected double tenth = 7.89;

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
        }
    }

    @Test
    public void testSerializablePutFieldsWriteObjectDefaultReadObject() throws Throwable {
        final Serializable serializable = new TestSerializablePutFieldsWriteObjectDefaultReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializablePutFieldsWriteObjectGetFieldsReadObject extends TestSerializable {

        private static final long serialVersionUID = 1191166362124148545L;
        protected double eleventh = 8.90;

        private void writeObject(ObjectOutputStream oos) throws IOException {
            final ObjectOutputStream.PutField field = oos.putFields();
            field.put("eleventh", eleventh);
            oos.writeFields();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField getField = ois.readFields();
            eleventh = (double) getField.get("eleventh", 8.90);
        }
    }

    @Test
    public void testSerializablePutFieldsWriteObjectGetFieldsReadObject() throws Throwable {
        final Serializable serializable = new TestSerializablePutFieldsWriteObjectGetFieldsReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testString() throws Throwable {
        final String s = "This is a test!";
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(s);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(s, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testInteger() throws Throwable {
        final Integer i = Integer.valueOf(12345);
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(i);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(i, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableThatReferencesSerializableOverridenReadObject implements Serializable {

        private static final long serialVersionUID = 3131360863878480344L;
        TestSerializableOverridenReadObject anSerializableOverridenReadObject;
        Integer balance;

        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof TestSerializableThatReferencesSerializableOverridenReadObject)) return false;
            TestSerializableThatReferencesSerializableOverridenReadObject that = (TestSerializableThatReferencesSerializableOverridenReadObject) obj;
            if (!safeEquals(balance, that.balance)) return false;
            if (!safeEquals(anSerializableOverridenReadObject, that.anSerializableOverridenReadObject)) return false;
            return true;
        }

        public int hashCode() {
            int result = 17;
            result = result * 31 + safeHashCode(balance);
            result = result * 31 + safeHashCode(anSerializableOverridenReadObject);
            return result;
        }

        private static int safeHashCode(Object obj) {
            return obj == null ? 0 : obj.hashCode();
        }

        private static boolean safeEquals(Object a, Object b) {
            return (a == b || (a != null && a.equals(b)));
        }
    }

    public static final class TestSerializableOverridenReadObject implements Serializable {

        private static final long serialVersionUID = 3141360863878480344L;
        String name;
        String ssn;
        transient boolean deserialized;

        public TestSerializableOverridenReadObject() {
            this.name = "Zamarreno";
            this.ssn = "234-567-8901";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof TestSerializableOverridenReadObject)) return false;
            TestSerializableOverridenReadObject that = (TestSerializableOverridenReadObject) obj;
            if (!name.equals(that.name)) return false;
            if (!ssn.equals(that.ssn)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = result * 31 + name.hashCode();
            result = result * 31 + ssn.hashCode();
            return result;
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            deserialized = true;
        }
    }

    @Test
    public void testSerializableThatReferencesSerializableOverridenReadObject() throws Throwable {
        final TestSerializableThatReferencesSerializableOverridenReadObject serializable = new TestSerializableThatReferencesSerializableOverridenReadObject();
        serializable.anSerializableOverridenReadObject = new TestSerializableOverridenReadObject();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestExternalizable implements Externalizable {

        private boolean ran;

        private static final long serialVersionUID = 2776810457096829768L;

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(54321);
            out.writeUTF("Hello!");
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            assertEquals(54321, in.readInt());
            assertEquals("Hello!", in.readUTF());
            assertTrue("No EOF", in.read() == -1);
            ran = true;
        }
    }

    public static final class TestExternalizableConstructed implements Externalizable {

        private final boolean ran;

        private static final long serialVersionUID = 2776810457096829768L;

        public TestExternalizableConstructed() {
            ran = false;
        }

        public TestExternalizableConstructed(final ObjectInput in) throws IOException {
            assertEquals(54321, in.readInt());
            assertEquals("Hello!", in.readUTF());
            assertTrue("No EOF", in.read() == -1);
            ran = true;
        }

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(54321);
            out.writeUTF("Hello!");
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        }
    }

    @Test
    public void testExternalizable() throws Throwable {
        final TestExternalizable ext = new TestExternalizable();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(ext);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestExternalizable extn = (TestExternalizable) unmarshaller.readObject();
                assertTrue("No EOF", unmarshaller.read() == -1);
                assertTrue("readExternal was not run", extn.ran);
            }
        });
        assertFalse("readExternal was run on the original", ext.ran);
    }

    @Test
    public void testExternalizableConstructed() throws Throwable {
        final TestExternalizableConstructed ext = new TestExternalizableConstructed();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(ext);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                if (unmarshaller instanceof ObjectInputStreamUnmarshaller) {
                    throw new SkipException("Test not relevant for " + unmarshaller);
                }
                final TestExternalizableConstructed extn = (TestExternalizableConstructed) unmarshaller.readObject();
                assertTrue("No EOF", unmarshaller.read() == -1);
                assertTrue("readExternal was not run", extn.ran);
            }
        });
        assertFalse("readExternal was run on the original", ext.ran);
    }

    @Test
    public void testObjectTable() throws Throwable {
        final AtomicBoolean writeRan = new AtomicBoolean();
        final AtomicBoolean readRan = new AtomicBoolean();
        final ObjectTable objectTable = new ObjectTable() {
            public Writer getObjectWriter(final Object object) {
                return new Writer() {
                    public void writeObject(final Marshaller marshaller, final Object object) throws IOException {
                        marshaller.writeInt(51423);
                        marshaller.writeUTF("Unga bunga!");
                        writeRan.set(true);
                    }
                };
            }

            public Object readObject(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
                assertEquals(51423, unmarshaller.readInt());
                assertEquals("Unga bunga!", unmarshaller.readUTF());
                assertEOF(unmarshaller);
                readRan.set(true);
                return new Object();
            }
        };
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setObjectTable(objectTable);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(new Object());
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertNotNull(unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
        assertTrue("writeObject never run", writeRan.get());
        assertTrue("readObject never run", readRan.get());
    }

    @Test
    public void testClassTableSerializable() throws Throwable {
        final ClassTable classTable = new ClassTable() {
            public Writer getClassWriter(final Class<?> clazz) {
                if (clazz == TestSerializable.class) {
                    return new Writer() {
                        public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
                            marshaller.writeUTF("Testtttt");
                            marshaller.writeInt(949332);
                            marshaller.writeDouble(15.125);
                        }
                    };
                } else {
                    return null;
                }
            }

            public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
                assertEquals("Testtttt", unmarshaller.readUTF());
                assertEquals(949332, unmarshaller.readInt());
                assertTrue(unmarshaller.readDouble() == 15.125);
                return TestSerializable.class;
            }
        };
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassTable(classTable);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(new TestSerializable());
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertSame(TestSerializable.class, unmarshaller.readObject().getClass());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testClassTableExternalizable() throws Throwable {
        final ClassTable classTable = new ClassTable() {
            public Writer getClassWriter(final Class<?> clazz) {
                if (clazz == TestExternalizable.class) {
                    return new Writer() {
                        public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
                            marshaller.writeUTF("Testtttt");
                            marshaller.writeInt(949332);
                            marshaller.writeDouble(15.125);
                        }
                    };
                } else {
                    return null;
                }
            }

            public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
                assertEquals("Testtttt", unmarshaller.readUTF());
                assertEquals(949332, unmarshaller.readInt());
                assertTrue(unmarshaller.readDouble() == 15.125);
                return TestExternalizable.class;
            }
        };
        final TestExternalizable ext = new TestExternalizable();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassTable(classTable);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(ext);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestExternalizable extn = (TestExternalizable) unmarshaller.readObject();
                assertEOF(unmarshaller);
                assertTrue("readExternal was not run", extn.ran);
            }
        });
        assertFalse("readExternal was run on the original", ext.ran);
    }

    @Test
    public void testMultiWrite() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                final TestSerializable obj = new TestSerializable();
                marshaller.writeObject(obj);
                marshaller.writeObject(obj);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertSame(unmarshaller.readObject(), unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testMultiWrite2() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                final TestSerializable obj = new TestSerializable();
                marshaller.writeObjectUnshared(obj);
                marshaller.writeObject(obj);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertNotSame(unmarshaller.readObjectUnshared(), unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testMultiWrite3() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                final TestSerializable obj1 = new TestSerializable();
                final TestSerializable obj2 = new TestSerializable();
                final TestSerializable obj3 = new TestSerializable();
                final TestSerializable obj4 = new TestSerializable();
                final TestSerializable obj5 = new TestSerializable();
                marshaller.writeObject(obj1);
                marshaller.writeObject(obj1);
                marshaller.writeObject(obj1);
                marshaller.writeObject(obj2);
                marshaller.writeObject(obj3);
                marshaller.writeObject(obj2);
                marshaller.writeObject(obj3);
                marshaller.writeObject(obj4);
                marshaller.writeObject(obj1);
                marshaller.writeObject(obj4);
                marshaller.writeObject(obj2);
                marshaller.writeObject(obj3);
                marshaller.writeObject(obj5);
                marshaller.writeObject(obj1);
                marshaller.writeObject(obj5);
                marshaller.writeObject(obj4);
                marshaller.writeObject(obj3);
                marshaller.writeObject(obj2);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestSerializable obj1 = (TestSerializable) unmarshaller.readObject();
                assertSame(obj1, unmarshaller.readObject());
                assertSame(obj1, unmarshaller.readObject());
                final TestSerializable obj2 = (TestSerializable) unmarshaller.readObject();
                assertNotSame(obj1, obj2);
                final TestSerializable obj3 = (TestSerializable) unmarshaller.readObject();
                assertNotSame(obj1, obj3);
                assertNotSame(obj2, obj3);
                assertSame(obj2, unmarshaller.readObject());
                assertSame(obj3, unmarshaller.readObject());
                final TestSerializable obj4 = (TestSerializable) unmarshaller.readObject();
                assertNotSame(obj1, obj4);
                assertNotSame(obj2, obj4);
                assertNotSame(obj3, obj4);
                assertSame(obj1, unmarshaller.readObject());
                assertSame(obj4, unmarshaller.readObject());
                assertSame(obj2, unmarshaller.readObject());
                assertSame(obj3, unmarshaller.readObject());
                final TestSerializable obj5 = (TestSerializable) unmarshaller.readObject();
                assertNotSame(obj1, obj5);
                assertNotSame(obj2, obj5);
                assertNotSame(obj3, obj5);
                assertNotSame(obj4, obj5);
                assertSame(obj1, unmarshaller.readObject());
                assertSame(obj5, unmarshaller.readObject());
                assertSame(obj4, unmarshaller.readObject());
                assertSame(obj3, unmarshaller.readObject());
                assertSame(obj2, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testBooleanArray() throws Throwable {
        final boolean[] test = new boolean[50];
        test[0] = test[5] = test[6] = test[14] = test[24] = test[39] = test[49] = true;
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                boolean[] result = (boolean[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testByteArray() throws Throwable {
        final byte[] test = new byte[50];
        new Random().nextBytes(test);
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                byte[] result = (byte[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testShortArray() throws Throwable {
        final short[] test = new short[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = (short) rng.nextInt();
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                short[] result = (short[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testIntArray() throws Throwable {
        final int[] test = new int[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextInt();
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                int[] result = (int[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testLongArray() throws Throwable {
        final long[] test = new long[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextLong();
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                long[] result = (long[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testFloatArray() throws Throwable {
        final float[] test = new float[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextFloat();
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                float[] result = (float[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testDoubleArray() throws Throwable {
        final double[] test = new double[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextDouble();
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                double[] result = (double[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testCharArray() throws Throwable {
        final char[] test = new char[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = (char) rng.nextInt();
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                char[] result = (char[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testObjectArray() throws Throwable {
        final TestSerializable[] test = new TestSerializable[50];
        for (int i = 0; i < 50; i++) {
            test[i] = new TestSerializable();
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object[] objArray = (Object[]) unmarshaller.readObject();
                assertTrue(Arrays.equals(test, objArray));
                assertSame(objArray, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testObjectArrayRecursion() throws Throwable {
        final Object[] test = new Object[5];
        for (int i = 0; i < 5; i++) {
            test[i] = test;
        }
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object[] test2 = (Object[]) unmarshaller.readObject();
                assertNotNull(test2[0]);
                assertNotNull(test2[1]);
                assertNotNull(test2[2]);
                assertNotNull(test2[3]);
                assertNotNull(test2[4]);
                assertSame(test2, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testHashMap() throws Throwable {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("kejlwqewq", "qwejwqioprjweqiorjpofd");
        map.put("34890fdu90uq09rdewq", "wqeioqwdias90ifd0adfw");
        map.put("dsajkljwqej21309ejjfdasfda", "dsajkdqwoid");
        map.put("nczxm,ncoijd0q93wjdwdwq", " dsajkldwqj9edwqu");
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(map);
                marshaller.writeObject(map);
            }

            @SuppressWarnings({ "unchecked" })
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Map<String, String> map2 = (Map<String, String>) unmarshaller.readObject();
                assertEquals(map, map2);
                assertSame(map2, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testConcurrentHashMap() throws Throwable {
        final Map<String, String> map = new ConcurrentHashMap<String, String>();
        map.put("kejlwqewq", "qwejwqioprjweqiorjpofd");
        map.put("34890fdu90uq09rdewq", "wqeioqwdias90ifd0adfw");
        map.put("dsajkljwqej21309ejjfdasfda", "dsajkdqwoid");
        map.put("nczxm,ncoijd0q93wjdwdwq", " dsajkldwqj9edwqu");
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(map);
                marshaller.writeObject(map);
            }

            @SuppressWarnings({ "unchecked" })
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Map<String, String> map2 = (Map<String, String>) unmarshaller.readObject();
                assertEquals(map, map2);
                assertSame(map2, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    private static final class HashMapExternalizer implements Externalizer {

        private static final long serialVersionUID = 4923778660953773530L;

        @SuppressWarnings({ "unchecked" })
        public void writeExternal(final Object subject, final ObjectOutput output) throws IOException {
            final HashMap map = (HashMap) subject;
            output.writeInt(map.size());
            for (Map.Entry e : (Set<Map.Entry>) map.entrySet()) {
                output.writeObject(e.getKey());
                output.writeObject(e.getValue());
            }
        }

        @SuppressWarnings({ "unchecked" })
        public Object createExternal(final Class<?> subjectType, final ObjectInput input, final Creator defaultCreator) throws IOException, ClassNotFoundException {
            final int size = input.readInt();
            final HashMap map = new HashMap(size * 2, 0.5f);
            for (int i = 0; i < size; i++) {
                map.put(input.readObject(), input.readObject());
            }
            return map;
        }

        public void readExternal(final Object subject, final ObjectInput input) throws IOException, ClassNotFoundException {
            // no op
        }
    }

    @Test
    public void testExternalizer() throws Throwable {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("kejlwqewq", "qwejwqioprjweqiorjpofd");
        map.put("34890fdu90uq09rdewq", "wqeioqwdias90ifd0adfw");
        map.put("dsajkljwqej21309ejjfdasfda", "dsajkdqwoid");
        map.put("nczxm,ncoijd0q93wjdwdwq", " dsajkldwqj9edwqu");
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassExternalizerFactory(new ClassExternalizerFactory() {
                    public Externalizer getExternalizer(final Class<?> type) {
                        if (type == HashMap.class) {
                            return new HashMapExternalizer();
                        } else {
                            return null;
                        }
                    }
                });
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(map);
                marshaller.writeObject(map);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object m1 = unmarshaller.readObject();
                assertEquals(map, m1);
                assertSame(m1, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestA implements Serializable {

        private static final long serialVersionUID = 4788787450574491652L;

        TestB testb;
    }

    public static class TestB implements Serializable {

        private static final long serialVersionUID = 4788787450574491652L;

        TestA testa;
    }

    @Test
    public void testCircularRefs() throws Throwable {
        final TestA testa = new TestA();
        final TestB testb = new TestB();
        testa.testb = testb;
        testb.testa = testa;
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(testa);
                marshaller.writeObject(testb);
                marshaller.writeObject(testa);
                marshaller.writeObject(testb);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestA ntesta = (TestA) unmarshaller.readObject();
                final TestB ntestb = (TestB) unmarshaller.readObject();
                assertSame(ntesta, unmarshaller.readObject());
                assertSame(ntestb, unmarshaller.readObject());
                assertSame(ntestb, ntesta.testb);
                assertSame(ntesta, ntestb.testa);
                assertEOF(unmarshaller);
            }
        });
    }

    public static class OuterClass implements Serializable {

        private static final long serialVersionUID = 4268208561523701594L;
        @SuppressWarnings({ "UnusedDeclaration" })
        private InnerClass inner;

        public class InnerClass implements Serializable {

            private static final long serialVersionUID = 26916326776519192L;
        }
    }

    @Test
    public void testInnerClass() throws Throwable {
        final OuterClass outerClass = new OuterClass();
        outerClass.inner = outerClass.new InnerClass();
        final OuterClass.InnerClass innerClass = new OuterClass().new InnerClass();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(outerClass);
                marshaller.writeObject(outerClass);
                marshaller.writeObject(innerClass);
                marshaller.writeObject(innerClass);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final OuterClass newOuterClass = (OuterClass) unmarshaller.readObject();
                final OuterClass newOuterClass2 = (OuterClass) unmarshaller.readObject();
                assertSame(newOuterClass, newOuterClass2);
                final OuterClass.InnerClass newInnerClass = (OuterClass.InnerClass) unmarshaller.readObject();
                final OuterClass.InnerClass newInnerClass2 = (OuterClass.InnerClass) unmarshaller.readObject();
                assertSame(newInnerClass, newInnerClass2);
                assertEOF(unmarshaller);
            }
        });
    }

    public static class TestC implements Externalizable {

        private static final long serialVersionUID = 4788787450574491652L;

        TestD testd;

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(testd);
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            testd = (TestD) in.readObject();
        }
    }

    public static class TestD implements Externalizable {

        private static final long serialVersionUID = 4788787450574491652L;

        TestC testc;

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(testc);
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            testc = (TestC) in.readObject();
        }
    }

    @Test
    public void testCircularRefsExt() throws Throwable {
        final TestC testc = new TestC();
        final TestD testd = new TestD();
        testc.testd = testd;
        testd.testc = testc;
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(testc);
                marshaller.writeObject(testd);
                marshaller.writeObject(testc);
                marshaller.writeObject(testd);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestC ntestc = (TestC) unmarshaller.readObject();
                final TestD ntestd = (TestD) unmarshaller.readObject();
                assertSame(ntestc, unmarshaller.readObject());
                assertSame(ntestd, unmarshaller.readObject());
                assertSame(ntestd, ntestc.testd);
                assertSame(ntestc, ntestd.testc);
                assertEOF(unmarshaller);
            }
        });
    }

    public enum Fruit {

        BANANA,
        APPLE,
        PEAR,
        ORANGE;
    }

    @Test
    public void testEnum1() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject("Blah");
                marshaller.writeObject(Fruit.APPLE);
                marshaller.writeObject(Boolean.TRUE);
                marshaller.writeObject(Fruit.PEAR);
                marshaller.writeObject("Goop");
                marshaller.writeObject(Fruit.APPLE);
                marshaller.writeObject(Fruit.PEAR);
                marshaller.writeObject(Fruit.APPLE);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals("Blah", unmarshaller.readObject());
                assertSame(Fruit.APPLE, unmarshaller.readObject());
                assertEquals(Boolean.TRUE, unmarshaller.readObject());
                assertSame(Fruit.PEAR, unmarshaller.readObject());
                assertEquals("Goop", unmarshaller.readObject());
                assertSame(Fruit.APPLE, unmarshaller.readObject());
                assertSame(Fruit.PEAR, unmarshaller.readObject());
                assertSame(Fruit.APPLE, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    @Test
    public void testTreeComp() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
                TreeMap<Integer, Integer> tree = new TreeMap<Integer, Integer>(new IntComp());

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(tree.comparator());
                tree.put(1, 1);
                marshaller.writeObject(tree);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                unmarshaller.readObject();
                TreeMap x = (TreeMap) unmarshaller.readObject();
                assertEquals(tree, x);
                assertTrue(x.comparator() instanceof IntComp);
            }
        });
    }

    public static class IntComp implements Comparator<Integer>, Serializable {

        public int compare(Integer o1, Integer o2) {
            return o1 == null ? 1 : o1.compareTo(o2);
        }

    }

    public interface Adder {

        int add(int amount);
    }

    public static final class TestInvocationHandler implements InvocationHandler, Serializable {

        private static final long serialVersionUID = 2808883573499129906L;

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            return Integer.valueOf(((Integer) args[0]).intValue() + 1);
        }
    }

    @Test
    public void testProxy() throws Throwable {
        final TestInvocationHandler tih = new TestInvocationHandler();
        final Adder adder = (Adder) Proxy.newProxyInstance(SimpleMarshallerTests.class.getClassLoader(), new Class<?>[] { Adder.class }, tih);
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                assertEquals(42, adder.add(41));
                marshaller.writeObject(adder);
                marshaller.writeObject(adder);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                Adder adder = (Adder) unmarshaller.readObject();
                final Object other = unmarshaller.readObject();
                assertSame(adder, other);
                assertEquals(42, adder.add(41));
                assertEOF(unmarshaller);
            }
        });
    }

    public static final class TestSerializableWithTransientFields implements Serializable {

        private static final long serialVersionUID = -4063085698159274676L;
        private transient int value;
        private String name;
    }

    @Test
    public void testSerializableWithTransientFields() throws Throwable {
        final TestSerializableWithTransientFields orig = new TestSerializableWithTransientFields();
        orig.value = 123;
        orig.name = "Balahala";
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(orig);
                marshaller.writeObject(orig);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableWithTransientFields repl = (TestSerializableWithTransientFields) unmarshaller.readObject();
                assertSame(repl, unmarshaller.readObject());
                assertEquals(0, repl.value);
                assertEquals(orig.name, repl.name);
            }
        });
    }

    public static final class TestSerializableWithFields implements Serializable {

        private static final long serialVersionUID = -4063085698159274676L;

        private transient int value;
        private String name;

        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("value", long.class),
                new ObjectStreamField("name", String.class),
        };

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            final ObjectInputStream.GetField field = ois.readFields();
            value = (int) field.get("value", 0L);
            name = (String) field.get("name", "");
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            final ObjectOutputStream.PutField field = oos.putFields();
            field.put("value", (long) value);
            field.put("name", name);
            oos.writeFields();
        }
    }

    @Test
    public void testSerializableWithFields() throws Throwable {
        final TestSerializableWithFields orig = new TestSerializableWithFields();
        orig.value = 123;
        orig.name = "Balahala";
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(orig);
                marshaller.writeObject(orig);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableWithFields repl = (TestSerializableWithFields) unmarshaller.readObject();
                assertSame(repl, unmarshaller.readObject());
                assertEquals(orig.value, repl.value);
                assertEquals(orig.name, repl.name);
            }
        });
    }

    @Test
    public void testExternalizerAnnotation() throws Throwable {
        final TestExternalizerWithAnnotation subject = new TestExternalizerWithAnnotation("Title", 1234);
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassExternalizerFactory(new AnnotationClassExternalizerFactory());
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(subject);
                marshaller.writeObject(subject);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestExternalizerWithAnnotation returnedSubject = (TestExternalizerWithAnnotation) unmarshaller.readObject();
                assertSame(returnedSubject, unmarshaller.readObject());
                assertEquals(subject.getString(), returnedSubject.getString());
                assertEquals(subject.getV(), returnedSubject.getV());
            }
        });
    }

    @Externalize(TestAnnotationExternalizer.class)
    public static final class TestExternalizerWithAnnotation {

        private final String string;
        private final int v;

        public TestExternalizerWithAnnotation(final String string, final int v) {
            this.string = string;
            this.v = v;
        }

        public String getString() {
            return string;
        }

        public int getV() {
            return v;
        }
    }

    public static final class TestAnnotationExternalizer implements Serializable, Externalizer {

        private static final long serialVersionUID = 1L;

        public void writeExternal(final Object subject, final ObjectOutput output) throws IOException {
            final TestExternalizerWithAnnotation testSubject = (TestExternalizerWithAnnotation) subject;
            output.writeObject(testSubject.getString());
            output.writeInt(testSubject.getV());
        }

        public Object createExternal(final Class<?> subjectType, final ObjectInput input, final Creator defaultCreator) throws IOException, ClassNotFoundException {
            return new TestExternalizerWithAnnotation((String) input.readObject(), input.readInt());
        }

        public void readExternal(final Object subject, final ObjectInput input) throws IOException, ClassNotFoundException {
            // empty
        }
    }

    public static final class OriginalClass implements Serializable {

        private static final long serialVersionUID = -1179299150244154246L;

        @SuppressWarnings({ "UnusedDeclaration" })
        private String blah = "Testing!";
    }

    public static final class ReplacementClass implements Serializable {

        private static final long serialVersionUID = -1179299150244154246L;

        private String blah = "Foo!";
    }

    @Test
    public void testClassReplace1() throws Throwable {
        final OriginalClass orig = new OriginalClass();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassResolver(new SimpleClassResolver(getClass().getClassLoader()) {
                    public String getClassName(final Class<?> clazz) throws IOException {
                        if (clazz == OriginalClass.class) {
                            return super.getClassName(ReplacementClass.class);
                        } else {
                            return super.getClassName(clazz);
                        }
                    }
                });
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (marshaller instanceof ObjectOutputStreamMarshaller) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(orig);
                marshaller.writeObject(orig);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object repl = unmarshaller.readObject();
                assertEquals(ReplacementClass.class, repl.getClass());
                assertSame(repl, unmarshaller.readObject());
                assertEquals(((ReplacementClass) repl).blah, "Testing!");
            }
        });
    }

    @Test
    public void testClassReplace2() throws Throwable {
        final OriginalClass orig = new OriginalClass();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassResolver(new SimpleClassResolver(getClass().getClassLoader()) {
                    protected Class<?> loadClass(final String name) throws ClassNotFoundException {
                        if (OriginalClass.class.getName().equals(name)) {
                            return ReplacementClass.class;
                        } else {
                            return super.loadClass(name);
                        }
                    }
                });
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (marshaller instanceof ObjectOutputStreamMarshaller) {
                    throw new SkipException("Test not relevant for " + marshaller);
                }
                marshaller.writeObject(orig);
                marshaller.writeObject(orig);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                if (unmarshaller instanceof ObjectInputStreamUnmarshaller) {
                    throw new SkipException("Substituting class name on read does not work with " + unmarshaller);
                }
                final Object repl = unmarshaller.readObject();
                assertEquals(ReplacementClass.class, repl.getClass());
                assertSame(repl, unmarshaller.readObject());
                assertEquals(((ReplacementClass) repl).blah, "Testing!");
            }
        });
    }

    @Test
    public void testReset() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                final String o1 = "This is a test";
                final Integer o2 = Integer.valueOf(48392);
                marshaller.writeObject(o1);
                marshaller.writeObject(o2);
                marshaller.writeObject(o1);
                marshaller.writeObject(o2);
                marshaller.clearClassCache(); // clears instance cache too
                marshaller.writeObject(o1);
                marshaller.writeObject(o2);
                marshaller.writeObject(o1);
                marshaller.writeObject(o2);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final String o1 = (String) unmarshaller.readObject();
                final Integer o2 = (Integer) unmarshaller.readObject();
                assertSame(o1, unmarshaller.readObject());
                assertEqualsOrSame(o2, unmarshaller.readObject());
                final String o1p = (String) unmarshaller.readObject();
                final Integer o2p = (Integer) unmarshaller.readObject();
                assertNotSame(o1, o1p);
                assertEqualsOrSame(o2, o2p);
                assertSame(o1p, unmarshaller.readObject());
                assertEqualsOrSame(o2p, unmarshaller.readObject());
            }
        });
    }

    public static final class VerifyingTestObject implements Serializable {

        private static final long serialVersionUID = 3554028965858904047L;

        private int number;
        private String string;

        public VerifyingTestObject() {
        }

        public VerifyingTestObject(final int number, final String string) {
            this.number = number;
            this.string = string;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(final int number) {
            this.number = number;
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            ois.registerValidation(new ObjectInputValidation() {
                public void validateObject() throws InvalidObjectException {
                    if (!Integer.toString(number).equals(string)) {
                        throw new InvalidObjectException("No match");
                    }
                }
            }, 10);
        }
    }

    @Test
    public void testValidation() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(new VerifyingTestObject(1234, "1234"));
                marshaller.writeObject(new VerifyingTestObject(1234, "4321"));
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                VerifyingTestObject obj = (VerifyingTestObject) unmarshaller.readObject();
                assertNotNull(obj);
                assertEquals(1234, obj.number);
                assertEquals("1234", obj.string);
                try {
                    unmarshaller.readObject();
                    fail("No validation exception thrown");
                } catch (InvalidObjectException e) {
                    // ok
                }
            }
        });
    }

    static class TestSerializableWithWriteReplaceHolder implements Serializable {

        private static final long serialVersionUID = 1L;
        private int i;
        private int j;

        public TestSerializableWithWriteReplaceHolder(TestSerializableWithWriteReplace original) {
            this.i = original.i.intValue();
            this.j = original.j.intValue();
        }

        public TestSerializableWithWriteReplaceHolder() {
        }

        protected Object readResolve() throws ObjectStreamException {
            System.out.println(this + ".readResolve()");
            return new TestSerializableWithWriteReplace(this);
        }
    }

    static class TestSerializableWithWriteReplace implements Serializable {

        private static final long serialVersionUID = 1L;
        private static int replaceCounter;
        private static int resolveCounter;
        private Integer i;
        private Integer j;

        public TestSerializableWithWriteReplace(int i, int j) {
            this.i = Integer.valueOf(i);
            this.j = Integer.valueOf(j);
        }

        public TestSerializableWithWriteReplace(TestSerializableWithWriteReplaceHolder holder) {
            resolveCounter++;
            this.i = Integer.valueOf(holder.i);
            this.j = Integer.valueOf(holder.j);
        }

        public TestSerializableWithWriteReplace() {
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof TestSerializableWithWriteReplace)) {
                return false;
            }
            TestSerializableWithWriteReplace other = (TestSerializableWithWriteReplace) o;
            return i.equals(other.i) && j.equals(other.j);
        }

        public boolean ok(int count) {
            return replaceCounter == count && resolveCounter == count;
        }

        protected Object writeReplace() throws ObjectStreamException {
            System.out.println(this + ".writeReplace()");
            replaceCounter++;
            return new TestSerializableWithWriteReplaceHolder(this);
        }
    }

    static class TestSerializableWithJavaUtilDate implements Serializable {

        public Date lastModified;
        private static final long serialVersionUID = -432434823510589221L;

        public TestSerializableWithJavaUtilDate() {
        }

        public TestSerializableWithJavaUtilDate(Date lastModified) {
            this.lastModified = lastModified;
        }
    }

    @Test
    public void testSerializableWithJavaUtilDate() throws Throwable {
        final TestSerializableWithJavaUtilDate serializable = new TestSerializableWithJavaUtilDate(
                new Date(System.currentTimeMillis()));
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                TestSerializableWithJavaUtilDate read = (TestSerializableWithJavaUtilDate) unmarshaller.readObject();
                assertEquals(serializable.lastModified + " is diff to read " + read.lastModified, serializable.lastModified, read.lastModified);
                assertEOF(unmarshaller);
            }
        });
    }

    /**
     * Verify that objects with writeReplace() and objects with readResolve() methods are handled correctly.
     */
    @Test
    public void testWriteReplace() throws Throwable {
        final TestSerializableWithWriteReplace t = new TestSerializableWithWriteReplace(1234, 5678);
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(t);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(t, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    /**
     * Verifies that marshallers and unmarshallers can be stopped and restarted.
     */
    @Test
    public void testReuse() throws Throwable {
        final TestSerializable t = new TestSerializable();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
        ByteOutput byteOutput = Marshalling.createByteOutput(baos);
        Marshaller marshaller = testMarshallerProvider.create(configuration.clone(), byteOutput);
        if (marshaller instanceof ObjectOutputStreamMarshaller) {
            throw new SkipException(marshaller + " doesn't support start()");
        }
        System.out.println("Marshaller = " + marshaller + " (version set to " + configuration.getVersion() + ")");
        marshaller.writeObject(t);
        marshaller.finish();
        byte[] bytes = baos.toByteArray();
        ByteInput byteInput = Marshalling.createByteInput(new ByteArrayInputStream(bytes));
        Unmarshaller unmarshaller = testUnmarshallerProvider.create(configuration.clone(), byteInput);
        if (unmarshaller instanceof ObjectInputStreamUnmarshaller) {
            throw new SkipException(unmarshaller + " doesn't support start()");
        }
        System.out.println("Unmarshaller = " + unmarshaller + " (version set to " + configuration.getVersion() + ")");
        assertEquals(t, unmarshaller.readObject());
        unmarshaller.finish();
        baos.reset();
        byteOutput = Marshalling.createByteOutput(baos);
        marshaller.start(byteOutput);
        marshaller.writeObject(t);
        marshaller.finish();
        bytes = baos.toByteArray();
        byteInput = Marshalling.createByteInput(new ByteArrayInputStream(bytes));
        unmarshaller.start(byteInput);
        System.out.println("Unmarshaller = " + unmarshaller + " (version set to " + configuration.getVersion() + ")");
        assertEquals(t, unmarshaller.readObject());
        unmarshaller.finish();
    }

    static class TestStreamHeader implements StreamHeader {

        private byte B1 = (byte) 12;
        private byte B2 = (byte) 13;
        private boolean readVisited;
        private boolean writeVisited;

        public void readHeader(ByteInput input) throws IOException {
            readVisited = true;
            System.out.println("readHeader() visited");
            byte b1 = (byte) input.read();
            byte b2 = (byte) input.read();
            System.out.println("b1: " + b1 + ", b2: " + b2);
            if (b1 != B1 || b2 != B2) {
                throw new StreamCorruptedException("invalid stream header");
            }
        }

        public void writeHeader(ByteOutput output) throws IOException {
            writeVisited = true;
            System.out.println("writeHeader() visited");
            output.write(B1);
            output.write(B2);
        }

        public boolean ok() {
            return readVisited && writeVisited;
        }
    }

    /**
     * Verify that use of customized StreamHeader works correctly.
     */
    @Test
    public void testStreamHeader() throws Throwable {
        if (testMarshallerProvider instanceof ObjectOutputStreamTestMarshallerProvider) {
            throw new SkipException("Can't set StreamHeader in compatibility tests");
        }
        if (testUnmarshallerProvider instanceof ObjectInputStreamTestUnmarshallerProvider) {
            throw new SkipException("Can't set StreamHeader in compatibility tests");
        }
        final Serializable serializable = new TestSerializable();
        final TestStreamHeader streamHeader = new TestStreamHeader();
        configuration.setStreamHeader(streamHeader);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
        ByteOutput byteOutput = Marshalling.createByteOutput(baos);
        Marshaller marshaller = testMarshallerProvider.create(configuration.clone(), byteOutput);
        System.out.println("Marshaller = " + marshaller + " (version set to " + configuration.getVersion() + ")");
        marshaller.writeObject(serializable);
        marshaller.finish();
        byte[] bytes = baos.toByteArray();
        ByteInput byteInput = Marshalling.createByteInput(new ByteArrayInputStream(bytes));
        Unmarshaller unmarshaller = testUnmarshallerProvider.create(configuration.clone(), byteInput);
        System.out.println("Unmarshaller = " + unmarshaller + " (version set to " + configuration.getVersion() + ")");
        assertEquals(serializable, unmarshaller.readObject());
        unmarshaller.finish();
        assertEOF(unmarshaller);
        assertTrue(streamHeader.ok());
    }

    public static class TestObjectResolver implements ObjectResolver {

        private boolean resolveVisited;
        private boolean replaceVisited;

        public Object readResolve(Object replacement) {
            if (new Integer(17).equals(replacement)) {
                resolveVisited = true;
                System.out.println("readResolve() visited");
                return new TestSerializable();
            } else {
                return replacement;
            }
        }

        public Object writeReplace(Object original) {
            if (original instanceof TestSerializable) {
                replaceVisited = true;
                System.out.println("writeReplace() visited");
                return new Integer(17);
            } else {
                return original;
            }
        }
        public boolean ok() {
            return resolveVisited && replaceVisited;
        }
    }

    public static class TestObjectPreResolver implements ObjectResolver {

        private boolean writeReplaceVisited;
        private boolean readReplaceVisited;

        public Object readResolve(Object replacement) {
        	if (replacement instanceof Integer) {
        		readReplaceVisited = true;
        	}
        	return replacement;
        }

        public Object writeReplace(Object original) {
            if (original instanceof TestSerializable) {
                writeReplaceVisited = true;
                return new Integer(17);
            } else {
                return original;
            }
        }
        
        public boolean ok() {
            return writeReplaceVisited && readReplaceVisited;
        }
    }

    
    /**
     * Verify that use of customized ObjectResolver works correctly.
     */
    @Test
    public void testObjectResolver() throws Throwable {
        if (testMarshallerProvider instanceof ObjectOutputStreamTestMarshallerProvider) {
            throw new SkipException("Can't use ObjectResolver in compatibility tests");
        }
        if (testUnmarshallerProvider instanceof ObjectInputStreamTestUnmarshallerProvider) {
            throw new SkipException("Can't use ObjectResolver in compatibility tests");
        }
        final TestObjectResolver objectResolver = new TestObjectResolver();
        final TestSerializable serializable = new TestSerializable();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setObjectResolver(objectResolver);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
        assertTrue(objectResolver.ok());
    }
    
    @Test
    public void testObjectPreResolver() throws Throwable {
        if (testMarshallerProvider instanceof ObjectOutputStreamTestMarshallerProvider) {
            throw new SkipException("Can't use objectPreResolver in compatibility tests");
        }
        if (testUnmarshallerProvider instanceof ObjectInputStreamTestUnmarshallerProvider) {
            throw new SkipException("Can't use objectPreResolver in compatibility tests");
        }
        final TestObjectPreResolver objectResolver = new TestObjectPreResolver();
        final TestSerializable serializable = new TestSerializable();
        final Integer expectedObj = new Integer(17);
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setObjectPreResolver(objectResolver);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(expectedObj, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
        assertTrue(objectResolver.ok());
    }

    static class TestSerializableWithInterleavedWriteReplace implements Serializable {

        private static final long serialVersionUID = 1L;
        private static int replaceCounter;
        private static int resolveCounter;
        private Integer i;
        private TestEnum te;

        public static void reset() {
            replaceCounter = 0;
            resolveCounter = 0;
        }

        public TestSerializableWithInterleavedWriteReplace(int i, TestEnum te) {
            this.i = Integer.valueOf(i);
            this.te = te;
        }

        public TestSerializableWithInterleavedWriteReplace() {
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof TestSerializableWithInterleavedWriteReplace)) {
                return false;
            }
            TestSerializableWithInterleavedWriteReplace other = (TestSerializableWithInterleavedWriteReplace) o;
            return i.equals(other.i) && te.equals(other.te);
        }

        public boolean ok(int count) {
            System.out.println("replaceCounter: " + replaceCounter + ", resolveCounter: " + resolveCounter);
            return 0 < replaceCounter && replaceCounter <= count && resolveCounter == 1;
        }

        protected Object writeReplace() throws ObjectStreamException {
            System.out.println(this + ".writeReplace()");
            replaceCounter++;
            if (te.equals(TestEnum.FIRST)) {
                return new TestSerializableWithInterleavedWriteReplace2(i.intValue(), te.next());
            }
            if (te.equals(TestEnum.SECOND)) {
                return new TestSerializableWithInterleavedWriteReplace3(i.intValue(), te.next());
            }
            if (te.equals(TestEnum.THIRD)) {
                return new TestSerializableWithInterleavedWriteReplace4(i.intValue(), te.next());
            }
            return this;
        }

        protected Object readResolve() throws ObjectStreamException {
            System.out.println(this + ".readResolve()");
            resolveCounter++;
            if (te.equals(TestEnum.FIFTH)) {
                return new TestSerializableWithInterleavedWriteReplace2(i.intValue(), TestEnum.SECOND);
            }
            if (te.equals(TestEnum.FOURTH)) {
                return new TestSerializableWithInterleavedWriteReplace2(i.intValue(), TestEnum.SECOND);
            }
            if (te.equals(TestEnum.THIRD)) {
                return new TestSerializableWithInterleavedWriteReplace2(i.intValue(), TestEnum.SECOND);
            } else {
                return this;
            }
        }
    }

    static class TestSerializableWithInterleavedWriteReplace2 extends TestSerializableWithInterleavedWriteReplace implements Serializable {

        private static final long serialVersionUID = 1527776832343228061L;

        public TestSerializableWithInterleavedWriteReplace2(int i, TestEnum te) {
            super(i, te);
        }

        public TestSerializableWithInterleavedWriteReplace2() {
        }
    }

    static class TestSerializableWithInterleavedWriteReplace3 extends TestSerializableWithInterleavedWriteReplace2 implements Serializable {

        private static final long serialVersionUID = -3231519519502968011L;

        public TestSerializableWithInterleavedWriteReplace3(int i, TestEnum te) {
            super(i, te);
        }

        public TestSerializableWithInterleavedWriteReplace3() {
        }
    }

    static class TestSerializableWithInterleavedWriteReplace4 extends TestSerializableWithInterleavedWriteReplace3 implements Serializable {

        private static final long serialVersionUID = -2396921036139628732L;

        public TestSerializableWithInterleavedWriteReplace4(int i, TestEnum te) {
            super(i, te);
        }

        public TestSerializableWithInterleavedWriteReplace4() {
        }
    }

    static class TestSerializableWithInterleavedWriteReplace5 extends TestSerializableWithInterleavedWriteReplace4 implements Serializable {

        private static final long serialVersionUID = 7353628200601762692L;

        public TestSerializableWithInterleavedWriteReplace5(int i, TestEnum te) {
            super(i, te);
        }

        public TestSerializableWithInterleavedWriteReplace5() {
        }
    }

    static class TestObjectResolverForInterleavedWriteReplace implements ObjectResolver {

        private int resolveCounter;
        private int replaceCounter;

        public Object readResolve(Object replacement) {
            if (!(replacement instanceof TestSerializableWithInterleavedWriteReplace)) {
                return replacement;
            }
            System.out.println(this + ".readResolve(): " + replacement);
            resolveCounter++;
            TestSerializableWithInterleavedWriteReplace tsw = (TestSerializableWithInterleavedWriteReplace) replacement;
            if (tsw.te.equals(TestEnum.SECOND)) {
                return new TestSerializableWithInterleavedWriteReplace(tsw.i.intValue(), tsw.te.previous());
            } else {
                return replacement;
            }
        }

        public Object writeReplace(Object original) {
            if (!(original instanceof TestSerializableWithInterleavedWriteReplace)) {
                return original;
            }
            System.out.println(this + ".writeReplace(): " + original);
            replaceCounter++;
            TestSerializableWithInterleavedWriteReplace tsw = (TestSerializableWithInterleavedWriteReplace) original;
            if (tsw.te.equals(TestEnum.FIRST)) {
                return new TestSerializableWithInterleavedWriteReplace2(tsw.i.intValue(), tsw.te.next());
            }
            if (tsw.te.equals(TestEnum.SECOND)) {
                return new TestSerializableWithInterleavedWriteReplace3(tsw.i.intValue(), tsw.te.next());
            }
            if (tsw.te.equals(TestEnum.THIRD)) {
                return new TestSerializableWithInterleavedWriteReplace4(tsw.i.intValue(), tsw.te.next());
            }
            if (tsw.te.equals(TestEnum.FOURTH)) {
                return new TestSerializableWithInterleavedWriteReplace5(tsw.i.intValue(), tsw.te.next());
            } else {
                return original;
            }
        }
        
        public Object preWriteReplace(Object original) {
            return original;
        }

        public boolean ok() {
            return replaceCounter == 1 && resolveCounter == 1;
        }
    }

    public enum TestEnum {

        FIRST {TestEnum next() {
            return SECOND;
        };TestEnum previous() {
            return FIFTH;
        }},
        SECOND {TestEnum next() {
            return THIRD;
        };TestEnum previous() {
            return FIRST;
        }},
        THIRD {TestEnum next() {
            return FOURTH;
        };TestEnum previous() {
            return SECOND;
        }},
        FOURTH {TestEnum next() {
            return FIFTH;
        };TestEnum previous() {
            return THIRD;
        }},
        FIFTH {TestEnum next() {
            return FIRST;
        };TestEnum previous() {
            return FOURTH;
        }};

        abstract TestEnum next();

        abstract TestEnum previous();
    }

    /**
     * Verify that objects with writeReplace()/readResolve() interact correctly with ObjectResolvers.
     */
    @Test
    public void testObjectResolverWithInterleavedWriteReplace() throws Throwable {
        if (testMarshallerProvider instanceof ObjectOutputStreamTestMarshallerProvider) {
            throw new SkipException("Can't use ObjectResolver in compatibility tests");
        }
        if (testUnmarshallerProvider instanceof ObjectInputStreamTestUnmarshallerProvider) {
            throw new SkipException("Can't use ObjectResolver in compatibility tests");
        }
        final TestObjectResolverForInterleavedWriteReplace objectResolver = new TestObjectResolverForInterleavedWriteReplace();
        final TestSerializableWithInterleavedWriteReplace o = new TestSerializableWithInterleavedWriteReplace(3, TestEnum.FIRST);
        TestSerializableWithInterleavedWriteReplace.reset();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setObjectResolver(objectResolver);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(o);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(o, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
        assertTrue(o.ok(4));
        assertTrue(objectResolver.ok());
    }

    public static class TestExternalizableInt implements Serializable {

        private static final long serialVersionUID = 805500397903006481L;
        private int secret;

        public TestExternalizableInt(int secret) {
            this.secret = secret;
        }

        public TestExternalizableInt() {
        }

        public int getSecret() {
            return secret;
        }

        public void setSecret(int secret) {
            this.secret = secret;
        }

        public boolean equals(Object o) {
            if (!(o instanceof TestExternalizableInt)) {
                return false;
            }
            return ((TestExternalizableInt) o).secret == this.secret;
        }

        public int hashCode() {
            return secret;
        }
    }

    public static class TestExternalizer implements Externalizer, Serializable {

        private static final long serialVersionUID = -8104713864804175542L;

        public Object createExternal(Class<?> subjectType, ObjectInput input, Creator defaultCreator) throws IOException, ClassNotFoundException {
            if (!TestExternalizableInt.class.isAssignableFrom(subjectType)) {
                throw new IOException(this + " only works for " + TestExternalizableInt.class + " but I got a " + subjectType);
            }
            Object obj = null;
            try {
                if (defaultCreator != null) {
                    obj = defaultCreator.create(subjectType);
                } else {
                    obj = Class.forName(subjectType.getName());
                }
            } catch (Exception e) {
                throw new IOException(e + "\n" + e.getMessage());
            }
            return obj;
        }

        public void readExternal(Object subject, ObjectInput input) throws IOException, ClassNotFoundException {
            if (TestExternalizableInt.class.isAssignableFrom(subject.getClass())) {
                System.out.println(this + " reading  " + subject.getClass());
                ((TestExternalizableInt) subject).setSecret(input.readInt());
            } else {
                throw new IOException(this + " only works for " + TestExternalizableInt.class);
            }
        }

        public void writeExternal(Object subject, ObjectOutput output) throws IOException {
            if (TestExternalizableInt.class.isAssignableFrom(subject.getClass())) {
                System.out.println(this + " writing " + subject.getClass());
                output.writeInt(((TestExternalizableInt) subject).getSecret());
            } else {
                throw new IOException(this + " only works for " + TestExternalizableInt.class);
            }
        }
    }

    static class TestExternalizerFactory implements ClassExternalizerFactory {

        public Externalizer getExternalizer(Class<?> type) {
            if (type == TestExternalizableInt.class) {
                TestExternalizer externalizer = new TestExternalizer();
                return externalizer;
            } else {
                return null;
            }
        }
    }

    /*
    * Verify that use of customized ExternalizerFactory to write the same object twice
    * results in sending a handle on the second write.
    */
    @Test
    public void testExternalizerWithRepeatedWrites() throws Throwable {
        if (testMarshallerProvider instanceof ObjectOutputStreamTestMarshallerProvider) {
            throw new SkipException("Can't use ClassExternalizerFactory in compatibility tests");
        }
        if (testUnmarshallerProvider instanceof ObjectInputStreamTestUnmarshallerProvider) {
            throw new SkipException("Can't use ClassExternalizerFactory in compatibility tests");
        }
        TestExternalizerFactory externalizerFactory = new TestExternalizerFactory();
        Object o = new TestExternalizableInt(7);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteOutput byteOutput = Marshalling.createByteOutput(baos);
        MarshallingConfiguration config = configuration.clone();
        config.setClassExternalizerFactory(externalizerFactory);
        Marshaller marshaller = testMarshallerProvider.create(config, byteOutput);
//        if (marshaller instanceof SerialMarshaller) {
//            throw new SkipException("TODO: Known issue - see JBMAR-50");
//        }
        marshaller.writeObject(o);
        marshaller.writeObject(o);
        marshaller.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ByteInput byteInput = Marshalling.createByteInput(bais);
        config = configuration.clone();
        config.setClassExternalizerFactory(externalizerFactory);
        Unmarshaller unmarshaller = testUnmarshallerProvider.create(config, byteInput);
        Object o2 = unmarshaller.readObject();
        assertEquals(o, o2);
        Object o3 = unmarshaller.readObject();
        assertSame(o2, o3);
    }

    public static final class TestExternalizableWithSerializableFields implements Externalizable {

        private boolean ran;
        private Object obj = new TestSerializableWithFields();

        private static final long serialVersionUID = 2776810457096829768L;

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(54321);
            out.writeUTF("Hello!");
            out.writeObject(obj);
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            assertEquals(54321, in.readInt());
            assertEquals("Hello!", in.readUTF());
            obj = in.readObject();
            assertTrue("No EOF", in.read() == -1);
            ran = true;
        }
    }

    @Test
    public void testExternalizableWithFollowingObjects() throws Throwable {
        final TestExternalizableWithSerializableFields ext1 = new TestExternalizableWithSerializableFields();
        final TestExternalizableWithSerializableFields ext2 = new TestExternalizableWithSerializableFields();
        final TestExternalizableWithSerializableFields ext3 = new TestExternalizableWithSerializableFields();
        final AtomicInteger version = new AtomicInteger();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                version.set(configuration.getVersion());
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(ext1);
                marshaller.writeObject(ext2);
                marshaller.writeObject(ext3);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                if ((unmarshaller instanceof RiverUnmarshaller) && version.get() < 1) {
                    throw new SkipException("River v0 can't detect eof on each object");
                }
                final TestExternalizableWithSerializableFields rext1 = (TestExternalizableWithSerializableFields) unmarshaller.readObject();
                final TestExternalizableWithSerializableFields rext2 = (TestExternalizableWithSerializableFields) unmarshaller.readObject();
                final TestExternalizableWithSerializableFields rext3 = (TestExternalizableWithSerializableFields) unmarshaller.readObject();
                assertTrue("No EOF", unmarshaller.read() == -1);
                assertTrue("readExternal 1 was not run", rext1.ran);
                assertTrue("readExternal 2 was not run", rext2.ran);
                assertTrue("readExternal 3 was not run", rext3.ran);
            }
        });
        assertFalse("readExternal was run on the original 1", ext1.ran);
        assertFalse("readExternal was run on the original 2", ext2.ran);
        assertFalse("readExternal was run on the original 3", ext3.ran);
    }

    @Test
    public void testExternalizablePlusExternalizer() throws Throwable {
        final TestExternalizableWithSerializableFields ext1 = new TestExternalizableWithSerializableFields();
        final TestExternalizableWithSerializableFields ext2 = new TestExternalizableWithSerializableFields();
        final TestExternalizableWithSerializableFields ext3 = new TestExternalizableWithSerializableFields();
        final Map<String, TestExternalizableWithSerializableFields> map = new HashMap<String, TestExternalizableWithSerializableFields>();
        map.put("kejlwqewq", ext1);
        map.put("34890fdu90uq09rdewq", ext2);
        map.put("nczxm,ncoijd0q93wjdwdwq", ext3);
        final AtomicInteger version = new AtomicInteger();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassExternalizerFactory(new ClassExternalizerFactory() {
                    public Externalizer getExternalizer(final Class<?> type) {
                        if (type == HashMap.class) {
                            return new HashMapExternalizer();
                        } else {
                            return null;
                        }
                    }
                });
                version.set(configuration.getVersion());
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(map);
                marshaller.writeObject(map);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                if ((unmarshaller instanceof RiverUnmarshaller) && version.get() < 1) {
                    throw new SkipException("River v0 can't detect eof on each object");
                }
                final Object m1 = unmarshaller.readObject();
                assertEquals(HashMap.class, m1.getClass());
                assertSame(m1, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }

    private static final int[] reallyLongStringLengths = { 0, 60, 255, 256, 300, 1000, 15000, 65538 };
    private static final long reallyLongStringLengthSeed = 0x1bd63a9e00333b34L;

    @Test
    public void testReallyLongStrings() throws Throwable {
        final Random random = new Random(reallyLongStringLengthSeed);
        for (int len : reallyLongStringLengths) {
            final StringBuilder builder = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                char ch;
                do {
                    ch = (char) random.nextInt();
                } while (!Character.isDefined(ch) || Character.isLowSurrogate(ch) || Character.isHighSurrogate(ch));
                builder.append(ch);
            }
            final String s = builder.toString();
            runReadWriteTest(new ReadWriteTest() {
                public void runWrite(final Marshaller marshaller) throws Throwable {
                    marshaller.writeObject(s);
                    marshaller.writeObject(s);
                }

                public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                    final String rs = (String) unmarshaller.readObject();
                    assertEquals("String does not match", s, rs);
                    assertSame(rs, unmarshaller.readObject());
                }
            });
        }
    }

    public static class TestSerializableEmpty implements Serializable {

        private static final long serialVersionUID = 997011009926105464L;
    }

    @Test
    public void testObjectGraphDistance() throws Throwable {
        final TestSerializableEmpty first = new TestSerializableEmpty();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(first);
                for (int i = 0; i < 65536 + 500; i++) {
                    marshaller.writeObject(new TestSerializableEmpty());
                }
                marshaller.writeObject(first);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object test = unmarshaller.readObject();
                for (int i = 0; i < 65536 + 500; i++) {
                    unmarshaller.readObject();
                }
                assertSame("Far backref", test, unmarshaller.readObject());
            }
        });
    }

    public static final class TestExternalizableForWithBlockData implements Externalizable {

        private static final long serialVersionUID = -5588239332881048585L;

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            for (int i = 0; i < 550; i++) {
                int current = in.readInt();
                assertEquals(i, current);
            }
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            for (int i = 0; i < 550; i++) {
                out.writeInt(i);
            }
        }
    }

    @Test
    public void testFlushWithBlockData() throws Throwable {
        final TestExternalizableForWithBlockData externalizableForJBMar84 = new TestExternalizableForWithBlockData();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(externalizableForJBMar84);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                unmarshaller.readObject();
            }
        });
    }

    public static final class TestModifiableFinalField implements Serializable {

        private static final long serialVersionUID = 712538031604959058L;
        private static final FieldSetter valSetter = FieldSetter.get(TestModifiableFinalField.class, "val");
        private static final FieldSetter fooSetter = FieldSetter.get(TestModifiableFinalField.class, "foo");

        private transient final int val;
        private transient final Object foo;

        public TestModifiableFinalField(final int val, final Object foo) {
            this.val = val;
            this.foo = foo;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            oos.writeInt(val);
            oos.writeObject(foo);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            valSetter.setInt(this, ois.readInt());
            fooSetter.set(this, ois.readObject());
        }

        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final TestModifiableFinalField that = (TestModifiableFinalField) o;
            if (val != that.val) return false;
            if (foo != null ? !foo.equals(that.foo) : that.foo != null) return false;
            return true;
        }

        public int hashCode() {
            int result = val;
            result = 31 * result + (foo != null ? foo.hashCode() : 0);
            return result;
        }
    }

    @Test
    public void testModifiableFinalFields() throws Throwable {
        final TestModifiableFinalField test = new TestModifiableFinalField(42, "Boogeyman");
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object newTest = unmarshaller.readObject();
                assertSame("Consecutive reads not identical", newTest, unmarshaller.readObject());
                assertEquals("Read object != written object", test, newTest);
            }
        });
    }

    static class Parent implements Serializable {

        private String id;
        private Child1 child1Obj;
        private static final long serialVersionUID = -1417329412107563636L;

        public Parent(String id, Child1 child1Obj) {
            this.id = id;
            this.child1Obj = child1Obj;
        }

        public String getId() {
            return id;
        }

        public Child1 getChild1Obj() {
            return child1Obj;
        }

        public boolean equals(final Object obj) {
            return obj instanceof Parent && equals((Parent) obj);
        }

        public boolean equals(final Parent obj) {
            return obj != null && (id == null ? obj.id == null : id.equals(obj.id)) && (child1Obj == null ? obj.child1Obj == null : child1Obj.equals(obj.child1Obj));
        }

        public String getFieldString() {
            return String.format("id=%s,child1=%s", id, child1Obj);
        }

        public String toString() {
            return String.format("%s{%s}", getClass().getSimpleName(), getFieldString());
        }
    }

    static class Child1 extends Parent {

        private int someInt;
        private static final long serialVersionUID = -1989376997581386909L;

        public Child1(int someInt, String parentStr) {
            super(parentStr, null);
            this.someInt = someInt;
        }

        public boolean equals(final Object obj) {
            return obj instanceof Child1 && equals((Child1) obj);
        }

        public boolean equals(final Parent obj) {
            return obj instanceof Child1 && equals((Child1) obj);
        }

        public boolean equals(final Child1 obj) {
            return super.equals((Parent) obj) && someInt == obj.someInt;
        }

        public String getFieldString() {
            return String.format("%s,someInt=%d", super.getFieldString(), Integer.valueOf(someInt));
        }
    }

    static class Child2 extends Parent {

        private int someInt;
        private static final long serialVersionUID = 5406289444171915286L;

        public Child2(int someInt, String parentStr, Child1 child1Obj) {
            super(parentStr, child1Obj);
            this.someInt = someInt;
        }

        public boolean equals(final Object obj) {
            return obj instanceof Child2 && equals((Child2) obj);
        }

        public boolean equals(final Parent obj) {
            return obj instanceof Child2 && equals((Child2) obj);
        }

        public boolean equals(final Child2 obj) {
            return super.equals((Parent) obj) && someInt == obj.someInt;
        }

        public String getFieldString() {
            return String.format("%s,someInt=%d", super.getFieldString(), Integer.valueOf(someInt));
        }
    }

    @Test
    public void testNestedSubclass() throws Throwable {
        final Child1 child1Obj = new Child1(1234, "1234");
        final Child2 child2Obj = new Child2(2345, "2345", child1Obj);
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(child2Obj);
                marshaller.writeObject(child2Obj);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object newTest = unmarshaller.readObject();
                assertSame("Consecutive reads not identical", newTest, unmarshaller.readObject());
                assertEquals("Read object != written object", child2Obj, newTest);
            }
        });
    }

    static class ClassTableSuper implements Serializable {

        private static final long serialVersionUID = 1L;

        int someInt = 123;
        String someString = "test";
    }

    static class ClassTableSub extends ClassTableSuper {
        private static final long serialVersionUID = -1732733608096099080L;

        float fl = 0.125f;
        String str2 = "blah";
    }

    @Test
    public void testClassTableHierarchy() throws Throwable {
        final ClassTable classTable = new ClassTable() {
            public Writer getClassWriter(final Class<?> clazz) throws IOException {
                return clazz == ClassTableSuper.class ? new Writer() {
                    public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
                        marshaller.writeByte(1);
                    }
                } : clazz == ClassTableSub.class ? new Writer() {
                    public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
                        marshaller.writeByte(2);
                    }
                } : null;
            }

            public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
                switch (unmarshaller.readByte()) {
                    case 1:
                        return ClassTableSuper.class;
                    case 2:
                        return ClassTableSub.class;
                    default:
                        throw new IllegalStateException();
                }
            }
        };
        final ClassTableSub sub = new ClassTableSub();
        final ClassTableSuper sup = new ClassTableSuper();
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassTable(classTable);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(sub);
                marshaller.writeObject(sup);
                marshaller.writeObject("check");
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final ClassTableSub sub1 = unmarshaller.readObject(ClassTableSub.class);
                final ClassTableSuper sup1 = unmarshaller.readObject(ClassTableSuper.class);
                assertEquals("check", unmarshaller.readObject());
                assertEquals(sub.someInt, sub1.someInt);
                assertEquals(sub.someString, sub1.someString);
                assertEquals(sub.fl, sub1.fl, 0.0f);
                assertEquals(sub.str2, sub1.str2);
                assertEquals(sup.someInt, sup1.someInt);
                assertEquals(sup.someString, sup1.someString);
            }
        });
    }

    @Test
    public void testTreeMapBackref() throws Throwable {
        final TreeMap<Object, Object> treeMap = new TreeMap<Object, Object>();
        treeMap.put("Hi", treeMap);
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(treeMap);
                marshaller.writeObject(treeMap);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TreeMap newTreeMap = unmarshaller.readObject(TreeMap.class);
                assertSame(newTreeMap, unmarshaller.readObject(TreeMap.class));
            }
        });
    }

    @Test
    public void testTreeSetBackref() throws Throwable {
        final TreeSet<TreeSetSelfRef> set = new TreeSet<TreeSetSelfRef>();
        final TreeSetSelfRef item = new TreeSetSelfRef("1", set);
        set.add(item);
        set.add(new TreeSetSelfRef("4", set));
        set.add(new TreeSetSelfRef("7", set));
        set.add(new TreeSetSelfRef("8", set));
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(item);
                marshaller.writeObject(item);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TreeSetSelfRef thing = unmarshaller.readObject(TreeSetSelfRef.class);
                assertSame(thing, unmarshaller.readObject(TreeSetSelfRef.class));
            }
        });
    }

    private static final class TreeSetSelfRef implements Comparable<TreeSetSelfRef>, Serializable {

        private static final long serialVersionUID = 1L;

        private final String zap;
        private final TreeSet<TreeSetSelfRef> parent;

        private TreeSetSelfRef(final String zap, final TreeSet<TreeSetSelfRef> parent) {
            this.zap = zap;
            this.parent = parent;
        }

        public int compareTo(final TreeSetSelfRef o) {
            return zap.compareTo(o.zap);
        }
    }

    private static final class NonPublicExt implements Externalizable {

        public NonPublicExt() {
        }

        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(1);
        }

        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            assertEquals(in.readInt(), 1);
        }
    }

    @Test
    public void testNonPublicExtClass() throws Throwable {
        final NonPublicExt test = new NonPublicExt();
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                unmarshaller.readObject();
            }
        });
    }

    @Test
    public void testHierarchyLikeLinkedHashMapSubclass() throws Throwable {
        final LRUMap test = new LRUMap(64);
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final LRUMap map = unmarshaller.readObject(LRUMap.class);
                assertSame(map, unmarshaller.readObject());
            }
        });
    }

    static class OrigLvl1 implements Serializable {

        private static final long serialVersionUID = 1L;

        private int foo;
        private String bar;

        OrigLvl1(final int foo, final String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public int getFoo() {
            return foo;
        }

        public void setFoo(final int foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(final String bar) {
            this.bar = bar;
        }

        public boolean equals(Object other) {
            return other instanceof OrigLvl1 && equals((OrigLvl1) other);
        }

        public boolean equals(OrigLvl1 other) {
            return other != null && other.getClass() == getClass() && foo == other.foo && bar.equals(other.bar);
        }
    }

    static class OrigLvl2 extends OrigLvl1 implements Serializable {

        private static final long serialVersionUID = 1L;

        private int[] baz;
        private String zap;

        OrigLvl2(final int foo, final String bar, final int[] baz, final String zap) {
            super(foo, bar);
            this.baz = baz;
            this.zap = zap;
        }

        public int[] getBaz() {
            return baz;
        }

        public void setBaz(final int[] baz) {
            this.baz = baz;
        }

        public String getZap() {
            return zap;
        }

        public void setZap(final String zap) {
            this.zap = zap;
        }

        public boolean equals(Object other) {
            return other instanceof OrigLvl2 && equals((OrigLvl2) other);
        }

        public boolean equals(final OrigLvl1 other) {
            return equals((Object) other);
        }

        public boolean equals(final OrigLvl2 other) {
            return other != null && other.getClass() == getClass() && super.equals(other) && Arrays.equals(baz, other.baz) && zap.equals(other.zap);
        }
    }

    static class OrigLvl3 extends OrigLvl2 implements Serializable {

        private static final long serialVersionUID = 1L;

        private boolean zab;
        private String paz;

        OrigLvl3(final int foo, final String bar, final int[] baz, final String zap, final boolean zab, final String paz) {
            super(foo, bar, baz, zap);
            this.zab = zab;
            this.paz = paz;
        }

        public boolean isZab() {
            return zab;
        }

        public void setZab(final boolean zab) {
            this.zab = zab;
        }

        public String getPaz() {
            return paz;
        }

        public void setPaz(final String paz) {
            this.paz = paz;
        }

        public boolean equals(Object other) {
            return other instanceof OrigLvl3 && equals((OrigLvl3) other);
        }

        public boolean equals(final OrigLvl1 other) {
            return equals((Object) other);
        }

        public boolean equals(final OrigLvl2 other) {
            return equals((Object) other);
        }

        public boolean equals(final OrigLvl3 other) {
            return other != null && other.getClass() == getClass() && super.equals(other) && zab == other.zab && paz.equals(other.paz);
        }
    }

    static class NewLvl1 implements Serializable {

        private static final long serialVersionUID = 1L;

        private int foo;
        private String bar;

        NewLvl1(final int foo, final String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public int getFoo() {
            return foo;
        }

        public void setFoo(final int foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(final String bar) {
            this.bar = bar;
        }

        public boolean equals(Object other) {
            return other instanceof NewLvl1 && equals((NewLvl1) other);
        }

        public boolean equals(NewLvl1 other) {
            return other != null && other.getClass() == getClass() && foo == other.foo && bar.equals(other.bar);
        }
    }

    static class NewLvl3 extends NewLvl1 implements Serializable {

        private static final long serialVersionUID = 1L;

        private boolean zab;
        private String paz;

        NewLvl3(final int foo, final String bar, final boolean zab, final String paz) {
            super(foo, bar);
            this.zab = zab;
            this.paz = paz;
        }

        public boolean isZab() {
            return zab;
        }

        public void setZab(final boolean zab) {
            this.zab = zab;
        }

        public String getPaz() {
            return paz;
        }

        public void setPaz(final String paz) {
            this.paz = paz;
        }

        public boolean equals(Object other) {
            return other instanceof NewLvl3 && equals((NewLvl3) other);
        }

        public boolean equals(final NewLvl1 other) {
            return equals((Object) other);
        }

        public boolean equals(final NewLvl3 other) {
            return other != null && other.getClass() == getClass() && super.equals(other) && zab == other.zab && paz.equals(other.paz);
        }
    }

    @Test
    public void testClassRemovedFromHierarchy() throws Throwable {
        final OrigLvl3 origLvl3 = new OrigLvl3(123, "blah", new int[] { 1, 2, 3 }, "bzzzz", true, "fooble");
        runReadWriteTest(new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassResolver(new SimpleClassResolver(getClass().getClassLoader()) {
                    public String getClassName(final Class<?> clazz) throws IOException {
                        if (clazz == OrigLvl1.class || clazz == OrigLvl2.class || clazz == OrigLvl3.class) {
                            return clazz.getName().replace("Orig", "New");
                        } else {
                            return clazz.getName();
                        }
                    }
                });
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(origLvl3);
                marshaller.writeObject(origLvl3);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                unmarshaller.readObject();
                unmarshaller.readObject();
            }
        });
    }
}
