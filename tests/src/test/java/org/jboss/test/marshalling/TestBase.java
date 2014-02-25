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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

/**
 *
 */
public abstract class TestBase {

    protected final TestMarshallerProvider testMarshallerProvider;
    protected final TestUnmarshallerProvider testUnmarshallerProvider;
    protected final MarshallingConfiguration configuration;

    public static void assertEOF(final ObjectInput objectInput) throws IOException {
        assertTrue("No EOF", objectInput.read() == -1);
    }

    @SuppressWarnings("unchecked")
    private static final Set<Class<?>> nonSameClasses = new HashSet<Class<?>>(Arrays.asList(
            Boolean.class,
            Byte.class,
            Character.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class
    ));

    public static void assertEqualsOrSame(final Object a, final Object b) {
        if (a == null || b == null) {
            assertTrue(a == b);
        } else {
            if (nonSameClasses.contains(a.getClass())) {
                assertEquals(a, b);
            } else {
                assertSame(a, b);
            }
        }
    }

    public static void assertEqualsOrSame(final String msg, final Object a, final Object b) {
        if (a == null || b == null) {
            assertTrue(msg, a == b);
        } else {
            if (nonSameClasses.contains(a.getClass())) {
                assertEquals(msg, a, b);
            } else {
                assertSame(msg, a, b);
            }
        }
    }

    @SuppressWarnings({ "ConstructorNotProtectedInAbstractClass" })
    public TestBase(final TestMarshallerProvider testMarshallerProvider, final TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        this.testMarshallerProvider = testMarshallerProvider;
        this.testUnmarshallerProvider = testUnmarshallerProvider;
        this.configuration = configuration;
    }

    public void runReadWriteTest(ReadWriteTest readWriteTest) throws Throwable {
        final MarshallingConfiguration readConfiguration = configuration.clone();
        readWriteTest.configureRead(readConfiguration);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
        final ByteOutput byteOutput = Marshalling.createByteOutput(baos);

        System.out.println("Read Configuration = " + readConfiguration);
        final Marshaller marshaller = testMarshallerProvider.create(readConfiguration, byteOutput);
        System.out.println("Marshaller = " + marshaller + " (version set to " + readConfiguration.getVersion() + ")");
        readWriteTest.runWrite(marshaller);
        marshaller.finish();
        final byte[] bytes = baos.toByteArray();

        final MarshallingConfiguration writeConfiguration = configuration.clone();
        readWriteTest.configureWrite(writeConfiguration);

        final ByteInput byteInput = Marshalling.createByteInput(new ByteArrayInputStream(bytes));
        System.out.println("Write Configuration = " + writeConfiguration);
        final Unmarshaller unmarshaller = testUnmarshallerProvider.create(writeConfiguration, byteInput);
        System.out.println("Unmarshaller = " + unmarshaller + " (version set to " + writeConfiguration.getVersion() + ")");
        readWriteTest.runRead(unmarshaller);
        unmarshaller.finish();
    }
}
