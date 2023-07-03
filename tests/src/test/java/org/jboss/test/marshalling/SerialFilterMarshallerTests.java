/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ObjectInputStreamUnmarshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.UnmarshallingFilter;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InvalidClassException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import static org.testng.AssertJUnit.fail;

/**
 * Tests that various collection types can be limited in size via unmarshalling filter.
 */
@SuppressWarnings("UnnecessaryBoxing")
public class SerialFilterMarshallerTests extends TestBase {

    private static final int PAYLOAD_SIZE = 20;
    private static final String REJECTING_LIMIT_PATTERN = "maxarray=10;maxdepth=10;maxrefs=10";
    private static final String ACCEPTING_LIMIT_PATTERN = "maxarray=100;maxdepth=100;maxrefs=100";

    /**
     * Constructor to be used by a test suite.
     */
    public SerialFilterMarshallerTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        super(testMarshallerProvider, testUnmarshallerProvider, configuration);
    }

    /**
     * Simple constructor for running one test at a time from an IDE.
     */
    @SuppressWarnings("unused")
    public SerialFilterMarshallerTests() {
        super(new MarshallerFactoryTestMarshallerProvider(new RiverMarshallerFactory(), 4),
                new MarshallerFactoryTestUnmarshallerProvider(new RiverMarshallerFactory(), 4),
                getMarshallingConfiguration(4));
        /*super(new MarshallerFactoryTestMarshallerProvider(new SerialMarshallerFactory(), 5),
                new MarshallerFactoryTestUnmarshallerProvider(new SerialMarshallerFactory(), 5),
                getMarshallingConfiguration(5));*/
    }

    /**
     * Creates payload collections to test.
     */
    @DataProvider
    public Object[][] collectionPayloads() {
        return new Object[][]{
                new Object[]{new Integer[PAYLOAD_SIZE]},
                new Object[]{new String[PAYLOAD_SIZE]},
                new Object[]{new int[PAYLOAD_SIZE]},
                new Object[]{fillCollection(new ArrayList<>())},
                new Object[]{fillCollection(new LinkedList<>())},
                new Object[]{fillCollection(new HashSet<>())},
                new Object[]{fillCollection(new LinkedHashSet<>())},
                new Object[]{fillCollection(new TreeSet<>())},
                new Object[]{fillCollection(new Vector<>())},
                new Object[]{fillCollection(new Stack<>())},
                new Object[]{fillCollection(new ArrayDeque<>())},
                new Object[]{fillMap(new HashMap<>())},
                new Object[]{fillMap(new LinkedHashMap<>())},
                new Object[]{fillMap(new TreeMap<>())},
                new Object[]{fillMap(new Hashtable<>())},
                new Object[]{fillMap(new IdentityHashMap<>())}
        };
    }

    @DataProvider
    public Object[][] otherPayloads() {
        return new Object[][]{
                new Object[]{new Short[0]}, // ID_ARRAY_EMPTY
                new Object[]{Integer.valueOf(1)}, // ID_INTEGER_OBJECT
        };
    }

    @Test(dataProvider = "collectionPayloads")
    public void testCollectionsRejectingLimitsFilter(Object payload) throws Throwable {
        runReadWriteTest(new RejectingUnmarshallingFilterReadWriteTest(REJECTING_LIMIT_PATTERN, payload));
    }

    @Test(dataProvider = "collectionPayloads")
    public void testCollectionsAcceptingLimitsFilter(Object payload) throws Throwable {
        runReadWriteTest(new AcceptingUnmarshallingFilterReadWriteTest(ACCEPTING_LIMIT_PATTERN, payload));
    }

    @Test(dataProvider = "collectionPayloads")
    public void testCollectionsRejectingClassFilter(Object payload) throws Throwable {
        skipPrimitivePayloads(payload);
        String payloadClassName = payloadType(payload).getName();
        runReadWriteTest(new RejectingUnmarshallingFilterReadWriteTest("!" + payloadClassName, payload));
    }

    @Test(dataProvider = "otherPayloads")
    public void testOtherPayloads(Object payload) throws Throwable {
        String payloadClassName = payloadType(payload).getName();
        runReadWriteTest(new RejectingUnmarshallingFilterReadWriteTest("!" + payloadClassName, payload));
    }


    /**
     * Sets unmarshalling filter and serializes & deserializes a payload.
     */
    private static abstract class AbstractUnmarshallingFilterReadWriteTest extends ReadWriteTest {
        protected final String filterSpec;
        protected final Object payload;

        AbstractUnmarshallingFilterReadWriteTest(final String filterSpec, final Object payload) {
            this.filterSpec = filterSpec;
            this.payload = payload;
        }

        @Override
        public void configure(MarshallingConfiguration configuration) throws Throwable {
            configuration.setUnmarshallingFilter(UnmarshallingFilter.Factory.createFilter(filterSpec));
        }

        @Override
        public void runWrite(final Marshaller marshaller) throws Throwable {
            marshaller.writeObject(payload);
        }

        @Override
        public abstract void runRead(Unmarshaller unmarshaller) throws Throwable;
    }

    /**
     * Checks that unmarshalling filter rejects a payload.
     */
    private class RejectingUnmarshallingFilterReadWriteTest extends AbstractUnmarshallingFilterReadWriteTest {

        RejectingUnmarshallingFilterReadWriteTest(String filterSpec, Object payload) {
            super(filterSpec, payload);
        }

        @Override
        public void runRead(Unmarshaller unmarshaller) throws Throwable {
            if (unmarshaller instanceof ObjectInputStreamUnmarshaller) {
                throw new SkipException("Test not relevant for " + unmarshaller);
            }
            try {
                unmarshaller.readObject();
                fail(String.format("Payload %s %s should have been rejected by filter spec %s. [%s, %s, %s]",
                        payload.getClass().getSimpleName(), payload, filterSpec, testMarshallerProvider, testUnmarshallerProvider, configuration));
            } catch (InvalidClassException e) {
                // expected
            }
        }
    }

    /**
     * Checks that unmarshalling filter accepts a payload.
     */
    private class AcceptingUnmarshallingFilterReadWriteTest extends AbstractUnmarshallingFilterReadWriteTest {

        AcceptingUnmarshallingFilterReadWriteTest(String filterSpec, Object payload) {
            super(filterSpec, payload);
        }

        @Override
        public void runRead(Unmarshaller unmarshaller) throws Throwable {
            if (unmarshaller instanceof ObjectInputStreamUnmarshaller) {
                throw new SkipException("Test not relevant for " + unmarshaller);
            }
            try {
                unmarshaller.readObject();
            } catch (InvalidClassException e) {
                fail(String.format("Payload %s %s should *not* have been rejected by filter spec %s. [%s, %s, %s]",
                        payload.getClass().getSimpleName(), payload, filterSpec, testMarshallerProvider, testUnmarshallerProvider, configuration));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static MarshallingConfiguration getMarshallingConfiguration(int version) {
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setVersion(version);
        return marshallingConfiguration;
    }

    private static <T extends Collection<Integer>> T fillCollection(T c) {
        for (int i = 0; i < PAYLOAD_SIZE; i++) {
            c.add(i);
        }
        return c;
    }

    private static <T extends Map<Integer, Integer>> T fillMap(T m) {
        for (int i = 0; i < PAYLOAD_SIZE; i++) {
            m.put(i, i);
        }
        return m;
    }

    private static Class<?> payloadType(Object payload) {
        return payload.getClass().isArray() ? payload.getClass().getComponentType() : payload.getClass();
    }

    private static void skipPrimitivePayloads(Object payload) {
        if (payloadType(payload).isPrimitive()) {
            throw new SkipException("Primitive types are allowed by default.");
        }
    }

}
