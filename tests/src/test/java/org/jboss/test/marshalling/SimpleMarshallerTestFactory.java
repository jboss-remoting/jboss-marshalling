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

import java.io.IOException;
import org.jboss.marshalling.AbstractClassResolver;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.testng.annotations.Factory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import static org.jboss.marshalling.Pair.create;
import static org.testng.Assert.assertEquals;

import org.jboss.marshalling.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 *
 */
@Test(dataProvider="baseProvider")
public final class SimpleMarshallerTestFactory {

    @DataProvider(name = "baseProvider")
    @SuppressWarnings({ "ZeroLengthArrayAllocation" })
    public static Object[][] parameters() {

        final MarshallerFactory riverMarshallerFactory = Marshalling.getProvidedMarshallerFactory("river");
        final TestMarshallerProvider riverTestMarshallerProviderV2 = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 2);
        final TestUnmarshallerProvider riverTestUnmarshallerProviderV2 = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 2);

        final TestMarshallerProvider riverTestMarshallerProviderV3 = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 3);
        final TestUnmarshallerProvider riverTestUnmarshallerProviderV3 = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 3);

        final MarshallerFactory serialMarshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
        final TestMarshallerProvider serialTestMarshallerProvider = new MarshallerFactoryTestMarshallerProvider(serialMarshallerFactory);
        final TestUnmarshallerProvider serialTestUnmarshallerProvider = new MarshallerFactoryTestUnmarshallerProvider(serialMarshallerFactory);

        final TestMarshallerProvider oosTestMarshallerProvider = new ObjectOutputStreamTestMarshallerProvider();
        final TestUnmarshallerProvider oisTestUnmarshallerProvider = new ObjectInputStreamTestUnmarshallerProvider();

        @SuppressWarnings("unchecked")
        final List<Pair<TestMarshallerProvider, TestUnmarshallerProvider>> marshallerProviderPairs = Arrays.asList(
                // river - v2 writer, v2 reader
                create(riverTestMarshallerProviderV2, riverTestUnmarshallerProviderV2),
                // river - v2 writer, v3 reader
                create(riverTestMarshallerProviderV2, riverTestUnmarshallerProviderV3),
                // river - v3 writer, v3 reader
                create(riverTestMarshallerProviderV3, riverTestUnmarshallerProviderV3),

                // serial
                create(serialTestMarshallerProvider, serialTestUnmarshallerProvider),
                create(serialTestMarshallerProvider, oisTestUnmarshallerProvider),
                create(oosTestMarshallerProvider, serialTestUnmarshallerProvider),
                null
        );

        final Collection<Object[]> c = new ArrayList<Object[]>();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        for (Pair<TestMarshallerProvider, TestUnmarshallerProvider> pair : marshallerProviderPairs) {
            if (pair == null) continue;
            // Add this combination
            c.add(new Object[] { pair.getA(), pair.getB(), configuration.clone() });
        }
        configuration.setClassResolver(new AbstractClassResolver() {
            protected ClassLoader getClassLoader() {
                return SimpleMarshallerTestFactory.class.getClassLoader();
            }

            public void annotateProxyClass(final Marshaller marshaller, final Class<?> proxyClass) throws IOException {
                marshaller.writeObject("Test One");
                marshaller.writeObject("Test Two");
            }

            public void annotateClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
                marshaller.writeObject("Test One");
                marshaller.writeObject("Test Two");
            }

            public Class<?> resolveProxyClass(final Unmarshaller unmarshaller, final String[] interfaces) throws IOException, ClassNotFoundException {
                assertEquals(unmarshaller.readObject(String.class), "Test One", "for proxy");
                assertEquals(unmarshaller.readObject(String.class), "Test Two", "for proxy");
                return super.resolveProxyClass(unmarshaller, interfaces);
            }

            public Class<?> resolveClass(final Unmarshaller unmarshaller, final String name, final long serialVersionUID) throws IOException, ClassNotFoundException {
                assertEquals(unmarshaller.readObject(String.class), "Test One", "for class " + name);
                assertEquals(unmarshaller.readObject(String.class), "Test Two", "for class " + name);
                return super.resolveClass(unmarshaller, name, serialVersionUID);
            }

            public String toString() {
                return "Test Class Resolver";
            }
        });
        for (Pair<TestMarshallerProvider, TestUnmarshallerProvider> pair : marshallerProviderPairs) {
            if (pair == null) continue;
            // Add this combination
            c.add(new Object[] { pair.getA(), pair.getB(), configuration.clone() });
        }

        return c.toArray(new Object[c.size()][]);
    }


    @Factory(dataProvider = "baseProvider")
    public Object[] getTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        return new Object[] { new SimpleMarshallerTests(testMarshallerProvider, testUnmarshallerProvider, configuration) };
    }
}
