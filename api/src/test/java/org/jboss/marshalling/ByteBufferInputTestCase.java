package org.jboss.marshalling;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test case for {@link ByteBufferInput}.
 * 
 * @author Ariel Kuechler
 */
public final class ByteBufferInputTestCase {

    /**
     * Test case to test {@link ByteBufferInput#read(byte[], int, int)} with offset in target array.
     * 
     * @throws IOException
     *             error during reading
     */
    @Test
    public final void testReadArrayWithOffset() throws IOException {
        final byte[] testBytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final ByteBuffer byteBuffer = ByteBuffer.wrap(testBytes);
        final ByteBufferInput byteBufferInput = new ByteBufferInput(byteBuffer);

        final byte[] result = new byte[4];
        int read;

        // read first four bytes without offset in target array
        read = byteBufferInput.read(result);
        Assert.assertEquals(4, read);
        Assert.assertEquals(new byte[] { 0, 1, 2, 3 }, result);

        // read next three bytes with 0 offset in target array
        read = byteBufferInput.read(result, 0, 3);
        Assert.assertEquals(3, read);
        Assert.assertEquals(new byte[] { 4, 5, 6, 3 }, result);

        // read next four bytes with an offset of two in target array
        read = byteBufferInput.read(result, 2, 2);
        Assert.assertEquals(2, read);
        Assert.assertEquals(new byte[] { 4, 5, 7, 8 }, result);

        byteBufferInput.close();
    }
}
