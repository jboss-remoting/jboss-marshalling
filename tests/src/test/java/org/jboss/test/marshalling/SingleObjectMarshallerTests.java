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

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.river.RiverMarshaller;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import org.testng.SkipException;

/**
 * A template for running tests on a single object.
 */
public final class SingleObjectMarshallerTests extends TestBase {

    private final Object subject;

    public SingleObjectMarshallerTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration, Object subject) {
        super(testMarshallerProvider, testUnmarshallerProvider, configuration);
        this.subject = subject;
    }

    private static final Set<Class<?>> noEqualsClasses;
    private static final Set<Class<?>> toStringEqualsClasses;

    static {
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(IdentityHashMap.class);
        set.add(TimeoutException.class);
        set.add(Collections.unmodifiableCollection(new HashSet<Object>()).getClass());
        set.add(TestCollectionHolder.class);
        noEqualsClasses = set;
        set = new HashSet<Class<?>>();
        set.add(StringBuffer.class);
        set.add(StringBuilder.class);
        toStringEqualsClasses = set;
    }

    @Test
    public void test() throws Throwable {
        runReadWriteTest(new ReadWriteTest() {
            public void runWrite(final Marshaller marshaller) throws Throwable {
                if (subject instanceof TestArrayList && marshaller instanceof RiverMarshaller && configuration.getVersion() == -1) {
                    throw new SkipException("TODO Known Issue - JBMAR-61");
                }
                marshaller.writeObject(subject);
                marshaller.writeObject(subject);
                marshaller.writeObject("Test follower");
            }

            public void runRead(final Unmarshaller unmarshaller) throws Throwable {
                Object readSubject = null;
                Object second = null;
                try {
                    readSubject = unmarshaller.readObject();
                    final Class<? extends Object> subjectClass = subject == null ? null : subject.getClass();
                    if (! noEqualsClasses.contains(subjectClass)) {
                        if (toStringEqualsClasses.contains(subjectClass)) {
                            assertEquals(subject.toString(), readSubject.toString());
                        } else {
                            assertEquals(subject, readSubject);
                        }
                    }
                    second = unmarshaller.readObject();
                    assertEqualsOrSame(readSubject, second);
                    assertEquals("Test follower", unmarshaller.readObject());
                    assertEOF(unmarshaller);
                } catch (AssertionError e) {
                    final AssertionError e2 = new AssertionError(String.format("Assertion error occurred.\n\t-- Subject is %s\n\t-- Read Subject is %s\n\t-- Second object is %s\n\t-- Unmarshaller is %s\n\t-- Config is %s",
                            stringOf(subject),
                            stringOf(readSubject),
                            stringOf(second),
                            stringOf(unmarshaller),
                            configuration));
                    e2.setStackTrace(e.getStackTrace());
                    throw e2;
                } catch (Throwable t) {
                    throw new RuntimeException(String.format("Throwable occurred.\n\t-- Subject is %s\n\t-- Read Subject is %s\n\t-- Second object is %s\n\t-- Unmarshaller is %s\n\t-- Config is %s",
                            stringOf(subject),
                            stringOf(readSubject),
                            stringOf(second),
                            stringOf(unmarshaller),
                            configuration), t);
                }
            }
        });
    }

    private static String stringOf(Object foo) {
        return foo == null ? "-null-" : (foo instanceof Class) ? foo.toString() : "(" + foo.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(foo)) + "[" + Integer.toHexString(hashCode(foo)) + "])";
    }

    private static int hashCode(Object obj) {
        if (obj == null) return 0;
        try {
            return obj.hashCode();
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
