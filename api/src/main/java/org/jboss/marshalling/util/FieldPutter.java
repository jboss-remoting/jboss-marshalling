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
import org.jboss.marshalling.Marshaller;

/**
 * Base class for a field "putter" which represents a field's data cell.
 */
public abstract class FieldPutter {

    /**
     * Construct a new instance.
     */
    protected FieldPutter() {
    }

    /**
     * Write the value of this field in its proper native format.
     *
     * @param marshaller the marshaller to which to write
     * @throws IOException if an error occurs
     */
    public abstract void write(final Marshaller marshaller) throws IOException;

    /**
     * Get the kind of field being written.
     *
     * @return the kind of field
     */
    public abstract Kind getKind();

    /**
     * Get the boolean value of this field.
     *
     * @return the boolean value of this field
     */
    public boolean getBoolean() {
        throw new IllegalArgumentException("Field is not a boolean field");
    }

    /**
     * Set the boolean value of this field.
     *
     * @param value the boolean value of this field
     */
    public void setBoolean(boolean value) {
        throw new IllegalArgumentException("Field is not a boolean field");
    }

    /**
     * Get the byte value of this field.
     *
     * @return the byte value of this field
     */
    public byte getByte() {
        throw new IllegalArgumentException("Field is not a byte field");
    }

    /**
     * Set the byte value of this field.
     *
     * @param value the byte value of this field
     */
    public void setByte(byte value) {
        throw new IllegalArgumentException("Field is not a byte field");
    }

    /**
     * Get the character value of this field.
     *
     * @return the character value of this field
     */
    public char getChar() {
        throw new IllegalArgumentException("Field is not a char field");
    }

    /**
     * Set the character value of this field.
     *
     * @param value the character value of this field
     */
    public void setChar(char value) {
        throw new IllegalArgumentException("Field is not a char field");
    }

    /**
     * Get the double value of this field.
     *
     * @return the double value of this field
     */
    public double getDouble() {
        throw new IllegalArgumentException("Field is not a double field");
    }

    /**
     * Set the double value of this field.
     *
     * @param value the double value of this field
     */
    public void setDouble(double value) {
        throw new IllegalArgumentException("Field is not a double field");
    }

    /**
     * Get the float value of this field.
     *
     * @return the float value of this field
     */
    public float getFloat() {
        throw new IllegalArgumentException("Field is not a float field");
    }

    /**
     * Set the float value of this field.
     *
     * @param value the float value of this field
     */
    public void setFloat(float value) {
        throw new IllegalArgumentException("Field is not a float field");
    }

    /**
     * Get the integer value of this field.
     *
     * @return the integer value of this field
     */
    public int getInt() {
        throw new IllegalArgumentException("Field is not an int field");
    }

    /**
     * Set the integer value of this field.
     *
     * @param value the integer value of this field
     */
    public void setInt(int value) {
        throw new IllegalArgumentException("Field is not an int field");
    }

    /**
     * Get the long value of this field.
     *
     * @return the long value of this field
     */
    public long getLong() {
        throw new IllegalArgumentException("Field is not a long field");
    }

    /**
     * Set the long value of this field.
     *
     * @param value the long value of this field
     */
    public void setLong(long value) {
        throw new IllegalArgumentException("Field is not a long field");
    }

    /**
     * Get the object value of this field.
     *
     * @return the object value of this field
     */
    public Object getObject() {
        throw new IllegalArgumentException("Field is not an Object field");
    }

    /**
     * Set the object value of this field.
     *
     * @param value the object value of this field
     */
    public void setObject(Object value) {
        throw new IllegalArgumentException("Field is not an Object field");
    }

    /**
     * Get the short value of this field.
     *
     * @return the short value of this field
     */
    public short getShort() {
        throw new IllegalArgumentException("Field is not a short field");
    }

    /**
     * Set the short value of this field.
     *
     * @param value the short value of this field
     */
    public void setShort(short value) {
        throw new IllegalArgumentException("Field is not a short field");
    }
}
