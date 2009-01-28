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

package org.jboss.marshalling.river;

import java.io.ObjectInput;
import java.io.IOException;

/**
 *
 */
public class RiverObjectInput implements ObjectInput {
    private final RiverUnmarshaller unmarshaller;

    public RiverObjectInput(final RiverUnmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public void close() throws IOException {
        // no operation
    }

    public int available() throws IOException {
        return unmarshaller.available();
    }

    public int read() throws IOException {
        return unmarshaller.read();
    }

    public int read(final byte[] b) throws IOException {
        return unmarshaller.read(b);
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        return unmarshaller.read(b, off, len);
    }

    public Object readObject() throws ClassNotFoundException, IOException {
        return unmarshaller.readObject();
    }

    public long skip(final long n) throws IOException {
        return unmarshaller.skip(n);
    }

    public boolean readBoolean() throws IOException {
        return unmarshaller.readBoolean();
    }

    public byte readByte() throws IOException {
        return unmarshaller.readByte();
    }

    public char readChar() throws IOException {
        return unmarshaller.readChar();
    }

    public double readDouble() throws IOException {
        return unmarshaller.readDouble();
    }

    public float readFloat() throws IOException {
        return unmarshaller.readFloat();
    }

    public void readFully(final byte[] b) throws IOException {
        unmarshaller.readFully(b);
    }

    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        unmarshaller.readFully(b, off, len);
    }

    public int readInt() throws IOException {
        return unmarshaller.readInt();
    }

    public String readLine() throws IOException {
        return unmarshaller.readLine();
    }

    public short readShort() throws IOException {
        return unmarshaller.readShort();
    }

    public int readUnsignedByte() throws IOException {
        return unmarshaller.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        return unmarshaller.readUnsignedShort();
    }

    public String readUTF() throws IOException {
        return unmarshaller.readUTF();
    }

    public long readLong() throws IOException {
        return unmarshaller.readLong();
    }

    public int skipBytes(final int n) throws IOException {
        return unmarshaller.skipBytes(n);
    }
}
