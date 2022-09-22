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

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 *
 * @author rmartinc
 */
@Test(dataProvider = "recordProvider")
public class RecordTestFactory {

    @DataProvider (name = "recordProvider")
    public static Object[][] parameters() {

        Object[][] tests = new Object[6][3];

        final MarshallingConfiguration config = new MarshallingConfiguration();
        final MarshallerFactory riverMarshallerFactory = Marshalling.getProvidedMarshallerFactory("river");

        tests[0][0] = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 2);
        tests[0][1] = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 2);
        tests[0][2] = config;

        tests[1][0] = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 3);
        tests[1][1] = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 3);
        tests[1][2] = config;

        tests[2][0] = new MarshallerFactoryTestMarshallerProvider(riverMarshallerFactory, 4);
        tests[2][1] = new MarshallerFactoryTestUnmarshallerProvider(riverMarshallerFactory, 4);
        tests[2][2] = config;

        final MarshallerFactory serialMarshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
        tests[3][0] = new MarshallerFactoryTestMarshallerProvider(serialMarshallerFactory);
        tests[3][1] = new MarshallerFactoryTestUnmarshallerProvider(serialMarshallerFactory);
        tests[3][2] = config;

        tests[4][0] = new MarshallerFactoryTestMarshallerProvider(serialMarshallerFactory);
        tests[4][1] = new ObjectInputStreamTestUnmarshallerProvider();
        tests[4][2] = config;

        tests[5][0] = new ObjectOutputStreamTestMarshallerProvider();
        tests[5][1] = new MarshallerFactoryTestUnmarshallerProvider(serialMarshallerFactory);
        tests[5][2] = config;

        return tests;
    }


    @Factory(dataProvider = "recordProvider")
    public Object[] getTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        return new Object[] {new RecordTests(testMarshallerProvider, testUnmarshallerProvider, configuration)};
    }
}
