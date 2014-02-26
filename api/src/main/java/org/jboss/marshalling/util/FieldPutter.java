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
 *
 */
public abstract class FieldPutter {

    protected FieldPutter() {
    }

    public abstract void write(final Marshaller marshaller) throws IOException;

    public abstract Kind getKind();

    public void setBoolean(boolean value) {
        throw new IllegalArgumentException("Field is not a boolean field");
    }

    public boolean getBoolean() {
        throw new IllegalArgumentException("Field is not a boolean field");
    }

    public byte getByte() {
        throw new IllegalArgumentException("Field is not a byte field");
    }

    public void setByte(byte value) {
        throw new IllegalArgumentException("Field is not a byte field");
    }

    public char getChar() {
        throw new IllegalArgumentException("Field is not a char field");
    }

    public void setChar(char value) {
        throw new IllegalArgumentException("Field is not a char field");
    }

    public double getDouble() {
        throw new IllegalArgumentException("Field is not a double field");
    }

    public void setDouble(double value) {
        throw new IllegalArgumentException("Field is not a double field");
    }

    public float getFloat() {
        throw new IllegalArgumentException("Field is not a float field");
    }

    public void setFloat(float value) {
        throw new IllegalArgumentException("Field is not a float field");
    }

    public int getInt() {
        throw new IllegalArgumentException("Field is not an int field");
    }

    public void setInt(int value) {
        throw new IllegalArgumentException("Field is not an int field");
    }

    public long getLong() {
        throw new IllegalArgumentException("Field is not a long field");
    }

    public void setLong(long value) {
        throw new IllegalArgumentException("Field is not a long field");
    }

    public Object getObject() {
        throw new IllegalArgumentException("Field is not an Object field");
    }

    public void setObject(Object value) {
        throw new IllegalArgumentException("Field is not an Object field");
    }

    public short getShort() {
        throw new IllegalArgumentException("Field is not a short field");
    }

    public void setShort(short value) {
        throw new IllegalArgumentException("Field is not a short field");
    }
}
