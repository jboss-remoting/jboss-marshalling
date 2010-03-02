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
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import static org.jboss.marshalling.Pair.create;
import org.jboss.marshalling.Pair;
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

        final MarshallerFactory riverMarshallerFactory = Marshalling.getMarshallerFactory("river");
        final TestMarshallerProvider riverTestMarshallerProviderV2 = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 2);
        final TestUnmarshallerProvider riverTestUnmarshallerProviderV2 = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 2);

        final TestMarshallerProvider riverTestMarshallerProviderV3 = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 3);
        final TestUnmarshallerProvider riverTestUnmarshallerProviderV3 = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 3);

        final MarshallerFactory serialMarshallerFactory = Marshalling.getMarshallerFactory("serial");
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

        return c.toArray(new Object[c.size()][]);
    }


    @Factory(dataProvider = "baseProvider")
    public Object[] getTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        return new Object[] { new SimpleMarshallerTests(testMarshallerProvider, testUnmarshallerProvider, configuration) };
    }
}
