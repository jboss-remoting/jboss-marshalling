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

import org.jboss.marshalling.MarshallerObjectInputStream;
import org.jboss.marshalling.reflect.SerializableField;
import java.io.IOException;
import java.io.ObjectInputValidation;
import java.io.NotActiveException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamClass;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class RiverObjectInputStream extends MarshallerObjectInputStream {
    private AtomicReference<State> state = new AtomicReference<State>(State.OFF);
    private final RiverUnmarshaller unmarshaller;
    
    protected RiverObjectInputStream(final RiverUnmarshaller unmarshaller) throws IOException, SecurityException {
        super(unmarshaller);
        this.unmarshaller = unmarshaller;
    }

    private SerializableClassDescriptor serializableClassDescriptor;
    private Object current;

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (! state.compareAndSet(State.UNREAD_FIELDS, State.ON)) {
            throw new NotActiveException("defaultReadObject() may only be called when the fields have not yet been read");
        }
        try {
            unmarshaller.readFields(current, serializableClassDescriptor);
        } finally {
            serializableClassDescriptor = null;
            current = null;
        }
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
        if (! state.compareAndSet(State.UNREAD_FIELDS, State.ON)) {
            throw new NotActiveException("readFields() may only be called when the fields have not yet been read");
        }
        final SerializableField[] streamFields = serializableClassDescriptor.getFields();
        final int cnt = streamFields.length;
        final ReadField[] readFields = new ReadField[cnt];
        // todo - find defaulted fields

        for (int i = 0; i < cnt; i++) {
            SerializableField field = streamFields[i];
            switch (field.getKind()) {
                case BOOLEAN: {
                    final boolean value = unmarshaller.readBoolean();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public boolean getBoolean() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case BYTE: {
                    final byte value = unmarshaller.readByte();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public byte getByte() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case CHAR: {
                    final char value = unmarshaller.readChar();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public char getChar() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case DOUBLE: {
                    final double value = unmarshaller.readDouble();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public double getDouble() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case FLOAT: {
                    final float value = unmarshaller.readFloat();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public float getFloat() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case INT: {
                    final int value = unmarshaller.readInt();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public int getInt() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case LONG: {
                    final long value = unmarshaller.readLong();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public long getLong() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case OBJECT: {
                    final Object value = unmarshaller.readObject();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public Object getObject() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                case SHORT: {
                    final short value = unmarshaller.readShort();
                    readFields[i] = new ReadField(field.getName(), false) {
                        public short getShort() throws IOException {
                            return value;
                        }
                    };
                    break;
                }
                default:
                    throw new IllegalStateException("Wrong field type");
            }
        }
        return new GetField() {
            public ObjectStreamClass getObjectStreamClass() {
                throw new UnsupportedOperationException("TODO...");
            }

            private ReadField find(final String name) {
                if (name == null) {
                    throw new NullPointerException("name is null");
                }
                for (ReadField field : readFields) {
                    if (name.equals(field.getName())) {
                        return field;
                    }
                }
                throw new IllegalArgumentException("No field named '" + name + "'");
            }

            public boolean defaulted(final String name) throws IOException {
                return find(name).isDefaulted();
            }

            public boolean get(final String name, final boolean val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getBoolean();
            }

            public byte get(final String name, final byte val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getByte();
            }

            public char get(final String name, final char val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getChar();
            }

            public short get(final String name, final short val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getShort();
            }

            public int get(final String name, final int val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getInt();
            }

            public long get(final String name, final long val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getLong();
            }

            public float get(final String name, final float val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getFloat();
            }

            public double get(final String name, final double val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getDouble();
            }

            public Object get(final String name, final Object val) throws IOException {
                final ReadField field = find(name);
                return field.isDefaulted() ? val : field.getObject();
            }
        };
    }

    public void registerValidation(final ObjectInputValidation obj, final int prio) throws NotActiveException, InvalidObjectException {
    }

    protected SerializableClassDescriptor swapClass(final SerializableClassDescriptor descriptor) {
        try {
            return serializableClassDescriptor;
        } finally {
            serializableClassDescriptor = descriptor;
        }
    }

    protected Object swapCurrent(final Object obj) {
        try {
            return current;
        } finally {
            current = obj;
        }
    }

    protected State start() {
        return state.getAndSet(State.UNREAD_FIELDS);
    }

    protected void finish(final State restoreState) throws IOException {
        switch (state.getAndSet(restoreState)) {
            case OFF:
                // ??
                break;
            case ON:
                // todo if blockmode, flush...
                break;
            case UNREAD_FIELDS:
                throw new NotActiveException("Fields were never read");
        }
    }

    protected void fullReset() {
        state.set(State.OFF);
        serializableClassDescriptor = null;
        current = null;
    }

    protected enum State {
        OFF,
        UNREAD_FIELDS,
        ON,
        ;
    }
}
