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

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.testng.annotations.Test;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import static org.testng.AssertJUnit.*;

/**
 * A template for running tests on a single object.
 */
public final class SingleObjectMarshallerTests extends TestBase {

    private final Object subject;

    public SingleObjectMarshallerTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration, Object subject) {
        super(testMarshallerProvider, testUnmarshallerProvider, configuration);
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
