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

package org.jboss.marshalling.util;

import java.io.IOException;

/**
 * Base class for a field which was read from the data stream.
 */
public abstract class ReadField implements Comparable<ReadField> {
    private final String name;
    private final boolean defaulted;

    /**
     * Construct a new instance.
     *
     * @param name the field name
     * @param defaulted {@code true} if the field's value was defaulted, {@code false} otherwise
     */
    protected ReadField(final String name, final boolean defaulted) {
        this.name = name;
        this.defaulted = defaulted;
    }

    /**
     * Get the kind of field represented by this object.
     *
     * @return the kind of field represented by this object
     */
    public abstract Kind getKind();

    /**
     * Get the field name.
     *
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * Determine whether this field was defaulted.
     *
     * @return {@code true} if this field value was defaulted, {@code false} otherwise
     */
    public boolean isDefaulted() {
        return defaulted;
    }

    /**
     * Get the boolean value of this field.
     *
     * @return the boolean value of this field
     * @throws IOException if the value cannot be read
     */
    public boolean getBoolean() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the character value of this field.
     *
     * @return the character value of this field
     * @throws IOException if the value cannot be read
     */
    public char getChar() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the float value of this field.
     *
     * @return the float value of this field
     * @throws IOException if the value cannot be read
     */
    public float getFloat() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the double value of this field.
     *
     * @return the double value of this field
     * @throws IOException if the value cannot be read
     */
    public double getDouble() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the byte value of this field.
     *
     * @return the byte value of this field
     * @throws IOException if the value cannot be read
     */
    public byte getByte() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the short value of this field.
     *
     * @return the short value of this field
     * @throws IOException if the value cannot be read
     */
    public short getShort() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the integer value of this field.
     *
     * @return the integer value of this field
     * @throws IOException if the value cannot be read
     */
    public int getInt() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the long value of this field.
     *
     * @return the long value of this field
     * @throws IOException if the value cannot be read
     */
    public long getLong() throws IOException {
        throw wrongFieldType();
    }

    /**
     * Get the object value of this field.
     *
     * @return the object value of this field
     * @throws IOException if the value cannot be read
     */
    public Object getObject() throws IOException {
        throw wrongFieldType();
    }

    private static IllegalArgumentException wrongFieldType() {
        return new IllegalArgumentException("Invalid field type");
    }

    /**
     * Compare this field with another on the basis of its name.
     *
     * @param o the other field object
     * @return the sort result based on the name of this field and the given field
     */
    public int compareTo(final ReadField o) {
        return name.compareTo(o.name);
    }
}
