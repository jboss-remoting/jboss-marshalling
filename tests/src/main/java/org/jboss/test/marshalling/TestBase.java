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

package org.jboss.test.marshalling;

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.ReflectiveCreator;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import org.jboss.marshalling.serialization.java.JavaSerializationMarshallerFactory;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.marshalling.serial.SerialMarshallerFactory;
import static org.junit.runners.Parameterized.Parameters;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import static junit.framework.Assert.*;

/**
 *
 */
public abstract class TestBase {

    protected final MarshallerFactory marshallerFactory;
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

    @Parameters
    @SuppressWarnings({ "ZeroLengthArrayAllocation" })
    public static Collection<Object[]> parameters() {
        final List<MarshallerFactory> marshallerFactories = Arrays.<MarshallerFactory>asList(
                new RiverMarshallerFactory(),
                new JavaSerializationMarshallerFactory(),
                new SerialMarshallerFactory()
        );
        final List<StreamHeader> streamHeaders = Arrays.<StreamHeader>asList(
                Marshalling.nullStreamHeader(),
                Marshalling.streamHeader(new byte[] { 1, 2, 3, 4, 5 }),
                null
        );
        final List<Creator> creators = Arrays.<Creator>asList(
                new ReflectiveCreator(),
                new SunReflectiveCreator()
        );

        final Collection<Object[]> c = new ArrayList<Object[]>();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        for (MarshallerFactory factory : marshallerFactories) {
            for (StreamHeader streamHeader : streamHeaders) {
                configuration.setStreamHeader(streamHeader);
                for (Creator creator : creators) {
                    configuration.setCreator(creator);

                    // Add this combination
                    c.add(new Object[] { factory, configuration.clone() });
                }
            }
        }

        return c;
    }


    @SuppressWarnings({ "ConstructorNotProtectedInAbstractClass" })
    public TestBase(MarshallerFactory marshallerFactory, MarshallingConfiguration configuration) {
        this.marshallerFactory = marshallerFactory;
        this.configuration = configuration;
    }

    public void runReadWriteTest(ReadWriteTest readWriteTest) throws Throwable {
        final MarshallingConfiguration configuration = this.configuration.clone();
        readWriteTest.configure(configuration);

        final Marshaller marshaller = marshallerFactory.createMarshaller(configuration);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
        marshaller.start(Marshalling.createByteOutput(baos));
        readWriteTest.runWrite(marshaller);
        marshaller.finish();
        final byte[] bytes = baos.toByteArray();
        final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);
        unmarshaller.start(Marshalling.createByteInput(new ByteArrayInputStream(bytes)));
        readWriteTest.runRead(unmarshaller);
        unmarshaller.finish();
    }
}
