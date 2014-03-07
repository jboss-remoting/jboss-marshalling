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

package org.jboss.marshalling.river;

import org.jboss.marshalling.MarshallerObjectInputStream;
import org.jboss.marshalling.TraceInformation;
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

/**
 *
 */
public class RiverObjectInputStream extends MarshallerObjectInputStream {
    private static final int OFF = 0;
    private static final int UNREAD_FIELDS = 1;
    private static final int UNREAD_FIELDS_EOB = 2;
    private static final int ON = 3;

    private final RiverUnmarshaller unmarshaller;
    private final BlockUnmarshaller blockUnmarshaller;

    private int state = OFF;

    protected RiverObjectInputStream(final RiverUnmarshaller riverUnmarshaller, final BlockUnmarshaller delegateUnmarshaller) throws IOException, SecurityException {
        super(delegateUnmarshaller);
        unmarshaller = riverUnmarshaller;
        blockUnmarshaller = delegateUnmarshaller;
    }

    private SerializableClassDescriptor serializableClassDescriptor;
    private Object current;
    private int restoreIdx;

    private int getAndSet(int set) {
        try {
            return state;
        } finally {
            state = set;
        }
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        int old = getAndSet(ON);
        switch (old) {
            case UNREAD_FIELDS:
            case UNREAD_FIELDS_EOB: break;
            default:
                throw new NotActiveException("readFields() may only be called when the fields have not yet been read");
        }
        try {
            unmarshaller.readFields(current, serializableClassDescriptor);
            if (old == UNREAD_FIELDS_EOB) {
                restoreIdx = blockUnmarshaller.tempEndOfStream();
            }
        } finally {
            serializableClassDescriptor = null;
            current = null;
        }
    }

    void discardReadObject() throws IOException {
        int old = getAndSet(ON);
        switch (old) {
            case UNREAD_FIELDS:
            case UNREAD_FIELDS_EOB: break;
            default:
                throw new NotActiveException("readFields() may only be called when the fields have not yet been read");
        }
        try {
            unmarshaller.discardFields(serializableClassDescriptor);
            if (old == UNREAD_FIELDS_EOB) {
                restoreIdx = blockUnmarshaller.tempEndOfStream();
            }
        } finally {
            serializableClassDescriptor = null;
            current = null;
        }
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
        int old = getAndSet(ON);
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
        if (old == UNREAD_FIELDS_EOB) {
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
                return null;
            }

            public boolean defaulted(final String name) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted();
            }

            public boolean get(final String name, final boolean val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getBoolean();
            }

            public byte get(final String name, final byte val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getByte();
            }

            public char get(final String name, final char val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getChar();
            }

            public short get(final String name, final short val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getShort();
            }

            public int get(final String name, final int val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getInt();
            }

            public long get(final String name, final long val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getLong();
            }

            public float get(final String name, final float val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getFloat();
            }

            public double get(final String name, final double val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getDouble();
            }

            public Object get(final String name, final Object val) throws IOException {
                final ReadField field = find(name);
                return field == null || field.isDefaulted() ? val : field.getObject();
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

    protected int start() {
        return getAndSet(UNREAD_FIELDS);
    }

    protected void finish(final int restoreState) throws IOException {
        switch (getAndSet(restoreState)) {
            case OFF:
                // ??
                break;
            case ON:
                // todo if blockmode, flush...
                break;
            case UNREAD_FIELDS:
            case UNREAD_FIELDS_EOB:
                unmarshaller.discardFields(serializableClassDescriptor);
                return;
            default:
                throw new IllegalStateException("Unknown state");
        }
    }

    private void checkState() throws IOException {
        switch (state) {
            case OFF:
                throw new NotActiveException("Object stream not active");
            case ON:
                return;
            case UNREAD_FIELDS:
            case UNREAD_FIELDS_EOB:
                discardReadObject();
                return;
            default:
                throw new IllegalStateException("Unknown state");
        }
    }

    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        checkState();
        return super.readObjectOverride();
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        checkState();
        return super.readUnshared();
    }

    public int read() throws IOException {
        checkState();
        return super.read();
    }

    public int read(final byte[] buf) throws IOException {
        checkState();
        return super.read(buf);
    }

    public int read(final byte[] buf, final int off, final int len) throws IOException {
        checkState();
        return super.read(buf, off, len);
    }

    public boolean readBoolean() throws IOException {
        checkState();
        return super.readBoolean();
    }

    public byte readByte() throws IOException {
        checkState();
        return super.readByte();
    }

    public int readUnsignedByte() throws IOException {
        checkState();
        return super.readUnsignedByte();
    }

    public char readChar() throws IOException {
        checkState();
        return super.readChar();
    }

    public short readShort() throws IOException {
        checkState();
        return super.readShort();
    }

    public int readUnsignedShort() throws IOException {
        checkState();
        return super.readUnsignedShort();
    }

    public int readInt() throws IOException {
        checkState();
        return super.readInt();
    }

    public long readLong() throws IOException {
        checkState();
        return super.readLong();
    }

    public float readFloat() throws IOException {
        checkState();
        return super.readFloat();
    }

    public double readDouble() throws IOException {
        checkState();
        return super.readDouble();
    }

    public void readFully(final byte[] buf) throws IOException {
        checkState();
        super.readFully(buf);
    }

    public void readFully(final byte[] buf, final int off, final int len) throws IOException {
        checkState();
        super.readFully(buf, off, len);
    }

    public int skipBytes(final int len) throws IOException {
        checkState();
        return super.skipBytes(len);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    public String readLine() throws IOException {
        checkState();
        return super.readLine();
    }

    public String readUTF() throws IOException {
        checkState();
        return super.readUTF();
    }

    public long skip(final long n) throws IOException {
        checkState();
        return super.skip(n);
    }

    protected void fullReset() {
        state = OFF;
        serializableClassDescriptor = null;
        current = null;
    }

    protected void noCustomData() {
        state = UNREAD_FIELDS_EOB;
    }

    protected int getRestoreIdx() {
        return restoreIdx;
    }
}
