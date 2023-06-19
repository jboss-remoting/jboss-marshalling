package org.jboss.marshalling;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ObjectInputFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class UnmarshallingFilterTestCase {

    private static final String SERIAL_FILTER_PROP = "jdk.serialFilter";
    private static final String SERIAL_FILTER_PATTERN = "java.util.ArrayList;!java.util.HashMap;maxarray=10;maxdepth=10;maxrefs=10;maxbytes=10";

    @Test
    public void testDefaultFilterSystemProperty() {
        try {
            System.setProperty(SERIAL_FILTER_PROP, SERIAL_FILTER_PATTERN);
            UnmarshallingFilter defaultFilter = UnmarshallingFilter.Factory.createJEPS290DefaultFilter(false);
            assertions(defaultFilter);
        } finally {
            System.clearProperty(SERIAL_FILTER_PROP);
        }
    }

    /**
     * TODO: This only compiles on JDK 9+.
     * TODO: Process-wide filter can only be set once.
     */
    @Test
    public void testDefaultFilterAPI() {
        ObjectInputFilter.Config.setSerialFilter(ObjectInputFilter.Config.createFilter(SERIAL_FILTER_PATTERN));
        UnmarshallingFilter defaultFilter = UnmarshallingFilter.Factory.createJEPS290DefaultFilter(false);
        assertions(defaultFilter);
    }

    private void assertions(UnmarshallingFilter filter) {
        // class matching
        Assert.assertEquals(filter.checkInput(new TestFilterInput(ArrayList.class, 10, 10, 10, 10)),
                UnmarshallingFilter.FilterResponse.ACCEPT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(HashMap.class, 10, 10, 10, 10)),
                UnmarshallingFilter.FilterResponse.REJECT);
        Assert.assertEquals(filter.checkInput(new TestFilterInput(Hashtable.class, 10, 10, 10, 10)),
                UnmarshallingFilter.FilterResponse.UNDECIDED);

        // limit checks
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

    static class TestFilterInput implements UnmarshallingFilter.FilterInput {

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
}
