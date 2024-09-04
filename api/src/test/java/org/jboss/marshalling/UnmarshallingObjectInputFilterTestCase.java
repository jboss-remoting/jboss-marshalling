/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

package org.jboss.marshalling;

import org.jboss.marshalling.cloner.DateFieldType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Tests UnmarshallingFilter API.
 */
public class UnmarshallingObjectInputFilterTestCase {

    private static final String SERIAL_FILTER_PATTERN = "java.util.ArrayList;!java.util.HashMap;maxarray=10;maxdepth=10;maxrefs=10;maxbytes=10";

    @Test
    public void testFilter() {
        UnmarshallingObjectInputFilter filter = UnmarshallingObjectInputFilter.Factory.createFilter(SERIAL_FILTER_PATTERN);

        // class matching
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(ArrayList.class, 10, 10, 10, 10)),
                UnmarshallingObjectInputFilter.Status.ALLOWED);
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(HashMap.class, 10, 10, 10, 10)),
                UnmarshallingObjectInputFilter.Status.REJECTED);
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(Hashtable.class, 10, 10, 10, 10)),
                UnmarshallingObjectInputFilter.Status.UNDECIDED);

        // undecided class, one of the limit checks rejecting
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(Hashtable.class, 11, 10, 10, 10)),
                UnmarshallingObjectInputFilter.Status.REJECTED);
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(Hashtable.class, 10, 11, 10, 10)),
                UnmarshallingObjectInputFilter.Status.REJECTED);
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(Hashtable.class, 10, 10, 11, 10)),
                UnmarshallingObjectInputFilter.Status.REJECTED);
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(Hashtable.class, 10, 10, 10, 11)),
                UnmarshallingObjectInputFilter.Status.REJECTED);

        // accepting filter before rejecting filters
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(ArrayList.class, 11, 11, 11, 11)),
                UnmarshallingObjectInputFilter.Status.ALLOWED);
    }

    @Test
    public void testWildcard() {
        UnmarshallingObjectInputFilter filter = UnmarshallingObjectInputFilter.Factory.createFilter("java.util.*;!org.jboss.marshalling.*");

        // accepts java.util.*
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(ArrayList.class, -1, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.ALLOWED);
        // undecided on java.lang.Integer, not mentioned in the filter
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(Integer.class, -1, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.UNDECIDED);
        // rejects org.jboss.marshalling.*
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(TestClass.class, -1, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.REJECTED);
        // undecided on org.jboss.marshalling.cloner.DateFieldType, which is in nested package not mentioned in the filter
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(DateFieldType.class, -1, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.UNDECIDED);
    }

    @Test
    public void testNestingWildcard() {
        UnmarshallingObjectInputFilter filter = UnmarshallingObjectInputFilter.Factory.createFilter("!org.jboss.marshalling.**");

        // undecided on java.lang.Integer, not mentioned in the filter
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(Integer.class, -1, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.UNDECIDED);
        // rejects everything under org.jboss.marshalling.**
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(TestClass.class, -1, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.REJECTED);
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(DateFieldType.class, -1, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.REJECTED);
    }

    @Test
    public void testNullClass() {
        UnmarshallingObjectInputFilter filter = UnmarshallingObjectInputFilter.Factory.createFilter("java.util.ArrayList;java.util.*;java.util.**");
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(null, 0, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.UNDECIDED);

        filter = UnmarshallingObjectInputFilter.Factory.createFilter("*");
        Assert.assertEquals(filter.checkInput(new TestFilterInfo(null, 0, 0, 0, 0)),
                UnmarshallingObjectInputFilter.Status.ALLOWED);
    }

    @Test
    public void testInvalidFilters() {
        assertInvalid("org.jboss.marshalling.***");
        assertInvalid("modulename/org.jboss.marshalling.***");
        assertInvalid("maxarray=1=1");
    }

    private void assertInvalid(String spec) {
        try {
            UnmarshallingObjectInputFilter.Factory.createFilter(spec);
            Assert.fail("Filter creation was expected to fail: " + spec);
        } catch (IllegalArgumentException expected) {}
    }

    static class TestFilterInfo implements UnmarshallingObjectInputFilter.FilterInfo {

        private final Class<?> unmarshalledClass;
        private final long arrayLength;
        private final long depth;
        private final long references;
        private final long streamBytes;

        public TestFilterInfo(Class<?> unmarshalledClass, long arrayLength, long depth, long references, long streamBytes) {
            this.unmarshalledClass = unmarshalledClass;
            this.arrayLength = arrayLength;
            this.depth = depth;
            this.references = references;
            this.streamBytes = streamBytes;
        }

        @Override
        public Class<?> getUnmarshalledClass() {
            return unmarshalledClass;
        }

        @Override
        public long getArrayLength() {
            return arrayLength;
        }

        @Override
        public long getDepth() {
            return depth;
        }

        @Override
        public long getReferences() {
            return references;
        }

        @Override
        public long getStreamBytes() {
            return streamBytes;
        }
    }

    static class TestClass implements Serializable {
    }
}
