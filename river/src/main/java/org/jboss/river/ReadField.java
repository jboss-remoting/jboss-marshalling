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

import java.io.IOException;

/**
 *
 */
public abstract class ReadField implements Comparable<ReadField> {
    private final String name;
    private final boolean defaulted;

    protected ReadField(final String name, final boolean defaulted) {
        this.name = name;
        this.defaulted = defaulted;
    }

    public String getName() {
        return name;
    }

    public boolean isDefaulted() {
        return defaulted;
    }

    public boolean getBoolean() throws IOException {
        throw wrongFieldType();
    }

    public char getChar() throws IOException {
        throw wrongFieldType();
    }

    public float getFloat() throws IOException {
        throw wrongFieldType();
    }

    public double getDouble() throws IOException {
        throw wrongFieldType();
    }

    public byte getByte() throws IOException {
        throw wrongFieldType();
    }

    public short getShort() throws IOException {
        throw wrongFieldType();
    }

    public int getInt() throws IOException {
        throw wrongFieldType();
    }

    public long getLong() throws IOException {
        throw wrongFieldType();
    }

    public Object getObject() throws IOException {
        throw wrongFieldType();
    }

    private IllegalArgumentException wrongFieldType() {
        return new IllegalArgumentException("Invalid field type");
    }

    public int compareTo(final ReadField o) {
        return name.compareTo(o.name);
    }
}
