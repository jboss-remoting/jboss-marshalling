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
