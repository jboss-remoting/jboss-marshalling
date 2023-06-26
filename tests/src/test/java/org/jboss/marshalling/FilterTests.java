package org.jboss.marshalling;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class FilterTests {

    @Test
    public void jdkSpecific_setObjectInputStreamFilter() throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteBufferInput(createPayload()))) {
            AbstractUnmarshaller.setObjectInputStreamFilter(ois, UnmarshallingFilter.ACCEPTING);
            ois.readObject();
        } catch (InvalidClassException e) {
            Assert.fail("Deserialization was expected to succeed.");
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteBufferInput(createPayload()))) {
            AbstractUnmarshaller.setObjectInputStreamFilter(ois, UnmarshallingFilter.REJECTING);
            ois.readObject();
            Assert.fail("Deserialization was expected to fail.");
        } catch (InvalidClassException expected) {
        }
    }

    private static ByteBuffer createPayload() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1000);
        try (ObjectOutputStream oos = new ObjectOutputStream(new ByteBufferOutput(buffer))) {
            oos.writeObject(new Integer[] {1, 2, 3});
        }
        //noinspection RedundantCast
        ((ByteBuffer) buffer).flip(); // JDK 9+ to JDK 8 cross-compilation issue
        return buffer;
    }

}
