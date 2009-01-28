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
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.testsupport.TestSuiteHelper;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import static junit.framework.Assert.*;

/**
 * A template for running tests on a single object.
 */
@RunWith(Parameterized.class)
public final class SingleObjectTests extends TestBase {
    public static junit.framework.Test suite() {
        return TestSuiteHelper.testSuiteFor(SingleObjectTests.class);
    }

    private final Object subject;

    private static Map<Object, Object> testMap() {
        final HashMap<Object, Object> map = new HashMap<Object, Object>();
        map.put(Integer.valueOf(1694), "Test");
        map.put("Blah blah", Boolean.TRUE);
        return map;
    }

    private static final Object[] testObjects = new Object[] {
            new TestComplexObject(true, (byte)5, 'c', (short)8192, 294902, 319203219042L, 21.125f, 42.625, "TestString", new HashSet<Object>(Arrays.asList("Hello", Boolean.TRUE, Integer.valueOf(12345)))),
            new TestComplexExternalizableObject(true, (byte)5, 'c', (short)8192, 294902, 319203219042L, 21.125f, 42.625, "TestString", new HashSet<Object>(Arrays.asList("Hello", Boolean.TRUE, Integer.valueOf(12345)))),
            Integer.valueOf(1234),
            Boolean.TRUE,
            testMap(),
    };

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final Collection<Object[]> c = new ArrayList<Object[]>();
        for (Object[] p : TestBase.parameters()) {
            for (Object o : testObjects) {
                c.add(new Object[] { p[0], ((MarshallingConfiguration)p[1]).clone(), o });
            }
        }
        return c;
    }

    public SingleObjectTests(MarshallerFactory marshallerFactory, MarshallingConfiguration configuration, Object subject) {
        super(marshallerFactory, configuration);
        System.out.printf("Using %s, %s, %s\n", marshallerFactory, configuration, subject);
        this.subject = subject;
    }

    @Test
    public void test() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                marshaller.writeObject(subject);
                marshaller.writeObject(subject);
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                final Object readSubject = unmarshaller.readObject();
                assertEquals(subject, readSubject);
                assertEqualsOrSame(readSubject, unmarshaller.readObject());
                assertEOF(unmarshaller);
            }
        });
    }
}
