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

import org.testng.annotations.Factory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.marshalling.serial.SerialMarshallerFactory;
import org.jboss.marshalling.serialization.java.JavaSerializationMarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import static org.jboss.test.marshalling.Pair.pair;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 *
 */
@Test
public final class SimpleMarshallerTestFactory {

    @DataProvider(name = "baseProvider")
    @SuppressWarnings({ "ZeroLengthArrayAllocation" })
    public static Object[][] parameters() {

        final RiverMarshallerFactory riverMarshallerFactory = new RiverMarshallerFactory();
        final TestMarshallerProvider riverTestMarshallerProviderV1 = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 1);
        final TestUnmarshallerProvider riverTestUnmarshallerProviderV1 = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 1);

        final TestMarshallerProvider riverTestMarshallerProviderV2 = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 2);
        final TestUnmarshallerProvider riverTestUnmarshallerProviderV2 = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 2);

        final SerialMarshallerFactory serialMarshallerFactory = new SerialMarshallerFactory();
        final TestMarshallerProvider serialTestMarshallerProvider = new MarshallerFactoryTestMarshallerProvider(serialMarshallerFactory);
        final TestUnmarshallerProvider serialTestUnmarshallerProvider = new MarshallerFactoryTestUnmarshallerProvider(serialMarshallerFactory);

        final JavaSerializationMarshallerFactory javaSerializationMarshallerFactory = new JavaSerializationMarshallerFactory();
        final TestMarshallerProvider javaTestMarshallerProvider = new MarshallerFactoryTestMarshallerProvider(javaSerializationMarshallerFactory);
        final TestUnmarshallerProvider javaTestUnmarshallerProvider = new MarshallerFactoryTestUnmarshallerProvider(javaSerializationMarshallerFactory);

        final TestMarshallerProvider oosTestMarshallerProvider = new ObjectOutputStreamTestMarshallerProvider();
        final TestUnmarshallerProvider oisTestUnmarshallerProvider = new ObjectInputStreamTestUnmarshallerProvider();

        @SuppressWarnings("unchecked")
        final List<Pair<TestMarshallerProvider, TestUnmarshallerProvider>> marshallerProviderPairs = Arrays.asList(
                // river - v1 writer, v1 reader
                pair(riverTestMarshallerProviderV1, riverTestUnmarshallerProviderV1),
                // river - v1 writer, v2 reader
                pair(riverTestMarshallerProviderV1, riverTestUnmarshallerProviderV2),
                // river - v2 writer, v2 reader
                pair(riverTestMarshallerProviderV2, riverTestUnmarshallerProviderV2),

                // serial
                pair(serialTestMarshallerProvider, serialTestUnmarshallerProvider),
                pair(serialTestMarshallerProvider, oisTestUnmarshallerProvider),
                pair(oosTestMarshallerProvider, serialTestUnmarshallerProvider),
                // reflection java serialization
                pair(javaTestMarshallerProvider, javaTestUnmarshallerProvider),
                pair(javaTestMarshallerProvider, oisTestUnmarshallerProvider),
                pair(oosTestMarshallerProvider, javaTestUnmarshallerProvider),
                null
        );

        final Collection<Object[]> c = new ArrayList<Object[]>();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setCreator(new SunReflectiveCreator());
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
