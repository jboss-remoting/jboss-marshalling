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

import org.jboss.marshalling.MarshallerObjectInputStream;
import org.jboss.marshalling.TraceInformation;
import org.jboss.marshalling.util.DefaultReadField;
import org.jboss.marshalling.util.ReadField;
import org.jboss.marshalling.util.ShortReadField;
import org.jboss.marshalling.util.ObjectReadField;
import org.jboss.marshalling.util.LongReadField;
import org.jboss.marshalling.util.IntReadField;
import org.jboss.marshalling.util.FloatReadField;
import org.jboss.marshalling.util.DoubleReadField;
import org.jboss.marshalling.util.CharReadField;
import org.jboss.marshalling.util.ByteReadField;
import org.jboss.marshalling.util.BooleanReadField;
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
    private final BlockUnmarshaller blockUnmarshaller;

    protected RiverObjectInputStream(final RiverUnmarshaller riverUnmarshaller, final BlockUnmarshaller delegateUnmarshaller) throws IOException, SecurityException {
        super(delegateUnmarshaller);
        unmarshaller = riverUnmarshaller;
        blockUnmarshaller = delegateUnmarshaller;
    }

    private SerializableClassDescriptor serializableClassDescriptor;
    private Object current;
    private int restoreIdx;

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        State old = state.getAndSet(State.ON);
        switch (old) {
            case UNREAD_FIELDS:
            case UNREAD_FIELDS_EOB: break;
            default:
                throw new NotActiveException("readFields() may only be called when the fields have not yet been read");
        }
        try {
            unmarshaller.readFields(current, serializableClassDescriptor);
            if (old == State.UNREAD_FIELDS_EOB) {
                restoreIdx = blockUnmarshaller.tempEndOfStream();
            }
        } finally {
            serializableClassDescriptor = null;
            current = null;
        }
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
        State old = state.getAndSet(State.ON);
        switch (old) {
            case UNREAD_FIELDS:
            case UNREAD_FIELDS_EOB: break;
            default:
                throw new NotActiveException("readFields() may only be called when the fields have not yet been read");
        }
        final SerializableField[] streamFields = serializableClassDescriptor.getFields();
        final int cnt = streamFields.length;
        final ReadField[] readFields = new ReadField[cnt];
        // todo - find defaulted fields

        for (int i = 0; i < cnt; i++) {
            SerializableField field = streamFields[i];
            try {
                switch (field.getKind()) {
                    case BOOLEAN: {
                        readFields[i] = new BooleanReadField(field, unmarshaller.readBoolean());
                        break;
                    }
                    case BYTE: {
                        readFields[i] = new ByteReadField(field, unmarshaller.readByte());
                        break;
                    }
                    case CHAR: {
                        readFields[i] = new CharReadField(field, unmarshaller.readChar());
                        break;
                    }
                    case DOUBLE: {
                        readFields[i] = new DoubleReadField(field, unmarshaller.readDouble());
                        break;
                    }
                    case FLOAT: {
                        readFields[i] = new FloatReadField(field, unmarshaller.readFloat());
                        break;
                    }
                    case INT: {
                        readFields[i] = new IntReadField(field, unmarshaller.readInt());
                        break;
                    }
                    case LONG: {
                        readFields[i] = new LongReadField(field, unmarshaller.readLong());
                        break;
                    }
                    case OBJECT: {
                        readFields[i] = new ObjectReadField(field, unmarshaller.readObject());
                        break;
                    }
                    case SHORT: {
                        readFields[i] = new ShortReadField(field, unmarshaller.readShort());
                        break;
                    }
                    default:
                        throw new IllegalStateException("Wrong field type");
                }
            } catch (IOException e) {
                TraceInformation.addFieldInformation(e, field.getName());
                throw e;
            } catch (ClassNotFoundException e) {
                TraceInformation.addFieldInformation(e, field.getName());
                throw e;
            } catch (RuntimeException e) {
                TraceInformation.addFieldInformation(e, field.getName());
                throw e;
            }
        }
        if (old == State.UNREAD_FIELDS_EOB) {
            restoreIdx = blockUnmarshaller.tempEndOfStream();
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
                return new DefaultReadField(name);
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
        unmarshaller.addValidation(obj, prio);
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
                //JBMAR-120
//                if(serializableClassDescriptor.getFields().length > 0) {
//                    throw new NotActiveException("Fields were never read");
//                }
        }
    }

    protected void fullReset() {
        state.set(State.OFF);
        serializableClassDescriptor = null;
        current = null;
    }

    protected void noCustomData() {
        state.set(State.UNREAD_FIELDS_EOB);
    }

    protected int getRestoreIdx() {
        return restoreIdx;
    }

    protected enum State {
        OFF,
        UNREAD_FIELDS,
        UNREAD_FIELDS_EOB,
        ON,
        ;
    }
}
