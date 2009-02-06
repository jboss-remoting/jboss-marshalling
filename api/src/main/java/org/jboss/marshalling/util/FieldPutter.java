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
import org.jboss.marshalling.Marshaller;

/**
 *
 */
public abstract class FieldPutter {

    protected FieldPutter() {
    }

    public abstract void write(final Marshaller marshaller) throws IOException;

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
