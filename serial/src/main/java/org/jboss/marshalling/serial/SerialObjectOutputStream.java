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

package org.jboss.marshalling.serial;

import org.jboss.marshalling.MarshallerObjectOutputStream;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.util.FieldPutter;
import org.jboss.marshalling.util.BooleanFieldPutter;
import org.jboss.marshalling.util.ByteFieldPutter;
import org.jboss.marshalling.util.CharFieldPutter;
import org.jboss.marshalling.util.DoubleFieldPutter;
import org.jboss.marshalling.util.FloatFieldPutter;
import org.jboss.marshalling.util.IntFieldPutter;
import org.jboss.marshalling.util.LongFieldPutter;
import org.jboss.marshalling.util.ObjectFieldPutter;
import org.jboss.marshalling.util.ShortFieldPutter;
import org.jboss.marshalling.util.Kind;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public final class SerialObjectOutputStream extends MarshallerObjectOutputStream {

    protected enum State {
        OFF,
        NEW,
        FIELDS,
        ON,
        ;
    }

    private final SerialMarshaller serialMarshaller;

    private State state = State.OFF;
    private Object currentObject;
    private SerializableClass currentSerializableClass;
    private Map<String, FieldPutter> currentFieldMap;

    protected SerialObjectOutputStream(final SerialMarshaller serialMarshaller, final BlockMarshaller blockMarshaller) throws IOException, SecurityException {
        super(blockMarshaller);
        this.serialMarshaller = serialMarshaller;
    }

    State saveState() {
        try {
            return state;
        } finally {
            state = State.NEW;
        }
    }

    State restoreState(final State state) {
        try {
            return this.state;
        } finally {
            this.state = state;
        }
    }

    Object saveCurrentObject(final Object currentObject) {
        try {
            return this.currentObject;
        } finally {
            this.currentObject = currentObject;
        }
    }

    void setCurrentObject(final Object currentObject) {
        this.currentObject = currentObject;
    }

    Map<String, FieldPutter> saveCurrentFieldMap() {
        return currentFieldMap;
    }

    void setCurrentFieldMap(Map<String, FieldPutter> map) {
        currentFieldMap = map;
    }

    SerializableClass saveCurrentSerializableClass(final SerializableClass currentSerializableClass) {
        try {
            return this.currentSerializableClass;
        } finally {
            this.currentSerializableClass = currentSerializableClass;
        }
    }

    void setCurrentSerializableClass(final SerializableClass currentSerializableClass) {
        this.currentSerializableClass = currentSerializableClass;
    }

    public void writeFields() throws IOException {
        if (state == State.FIELDS) {
            for (FieldPutter putter : currentFieldMap.values()) {
                if (putter.getKind() != Kind.OBJECT) {
                    putter.write(serialMarshaller);
                }
            }
            for (FieldPutter putter : currentFieldMap.values()) {
                if (putter.getKind() == Kind.OBJECT) {
                    putter.write(serialMarshaller);
                }
            }
        } else {
            throw new IllegalStateException("fields may not be written now");
        }
        state = State.ON;
    }

    public PutField putFields() throws IOException {
        if (state == State.NEW) {
            final Map<String, FieldPutter> map = new TreeMap<String, FieldPutter>();
            currentFieldMap = map;
            for (SerializableField serializableField : currentSerializableClass.getFields()) {
                final FieldPutter putter;
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        putter = new BooleanFieldPutter();
                        break;
                    }
                    case BYTE: {
                        putter = new ByteFieldPutter();
                        break;
                    }
                    case CHAR: {
                        putter = new CharFieldPutter();
                        break;
                    }
                    case DOUBLE: {
                        putter = new DoubleFieldPutter();
                        break;
                    }
                    case FLOAT: {
                        putter = new FloatFieldPutter();
                        break;
                    }
                    case INT: {
                        putter = new IntFieldPutter();
                        break;
                    }
                    case LONG: {
                        putter = new LongFieldPutter();
                        break;
                    }
                    case OBJECT: {
                        putter = new ObjectFieldPutter(serializableField.isUnshared());
                        break;
                    }
                    case SHORT: {
                        putter = new ShortFieldPutter();
                        break;
                    }
                    default: {
                        continue;
                    }
                }
                map.put(serializableField.getName(), putter);
            }
            state = State.FIELDS;
            return new PutField() {
                public void put(final String name, final boolean val) {
                    find(name).setBoolean(val);
                }

                public void put(final String name, final byte val) {
                    find(name).setByte(val);
                }

                public void put(final String name, final char val) {
                    find(name).setChar(val);
                }

                public void put(final String name, final short val) {
                    find(name).setShort(val);
                }

                public void put(final String name, final int val) {
                    find(name).setInt(val);
                }

                public void put(final String name, final long val) {
                    find(name).setLong(val);
                }

                public void put(final String name, final float val) {
                    find(name).setFloat(val);
                }

                public void put(final String name, final double val) {
                    find(name).setDouble(val);
                }

                public void put(final String name, final Object val) {
                    find(name).setObject(val);
                }

                public void write(final ObjectOutput out) throws IOException {
                    throw new UnsupportedOperationException("write(ObjectOutput)");
                }

                private FieldPutter find(final String name) {
                    final FieldPutter putter = map.get(name);
                    if (putter == null) {
                        throw new IllegalArgumentException("No field named '" + name + "' found");
                    }
                    return putter;
                }
            };
        } else {
            throw new IllegalStateException("putFields() may not be called now");
        }
    }

    public void defaultWriteObject() throws IOException {
        if (state == State.NEW || state == State.FIELDS) {
            serialMarshaller.doWriteFields(currentSerializableClass, currentObject);
        } else {
            throw new IllegalStateException("fields may not be written now");
        }
        state = State.ON;
    }
}
