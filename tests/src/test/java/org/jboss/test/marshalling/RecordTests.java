/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 *
 * @author rmartinc
 */
public class RecordTests extends TestBase {

    public RecordTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        super(testMarshallerProvider, testUnmarshallerProvider, configuration);
    }

    public static record NonSerialized(int one, long two) {};

    @Test(expectedExceptions = NotSerializableException.class)
    public void testNonSerializable() throws Throwable {
        NonSerialized ns = new NonSerialized(1, 2L);
        ReadWriteTest readWriteTest = new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(ns);
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
            }
        };
        runWriteOnly(readWriteTest);
    }

    public static record Empty() implements Serializable {};

    @Test
    public void testEmpty() throws Throwable {
        Empty e1 = new Empty();
        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject("Blah");
                marshaller.writeObject(e1);
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
                AssertJUnit.assertEquals(e1, unmarshaller.readObject());
            }
        });
    }

    public static record Person(String firstname, String lastname, int age, boolean enabled) implements Serializable {};

    @Test
    public void testPerson() throws Throwable {
        Person p1 = new Person("John", "Doe", 31, true);
        Person p2 = null;
        Person p3 = new Person("jane", null, 35, false);
        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject("Blah");
                marshaller.writeObject(p1);
                marshaller.writeObject(p2);
                marshaller.writeObject(p3);
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
                AssertJUnit.assertEquals(p1, unmarshaller.readObject());
                AssertJUnit.assertNull(unmarshaller.readObject());
                AssertJUnit.assertEquals(p3, unmarshaller.readObject());
            }
        });
    }

    public static record Marriage(Person p1, Person p2, String address) implements Serializable {};

    @Test
    public void testMariage() throws Throwable {
        Person p1 = new Person("John", "Doe", 31, true);
        Person p2 = new Person("Jane", "Doe", 35, false);
        Marriage m = new Marriage(p1, p2, "22 Acacia Avenue");
        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject("Blah");
                marshaller.writeObject(m);
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
                AssertJUnit.assertEquals(m, unmarshaller.readObject());
            }
        });
    }

    @Test
    public void testArray() throws Throwable {
        Person p1 = new Person("John", "Doe", 31, true);
        Person p2 = new Person("Jane", "Doe", 35, false);
        Person[] array = new Person[]{p1, p2, p1};
        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject("Blah");
                marshaller.writeObject(array);
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
                AssertJUnit.assertArrayEquals(array, (Person[]) unmarshaller.readObject());
            }
        });
    }

    @Test
    public void testList() throws Throwable {
        Person p1 = new Person("John", "Doe", 31, true);
        Person p2 = new Person("Jane", "Doe", 35, false);
        Person p3 = new Person("Kate", "Doe", 2, true);
        List<Person> list = new ArrayList<>();
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p2);
        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject("Blah");
                marshaller.writeObject(list);
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
                AssertJUnit.assertEquals(list, (List<?>) unmarshaller.readObject());
            }
        });
    }

    public static class FullHouse implements Serializable {
        private Marriage marriage;
        private Person[] children;
        private int rooms;
        private String description;

        public FullHouse(Marriage marriage) {
            this.marriage = marriage;
        }

        public Marriage getMarriage() {
            return marriage;
        }

        public void setMarriage(Marriage marriage) {
            this.marriage = marriage;
        }

        public Person[] getChildren() {
            return children;
        }

        public void setChildren(Person[] children) {
            this.children = children;
        }

        public void addChild(Person c) {
            if (children == null) {
                children = new Person[1];
                children[0] = c;
            } else {
                Person[] newChildren = Arrays.copyOf(children, children.length + 1);
                newChildren[newChildren.length - 1] = c;
                children = newChildren;
            }
        }

        public int getRooms() {
            return rooms;
        }

        public void setRooms(int rooms) {
            this.rooms = rooms;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public int hashCode() {
            return 89 * (89 * 7 + Objects.hash(marriage, rooms, description))
                    + Arrays.deepHashCode(this.children);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final FullHouse other = (FullHouse) obj;
            return this.rooms == other.rooms
                    && Objects.equals(this.description, other.description)
                    && Objects.equals(this.marriage, other.marriage)
                    && Arrays.deepEquals(this.children, other.children);
        }

        @Override
        public String toString() {
            return new StringBuilder().append(super.toString())
                    .append(" marriage=").append(marriage)
                    .append(" children=") .append(children == null? "null" : Arrays.asList(children))
                    .append(" rooms=").append(rooms)
                    .append(" description=").append(description)
                    .toString();
        }
    }

    @Test
    public void testFullHouse() throws Throwable {
        Person p1 = new Person("John", "Doe", 31, true);
        Person p2 = new Person("Jane", "Doe", 35, false);
        Marriage m = new Marriage(p1, p2, "22 Acacia Avenue");
        FullHouse h1 = new FullHouse(m);
        h1.addChild(new Person("Jhon", "Doe Jr", 10, true));
        h1.addChild(new Person("Janeth", "Doe", 8, true));
        h1.setDescription("A happy house!!!");
        h1.setRooms(5);
        FullHouse h2 = new FullHouse(m);
        h2.setRooms(3);

        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(h1);
                marshaller.writeObject(h2);
                marshaller.writeObject("Blah");
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals(h1, (FullHouse) unmarshaller.readObject());
                AssertJUnit.assertEquals(h2, (FullHouse) unmarshaller.readObject());
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
            }
        });
    }

    @Test
    public void testFullHouseArray() throws Throwable {
        Person p1 = new Person("John", "Doe", 31, true);
        Person p2 = new Person("Jane", "Doe", 35, false);
        Marriage m = new Marriage(p1, p2, "22 Acacia Avenue");
        FullHouse h1 = new FullHouse(m);
        h1.addChild(new Person("Jhon", "Doe Jr", 10, true));
        h1.addChild(new Person("Janeth", "Doe", 8, true));
        h1.setDescription("A happy house!!!");
        h1.setRooms(5);
        FullHouse h2 = new FullHouse(m);
        h2.setRooms(3);
        FullHouse[] array = new FullHouse[]{h1, h2};

        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(h1);
                marshaller.writeObject(array);
                marshaller.writeObject("Blah");
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals(h1, (FullHouse) unmarshaller.readObject());
                AssertJUnit.assertArrayEquals(array, (FullHouse[]) unmarshaller.readObject());
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
            }
        });
    }

    @Test
    public void testSet() throws Throwable {
        Person p1 = new Person("John", "Doe", 31, true);
        Person p2 = new Person("Jane", "Doe", 35, false);
        Marriage m = new Marriage(p1, p2, "22 Acacia Avenue");
        FullHouse h1 = new FullHouse(m);
        h1.addChild(new Person("Jhon", "Doe Jr", 10, true));
        h1.addChild(new Person("Janeth", "Doe", 8, true));
        h1.setDescription("A happy house!!!");
        h1.setRooms(5);
        FullHouse h2 = new FullHouse(m);
        h2.setRooms(3);
        Set<FullHouse> set = new HashSet<>();
        set.add(h1);
        set.add(h2);
        runReadWriteTest(new ReadWriteTest() {
            @Override
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(h1);
                marshaller.writeObject(set);
                marshaller.writeObject("Blah");
            }

            @Override
            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                AssertJUnit.assertEquals(h1, (FullHouse) unmarshaller.readObject());
                AssertJUnit.assertEquals(set, (Set<?>) unmarshaller.readObject());
                AssertJUnit.assertEquals("Blah", unmarshaller.readObject());
            }
        });
    }
}
