/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
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

package org.jboss.river;

import junit.framework.TestCase;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.Creator;
import org.jboss.testsupport.LoggingHelper;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Arrays;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 */
public final class RiverTestCase extends TestCase {

    static {
        LoggingHelper.init();
    }

    public void testUnserializable() throws Throwable {
        try {
            new ReadWriteTest() {
                public void runWrite(final Marshaller marshaller) throws Throwable {
                    marshaller.writeObject(new Object());
                }

                public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                }
            }.run();
        } catch (NotSerializableException e) {
            // ok
            return;
        } catch (IOException e) {
            fail("Wrong exception");
        }
        fail("Missing exception");
    }

    private static final class TestSerializable implements Serializable {
        private static final long serialVersionUID = -3834685845327229499L;

        private int first = 1234;
        private float second = 13.725f;
        private boolean argh = true;
        private byte[] third = new byte[] { 15, 2, 3 };
        private Object zap = Float.valueOf(5.5f);
        private Void foo = null;

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

        private void writeObject(ObjectOutputStream oos) throws IOException {
            final ObjectOutputStream.PutField field = oos.putFields();
            field.put("first", first);
            field.put("second", second);
            field.put("argh", argh);
            field.put("third", third);
            field.put("zap", zap);
            field.put("foo", foo);
            oos.writeFields();
        }
    }

    public void testSimple() throws Throwable {
        final Serializable serializable = new TestSerializable();
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(serializable);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(serializable, unmarshaller.readObject());
                assertTrue("No EOF", unmarshaller.read() == -1);
            }
        }.run();
    }

    public void testString() throws Throwable {
        final String s = "This is a test!";
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(s);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(s, unmarshaller.readObject());
                assertTrue("No EOF", unmarshaller.read() == -1);
            }
        }.run();
    }

    public void testInteger() throws Throwable {
        final Integer i = Integer.valueOf(12345);
        final AtomicBoolean ran = new AtomicBoolean();
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(i);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertEquals(i, unmarshaller.readObject());
                assertTrue("No EOF", unmarshaller.read() == -1);
                ran.set(true);
            }
        }.run();
        assertTrue("runRead never ran", ran.get());
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

    public void testExternalizable() throws Throwable {
        final TestExternalizable ext = new TestExternalizable();
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(ext);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestExternalizable extn = (TestExternalizable) unmarshaller.readObject();
                assertTrue("No EOF", unmarshaller.read() == -1);
                assertTrue("readExternal was not run", extn.ran);
            }
        }.run();
        assertFalse("readExternal was run on the original", ext.ran);
    }

    public void testObjectTable() throws Throwable {
        final AtomicBoolean ran = new AtomicBoolean();
        final ObjectTable objectTable = new ObjectTable() {
            public Writer getObjectWriter(final Object object) {
                return new Writer() {
                    public void writeObject(final Marshaller marshaller, final Object object) throws IOException {
                        marshaller.writeInt(51423);
                        marshaller.writeUTF("Unga bunga!");
                    }
                };
            }

            public Object readObject(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
                assertEquals(51423, unmarshaller.readInt());
                assertEquals("Unga bunga!", unmarshaller.readUTF());
                assertTrue("No EOF", unmarshaller.read() == -1);
                ran.set(true);
                return new Object();
            }
        };
        new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setObjectTable(objectTable);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(new Object());
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                unmarshaller.readObject();
                assertTrue("No EOF", unmarshaller.read() == -1);
            }
        }.run();
        assertTrue("readObject never run", ran.get());
    }

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
        new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassTable(classTable);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(new TestSerializable());
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertSame(TestSerializable.class, unmarshaller.readObject().getClass());
            }
        }.run();
    }

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
        new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setClassTable(classTable);
            }

            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(ext);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final TestExternalizable extn = (TestExternalizable) unmarshaller.readObject();
                assertTrue("No EOF", unmarshaller.read() == -1);
                assertTrue("readExternal was not run", extn.ran);
            }
        }.run();
        assertFalse("readExternal was run on the original", ext.ran);
    }

    public void testMultiWrite() throws Throwable {
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                final TestSerializable obj = new TestSerializable();
                marshaller.writeObject(obj);
                marshaller.writeObject(obj);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertSame(unmarshaller.readObject(), unmarshaller.readObject());
            }
        }.run();
    }

    public void testMultiWrite2() throws Throwable {
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                final TestSerializable obj = new TestSerializable();
                marshaller.writeObjectUnshared(obj);
                marshaller.writeObject(obj);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                assertNotSame(unmarshaller.readObjectUnshared(), unmarshaller.readObject());
            }
        }.run();
    }

    public void testMultiWrite3() throws Throwable {
        new ReadWriteTest() {
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
            }
        }.run();
    }

    public void testBooleanArray() throws Throwable {
        final boolean[] test = new boolean[50];
        test[0] = test[5] = test[6] = test[14] = test[24] = test[39] = test[49] = true;
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                boolean[] result = (boolean[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testByteArray() throws Throwable {
        final byte[] test = new byte[50];
        new Random().nextBytes(test);
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                byte[] result = (byte[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testShortArray() throws Throwable {
        final short[] test = new short[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = (short) rng.nextInt();
        }
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                short[] result = (short[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testIntArray() throws Throwable {
        final int[] test = new int[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextInt();
        }
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                int[] result = (int[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testLongArray() throws Throwable {
        final long[] test = new long[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextLong();
        }
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                long[] result = (long[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testFloatArray() throws Throwable {
        final float[] test = new float[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextFloat();
        }
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                float[] result = (float[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testDoubleArray() throws Throwable {
        final double[] test = new double[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = rng.nextDouble();
        }
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                double[] result = (double[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testCharArray() throws Throwable {
        final char[] test = new char[50];
        final Random rng = new Random();
        for (int i = 0; i < test.length; i++) {
            test[i] = (char) rng.nextInt();
        }
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                char[] result = (char[]) unmarshaller.readObject();
                assertSame(result, unmarshaller.readObject());
                assertTrue(Arrays.equals(test, result));
            }
        }.run();
    }

    public void testObjectArray() throws Throwable {
        final TestSerializable[] test = new TestSerializable[50];
        for (int i = 0; i < 50; i ++) {
            test[i] = new TestSerializable();
        }
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test);
                marshaller.writeObject(test);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object[] objArray = (Object[]) unmarshaller.readObject();
                assertTrue(Arrays.equals(test, objArray));
                assertSame(objArray, unmarshaller.readObject());
            }
        }.run();
    }

    public void testObjectArrayRecursion() throws Throwable {
        final Object[] test = new Object[5];
        for (int i = 0; i < 5; i ++) {
            test[i] = test;
        }
        new ReadWriteTest() {
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
            }
        }.run();
    }

    public void testHashMap() throws Throwable {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("kejlwqewq", "qwejwqioprjweqiorjpofd");
        map.put("34890fdu90uq09rdewq", "wqeioqwdias90ifd0adfw");
        map.put("dsajkljwqej21309ejjfdasfda", "dsajkdqwoid");
        map.put("nczxm,ncoijd0q93wjdwdwq", " dsajkldwqj9edwqu");
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(map);
                marshaller.writeObject(map);
            }

            @SuppressWarnings({"unchecked"})
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Map<String, String> map2 = (Map<String, String>) unmarshaller.readObject();
                assertEquals(map, map2);
                assertSame(map2, unmarshaller.readObject());
            }
        }.run();
    }

    private static final class HashMapExternalizer implements Externalizer {
        private static final long serialVersionUID = 4923778660953773530L;

        @SuppressWarnings({"unchecked"})
        public void writeExternal(final Object subject, final ObjectOutput output) throws IOException {
            final HashMap map = (HashMap) subject;
            output.writeInt(map.size());
            for (Map.Entry e : (Set<Map.Entry>)map.entrySet()) {
                output.writeObject(e.getKey());
                output.writeObject(e.getValue());
            }
        }

        @SuppressWarnings({"unchecked"})
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

    public void testExternalizer() throws Throwable {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("kejlwqewq", "qwejwqioprjweqiorjpofd");
        map.put("34890fdu90uq09rdewq", "wqeioqwdias90ifd0adfw");
        map.put("dsajkljwqej21309ejjfdasfda", "dsajkdqwoid");
        map.put("nczxm,ncoijd0q93wjdwdwq", " dsajkldwqj9edwqu");
        new ReadWriteTest() {
            public void configure(final MarshallingConfiguration configuration) throws Throwable {
                configuration.setExternalizerFactory(new ExternalizerFactory() {
                    @SuppressWarnings({"unchecked"})
                    public Externalizer getExternalizer(final Object instance) {
                        if (instance instanceof HashMap) {
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
            }
        }.run();
    }

    public static class TestA implements Serializable {
        private static final long serialVersionUID = 4788787450574491652L;

        TestB testb;
    }

    public static class TestB implements Serializable {
        private static final long serialVersionUID = 4788787450574491652L;

        TestA testa;
    }

    public void testCircularRefs() throws Throwable {
        final TestA testa = new TestA();
        final TestB testb = new TestB();
        testa.testb = testb;
        testb.testa = testa;
        new ReadWriteTest() {
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
            }
        }.run();
    }

    public static class OuterClass implements Serializable {
        private static final long serialVersionUID = 4268208561523701594L;
        @SuppressWarnings({"UnusedDeclaration"})
        private InnerClass inner;

        public class InnerClass implements Serializable {
            private static final long serialVersionUID = 26916326776519192L;
        }
    }

    public void testInnerClass() throws Throwable {
        final OuterClass outerClass = new OuterClass();
        outerClass.inner = outerClass.new InnerClass();
        final OuterClass.InnerClass innerClass = new OuterClass().new InnerClass();
        new ReadWriteTest() {
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
            }
        }.run();
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

    public void testCircularRefsExt() throws Throwable {
        final TestC testc = new TestC();
        final TestD testd = new TestD();
        testc.testd = testd;
        testd.testc = testc;
        new ReadWriteTest() {
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
            }
        }.run();
    }

    public enum Fruit {
        BANANA,
        APPLE,
        PEAR,
        ORANGE;
    }

    public void testEnum1() throws Throwable {
        new ReadWriteTest() {
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
                assertSame(Boolean.TRUE, unmarshaller.readObject());
                assertSame(Fruit.PEAR, unmarshaller.readObject());
                assertEquals("Goop", unmarshaller.readObject());
                assertSame(Fruit.APPLE, unmarshaller.readObject());
                assertSame(Fruit.PEAR, unmarshaller.readObject());
                assertSame(Fruit.APPLE, unmarshaller.readObject());
            }
        }.run();
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

    public void testProxy() throws Throwable {
        final TestInvocationHandler tih = new TestInvocationHandler();
        final Adder adder = (Adder) Proxy.newProxyInstance(RiverTestCase.class.getClassLoader(), new Class<?>[] { Adder.class }, tih);
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                assertEquals(42, adder.add(41));
                marshaller.writeObject(adder);
                marshaller.writeObject(adder);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                Adder adder = (Adder) unmarshaller.readObject();
                assertSame(adder, unmarshaller.readObject());
                assertEquals(42, adder.add(41));
            }
        }.run();
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

    public void testSerializableWithFields() throws Throwable {
        final TestSerializableWithFields orig = new TestSerializableWithFields();
        orig.value = 123;
        orig.name = "Balahala";
        new ReadWriteTest() {
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
        }.run();
    }

    interface TestInterface extends Serializable {}

    public static final class TestInterfaceFields implements Serializable {
        TestInterface field;
        private static final long serialVersionUID = 2169154160658483773L;
    }

    public void testInterfaceField() throws Throwable {
        final TestInterfaceFields test1 = new TestInterfaceFields();
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test1);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                unmarshaller.readObject();
            }
        }.run();
    }

    interface TestExtInterface extends Externalizable {}

    public static final class TestExtInterfaceFields implements Serializable {
        TestExtInterface field;
        private static final long serialVersionUID = 2169154160658483773L;
    }

    public void testExtInterfaceField() throws Throwable {
        final TestExtInterfaceFields test1 = new TestExtInterfaceFields();
        new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(test1);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                unmarshaller.readObject();
            }
        }.run();
    }
}
