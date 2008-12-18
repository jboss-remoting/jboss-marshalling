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

package org.jboss.river;

import java.io.ObjectOutput;
import java.io.IOException;

/**
 *
 */
public class RiverObjectOutput implements ObjectOutput {
    private final RiverMarshaller marshaller;

    public RiverObjectOutput(final RiverMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    public void writeBoolean(final boolean v) throws IOException {
        marshaller.writeBoolean(v);
    }

    public void writeByte(final int v) throws IOException {
        marshaller.writeByte(v);
    }

    public void writeShort(final int v) throws IOException {
        marshaller.writeShort(v);
    }

    public void writeChar(final int v) throws IOException {
        marshaller.writeChar(v);
    }

    public void writeInt(final int v) throws IOException {
        marshaller.writeInt(v);
    }

    public void writeLong(final long v) throws IOException {
        marshaller.writeLong(v);
    }

    public void writeFloat(final float v) throws IOException {
        marshaller.writeFloat(v);
    }

    public void writeDouble(final double v) throws IOException {
        marshaller.writeDouble(v);
    }

    public void writeBytes(final String s) throws IOException {
        marshaller.writeBytes(s);
    }

    public void writeChars(final String s) throws IOException {
        marshaller.writeChars(s);
    }

    public void writeObject(final Object obj) throws IOException {
        marshaller.writeObject(obj);
    }

    public void flush() throws IOException {
        // ignore
    }

    public void close() throws IOException {
        // ignore
    }

    public void write(final int b) throws IOException {
        marshaller.write(b);
    }

    public void write(final byte[] b) throws IOException {
        marshaller.write(b);
    }

    public void write(final byte[] b, final int off, final int len) throws IOException {
        marshaller.write(b, off, len);
    }

    public void writeUTF(final String s) throws IOException {
        marshaller.writeUTF(s);
    }

    protected void start() {

    }

    protected void finish() {
        
    }
}
