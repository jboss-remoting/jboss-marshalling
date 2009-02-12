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

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.jboss.marshalling.MarshallingConfiguration;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 */
@Test
public final class SingleObjectMarshallerTestFactory {

    @DataProvider(name = "singleObjectProvider")
    @SuppressWarnings({ "ZeroLengthArrayAllocation" })
    public static Object[][] parameters() {
        final List<Object[]> newList = new ArrayList<Object[]>();
        for (Object[] objects : SimpleMarshallerTestFactory.parameters()) {
            for (Object object : testObjects) {
                final Object[] params = new Object[4];
                System.arraycopy(objects, 0, params, 0, 3);
                params[3] = object;
                newList.add(params);
            }
        }
        return newList.toArray(new Object[newList.size()][]);
    }

    private static Map<Object, Object> testMap() {
        final HashMap<Object, Object> map = new HashMap<Object, Object>();
        map.put(Integer.valueOf(1694), "Test");
        map.put("Blah blah", Boolean.TRUE);
        return map;
    }

    private static List<Object> testList() {
        final ArrayList<Object> list = new ArrayList<Object>();
        list.add(Integer.valueOf(478392));
        list.add("A string");
        list.add(Boolean.FALSE);
        list.add(Boolean.TRUE);
        return list;
    }

    private static final Object[] testObjects = new Object[] {
            new TestComplexObject(true, (byte)5, 'c', (short)8192, 294902, 319203219042L, 21.125f, 42.625, "TestString", new HashSet<Object>(Arrays.asList("Hello", Boolean.TRUE, Integer.valueOf(12345)))),
            new TestComplexExternalizableObject(true, (byte)5, 'c', (short)8192, 294902, 319203219042L, 21.125f, 42.625, "TestString", new HashSet<Object>(Arrays.asList("Hello", Boolean.TRUE, Integer.valueOf(12345)))),
            Integer.valueOf(1234),
            Boolean.TRUE,
            testMap(),
            testList(),
            Collections.unmodifiableMap(testMap()),
            Collections.unmodifiableList(testList()),
    };

    @Factory(dataProvider = "singleObjectProvider")
    public Object[] getTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration, Object subject) {
        return new Object[] { new SingleObjectMarshallerTests(testMarshallerProvider, testUnmarshallerProvider, configuration, subject) };
    }
}
