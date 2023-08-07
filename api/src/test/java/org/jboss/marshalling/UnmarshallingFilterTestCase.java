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
public class UnmarshallingFilterTestCase {

    private static final String SERIAL_FILTER_PATTERN = "java.util.ArrayList;!java.util.HashMap;maxarray=10;maxdepth=10;maxrefs=10;maxbytes=10";

    @Test
    public void testFilter() {
        UnmarshallingFilter filter = UnmarshallingFilter.Factory.createFilter(SERIAL_FILTER_PATTERN);

        // class matching
        Assert.assertEquals(filter.checkInput(new TestFilterInput(ArrayList.class, 10, 10, 10, 10)),
                UnmarshallingFilter.FilterResponse.ACCEPT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(HashMap.class, 10, 10, 10, 10)),
                UnmarshallingFilter.FilterResponse.REJECT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Hashtable.class, 10, 10, 10, 10)),
                UnmarshallingFilter.FilterResponse.UNDECIDED);

        // undecided class, one of the limit checks rejecting
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Hashtable.class, 11, 10, 10, 10)),
                UnmarshallingFilter.FilterResponse.REJECT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Hashtable.class, 10, 11, 10, 10)),
                UnmarshallingFilter.FilterResponse.REJECT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Hashtable.class, 10, 10, 11, 10)),
                UnmarshallingFilter.FilterResponse.REJECT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Hashtable.class, 10, 10, 10, 11)),
                UnmarshallingFilter.FilterResponse.REJECT);

        // accepting filter before rejecting filters
        Assert.assertEquals(filter.checkInput(new TestFilterInput(ArrayList.class, 11, 11, 11, 11)),
                UnmarshallingFilter.FilterResponse.ACCEPT);
    }

    @Test
    public void testWildcard() {
        UnmarshallingFilter filter = UnmarshallingFilter.Factory.createFilter("java.util.*;!org.jboss.marshalling.*");

        // accepts java.util.*
        Assert.assertEquals(filter.checkInput(new TestFilterInput(ArrayList.class, -1, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.ACCEPT);
        // undecided on java.lang.Integer, not mentioned in the filter
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Integer.class, -1, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.UNDECIDED);
        // rejects org.jboss.marshalling.*
        Assert.assertEquals(filter.checkInput(new TestFilterInput(TestClass.class, -1, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.REJECT);
        // undecided on org.jboss.marshalling.cloner.DateFieldType, which is in nested package not mentioned in the filter
        Assert.assertEquals(filter.checkInput(new TestFilterInput(DateFieldType.class, -1, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.UNDECIDED);
    }

    @Test
    public void testNestingWildcard() {
        UnmarshallingFilter filter = UnmarshallingFilter.Factory.createFilter("!org.jboss.marshalling.**");

        // undecided on java.lang.Integer, not mentioned in the filter
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Integer.class, -1, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.UNDECIDED);
        // rejects everything under org.jboss.marshalling.**
        Assert.assertEquals(filter.checkInput(new TestFilterInput(TestClass.class, -1, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.REJECT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(DateFieldType.class, -1, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.REJECT);
    }

    @Test
    public void testNullClass() {
        UnmarshallingFilter filter = UnmarshallingFilter.Factory.createFilter("java.util.ArrayList;java.util.*;java.util.**");
        Assert.assertEquals(filter.checkInput(new TestFilterInput(null, 0, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.UNDECIDED);

        filter = UnmarshallingFilter.Factory.createFilter("*");
        Assert.assertEquals(filter.checkInput(new TestFilterInput(null, 0, 0, 0, 0)),
                UnmarshallingFilter.FilterResponse.ACCEPT);
    }

    @Test
    public void testInvalidFilters() {
        assertInvalid("org.jboss.marshalling.***");
        assertInvalid("modulename/org.jboss.marshalling.***");
        assertInvalid("maxarray=1=1");
    }

    private void assertInvalid(String spec) {
        try {
            UnmarshallingFilter.Factory.createFilter(spec);
            Assert.fail("Filter creation was expected to fail: " + spec);
        } catch (IllegalArgumentException expected) {}
    }

    static class TestFilterInput implements FilterInput {

        private final Class<?> unmarshalledClass;
        private final long arrayLength;
        private final long depth;
        private final long references;
        private final long streamBytes;

        public TestFilterInput(Class<?> unmarshalledClass, long arrayLength, long depth, long references, long streamBytes) {
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
