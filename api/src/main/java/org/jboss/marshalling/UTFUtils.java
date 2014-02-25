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

package org.jboss.marshalling;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.io.EOFException;

/**
 * Handy utility methods for dealing with strings in the modified UTF-8 format.
 * @apiviz.exclude
 */
public final class UTFUtils {
    private static final String INVALID_BYTE = "Invalid byte";
    private static final String MALFORMED = "Malformed UTF-8 sequence";

    private UTFUtils() {
    }

    private static final int UTF_BUFS_CHAR_CNT = 256;
    private static final int UTF_BUFS_BYTE_CNT = UTF_BUFS_CHAR_CNT * 3;

    private static final class BytesHolder extends ThreadLocal<byte[]> {
        protected byte[] initialValue() {
            return new byte[UTF_BUFS_BYTE_CNT];
        }
    }

    private static final BytesHolder BYTES_HOLDER = new BytesHolder();

    /**
     * Get the number of bytes used by the modified UTF-8 encoded form of the given string.  If the length is
     * greater than {@code 65536}, an exception is thrown.
     *
     * @param s the string
     * @return the length
     * @throws UTFDataFormatException if the string is longer than {@code 65536} characters
     * @see java.io.DataInput#readUTF()
     */
    public static int getShortUTFLength(final String s) throws UTFDataFormatException {
        final int length = s.length();
        int l = 0;
        for (int i = 0; i < length; i ++) {
            final char c = s.charAt(i);
            if (c > 0 && c <= 0x7f) {
                l ++;
            } else if (c <= 0x07ff) {
                l += 2;
            } else {
                l += 3;
            }
            if (l > 65535) {
                throw new UTFDataFormatException("String is too long for writeUTF");
            }
        }
        return l;
    }

    /**
     * Get the number of bytes used by the modified UTF-8 encoded form of the given string.
     *
     * @param s the string
     * @return the length
     * @see java.io.DataInput#readUTF()
     */
    public static long getLongUTFLength(final String s) {
        final int length = s.length();
        long l = 0;
        for (int i = 0; i < length; i ++) {
            final char c = s.charAt(i);
            if (c > 0 && c <= 0x7f) {
                l ++;
            } else if (c <= 0x07ff) {
                l += 2L;
            } else {
                l += 3L;
            }
        }
        return l;
    }

    /**
     * Write the modified UTF-8 form of the given string to the given output.
     *
     * @param output the output to write to
     * @param s the string
     * @throws IOException if an I/O error occurs
     * @see java.io.DataOutput#writeUTF(String)
     */
    public static void writeUTFBytes(final ByteOutput output, final String s) throws IOException {
        final byte[] byteBuf = BYTES_HOLDER.get();

        final int length = s.length();

        int strIdx = 0;
        int byteIdx = 0;
        while (strIdx < length) {
            final char c = s.charAt(strIdx ++);
            if (c > 0 && c <= 0x7f) {
                byteBuf[byteIdx ++] = (byte) c;
            } else if (c <= 0x07ff) {
                byteBuf[byteIdx ++] = (byte)(0xc0 | 0x1f & c >> 6);
                byteBuf[byteIdx ++] = (byte)(0x80 | 0x3f & c);
            } else {
                byteBuf[byteIdx ++] = (byte)(0xe0 | 0x0f & c >> 12);
                byteBuf[byteIdx ++] = (byte)(0x80 | 0x3f & c >> 6);
                byteBuf[byteIdx ++] = (byte)(0x80 | 0x3f & c);
            }
            if (byteIdx > UTF_BUFS_BYTE_CNT - 4) {
                output.write(byteBuf, 0, byteIdx);
                byteIdx = 0;
            }
        }
        if (byteIdx > 0) {
            output.write(byteBuf, 0, byteIdx);
        }
    }

    /**
     * Read the given number of characters from the given byte input.  The length given is in characters,
     * <b>NOT</b> in bytes.
     *
     * @param input the byte source
     * @param len the number of characters to read
     * @return the string
     * @throws IOException if an I/O error occurs
     * @see java.io.DataInput#readUTF()
     */
    public static String readUTFBytes(final ByteInput input, final int len) throws IOException {
        final byte[] byteBuf = BYTES_HOLDER.get();
        final char[] chars = new char[len];
        int i = 0, cnt = 0, charIdx = 0;
        while (charIdx < len) {
            if (i == cnt) {
                cnt = input.read(byteBuf, 0, Math.min(UTF_BUFS_BYTE_CNT, len - charIdx));
                if (cnt < 0) {
                    throw new EOFException();
                }
                i = 0;
            }
            final int a = byteBuf[i++] & 0xff;
            if (a < 0x80) {
                // low bit clear
                chars[charIdx ++] = (char) a;
            } else if (a < 0xc0) {
                throw new UTFDataFormatException(INVALID_BYTE);
            } else if (a < 0xe0) {
                if (i == cnt) {
                    cnt = input.read(byteBuf, 0, Math.min(UTF_BUFS_BYTE_CNT, len - charIdx));
                    if (cnt < 0) {
                        throw new EOFException();
                    }
                    i = 0;
                }
                final int b = byteBuf[i ++] & 0xff;
                if ((b & 0xc0) != 0x80) {
                    throw new UTFDataFormatException(INVALID_BYTE);
                }
                chars[charIdx ++] = (char) ((a & 0x1f) << 6 | b & 0x3f);
            } else if (a < 0xf0) {
                if (i == cnt) {
                    cnt = input.read(byteBuf, 0, Math.min(UTF_BUFS_BYTE_CNT, len - charIdx));
                    if (cnt < 0) {
                        throw new EOFException();
                    }
                    i = 0;
                }
                final int b = byteBuf[i ++] & 0xff;
                if ((b & 0xc0) != 0x80) {
                    throw new UTFDataFormatException(INVALID_BYTE);
                }
                if (i == cnt) {
                    cnt = input.read(byteBuf, 0, Math.min(UTF_BUFS_BYTE_CNT, len - charIdx));
                    if (cnt < 0) {
                        throw new EOFException();
                    }
                    i = 0;
                }
                final int c = byteBuf[i ++] & 0xff;
                if ((c & 0xc0) != 0x80) {
                    throw new UTFDataFormatException(INVALID_BYTE);
                }
                chars[charIdx ++] = (char) ((a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f);
            } else {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
        }
        return String.valueOf(chars);
    }

    /**
     * Read the given number of characters from the given byte input.  The length given is in bytes.
     *
     * @param input the byte source
     * @param len the number of bytes to read
     * @return the string
     * @throws IOException if an I/O error occurs
     * @see java.io.DataInput#readUTF()
     */
    public static String readUTFBytesByByteCount(final ByteInput input, final long len) throws IOException {
        final StringBuilder builder = new StringBuilder();
        for (long i = 0; i < len; i ++) {
            final int a = input.read();
            if (a < 0) {
                throw new EOFException("Expected " + (len - i) + " more bytes");
            } else if (a == 0) {
                builder.append('\0');
            } else if (a < 0x80) {
                builder.append((char) a);
            } else if (a < 0xc0) {
                throw new UTFDataFormatException(INVALID_BYTE);
            } else if (a < 0xe0) {
                if (++i < len) {
                    final int b = input.read();
                    if (b == -1) {
                        throw new EOFException("Expected " + (len - i) + " more bytes");
                    } else if ((b & 0xc0) != 0x80) {
                        throw new UTFDataFormatException(INVALID_BYTE);
                    }
                    builder.append((char) ((a & 0x1f) << 6 | b & 0x3f));
                } else {
                    throw new UTFDataFormatException(MALFORMED);
                }
            } else if (a < 0xf0) {
                if (++i < len) {
                    final int b = input.read();
                    if (b == -1) {
                        throw new EOFException("Expected " + (len - i) + " more bytes");
                    } else if ((b & 0xc0) != 0x80) {
                        throw new UTFDataFormatException(INVALID_BYTE);
                    }
                    if (++i < len) {
                        final int c1 = input.read();
                        if (c1 == -1) {
                            throw new EOFException("Expected " + (len - i) + " more bytes");
                        } else if ((c1 & 0xc0) != 0x80) {
                            throw new UTFDataFormatException(INVALID_BYTE);
                        }
                        builder.append((char) ((a & 0x0f) << 12 | (b & 0x3f) << 6 | c1 & 0x3f));
                    } else {
                        throw new UTFDataFormatException(MALFORMED);
                    }
                } else {
                    throw new UTFDataFormatException(MALFORMED);
                }
            } else {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
        }
        return builder.toString();
    }

    /**
     * Read a null-terminated modified UTF-8 string from the given byte input.  Bytes are read until a 0 is found or
     * until the end of the stream, whichever comes first.
     *
     * @param input the input
     * @return the string
     * @throws IOException if an I/O error occurs
     * @see java.io.DataInput#readUTF()
     */
    public static String readUTFZBytes(final ByteInput input) throws IOException {
        final StringBuilder builder = new StringBuilder();
        for (;;) {
            final int c = readUTFChar(input);
            if (c == -1) {
                return builder.toString();
            }
            builder.append((char) c);
        }
    }

    private static int readUTFChar(final ByteInput input) throws IOException {
        final int a = input.read();
        if (a < 0) {
            throw new EOFException();
        } else if (a == 0) {
            return -1;
        } else if (a < 0x80) {
            return (char)a;
        } else if (a < 0xc0) {
            throw new UTFDataFormatException(INVALID_BYTE);
        } else if (a < 0xe0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
            return (a & 0x1f) << 6 | b & 0x3f;
        } else if (a < 0xf0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
            final int c = input.read();
            if (c == -1) {
                throw new EOFException();
            } else if ((c & 0xc0) != 0x80) {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
            return (a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f;
        } else {
            throw new UTFDataFormatException(INVALID_BYTE);
        }
    }
}
