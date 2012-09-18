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

import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.util.ReadField;
import org.jboss.marshalling.util.BooleanReadField;
import org.jboss.marshalling.util.ByteReadField;
import org.jboss.marshalling.util.CharReadField;
import org.jboss.marshalling.util.DoubleReadField;
import org.jboss.marshalling.util.FloatReadField;
import org.jboss.marshalling.util.IntReadField;
import org.jboss.marshalling.util.LongReadField;
import org.jboss.marshalling.util.ObjectReadField;
import org.jboss.marshalling.util.ShortReadField;
import org.jboss.marshalling.util.Kind;
import java.io.IOException;
import java.io.ObjectStreamConstants;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.util.Map;
import java.util.HashMap;

/**
 *
 */
class PlainDescriptor extends Descriptor implements ObjectStreamConstants {
    private final SerializableField[] fields;
    private final int flags;

    protected PlainDescriptor(final Class<?> type, final Descriptor parent, final SerializableField[] fields, final int flags) {
        super(parent, type);
        this.fields = fields;
        this.flags = flags;
    }

    public SerializableField[] getFields() {
        return fields;
    }

    public int getFlags() {
        return flags;
    }

    protected void readSerial(final SerialUnmarshaller serialUnmarshaller, final SerializableClass sc, final Object subject) throws IOException, ClassNotFoundException {
        if ((flags & SC_WRITE_METHOD) != 0) {
            if (sc.hasReadObject()) {
                doReadObject(serialUnmarshaller, sc, subject);
            } else {
                defaultReadFields(serialUnmarshaller, subject);
            }
            final BlockUnmarshaller blockUnmarshaller = serialUnmarshaller.getBlockUnmarshaller();
            blockUnmarshaller.readToEndBlockData();
            blockUnmarshaller.unblock();
        } else {
            if (sc.hasReadObject()) {
                final BlockUnmarshaller blockUnmarshaller = serialUnmarshaller.getBlockUnmarshaller();
                blockUnmarshaller.endOfStream();
                doReadObject(serialUnmarshaller, sc, subject);
                blockUnmarshaller.unblock();
            } else {
                defaultReadFields(serialUnmarshaller, subject);
            }
        }
    }

    private void doReadObject(final SerialUnmarshaller serialUnmarshaller, final SerializableClass sc, final Object subject) throws ClassNotFoundException, IOException {
        final SerialObjectInputStream ois = serialUnmarshaller.getObjectInputStream();
        final SerialObjectInputStream.State oldState = ois.saveState();
        final PlainDescriptor oldDescriptor = ois.saveCurrentDescriptor(this);
        final SerializableClass oldSerializableClass = ois.saveCurrentSerializableClass(sc);
        final Object oldSubject = ois.saveCurrentSubject(subject);
        try {
            sc.callReadObject(subject, ois);
            if (sc.getFields().length > 0 && ois.restoreState(oldState) != SerialObjectInputStream.State.ON) {
                throw new StreamCorruptedException("readObject() did not read fields");
            }
        } finally {
            ois.restoreState(oldState);
            ois.setCurrentDescriptor(oldDescriptor);
            ois.setCurrentSerializableClass(oldSerializableClass);
            ois.setCurrentSubject(oldSubject);
        }
    }

    void defaultReadFields(final SerialUnmarshaller serialUnmarshaller, final Object subject) throws IOException, ClassNotFoundException {
        try {
            // first primitive fields
            for (SerializableField serializableField : fields) {
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        serializableField.getField().setBoolean(subject, serialUnmarshaller.readBoolean());
                        break;
                    }
                    case BYTE: {
                        serializableField.getField().setByte(subject, serialUnmarshaller.readByte());
                        break;
                    }
                    case CHAR: {
                        serializableField.getField().setChar(subject, serialUnmarshaller.readChar());
                        break;
                    }
                    case DOUBLE: {
                        serializableField.getField().setDouble(subject, serialUnmarshaller.readDouble());
                        break;
                    }
                    case FLOAT: {
                        serializableField.getField().setFloat(subject, serialUnmarshaller.readFloat());
                        break;
                    }
                    case INT: {
                        serializableField.getField().setInt(subject, serialUnmarshaller.readInt());
                        break;
                    }
                    case LONG: {
                        serializableField.getField().setLong(subject, serialUnmarshaller.readLong());
                        break;
                    }
                    case SHORT: {
                        serializableField.getField().setShort(subject, serialUnmarshaller.readShort());
                        break;
                    }
                }
            }
            // next object fields
            for (SerializableField serializableField : fields) {
                if (serializableField.getKind() == Kind.OBJECT) {
                    serializableField.getField().set(subject, serialUnmarshaller.readObject());
                }
            }
        } catch (IllegalAccessException e) {
            final InvalidClassException ice = new InvalidClassException("Unexpected illegal access");
            ice.initCause(e);
            throw ice;
        }
    }

    ObjectInputStream.GetField getField(final SerialUnmarshaller serialUnmarshaller, final SerializableClass sc) throws IOException, ClassNotFoundException {
        final Map<String, ReadField> readFields = new HashMap<String, ReadField>();
        for (SerializableField serializableField : sc.getFields()) {
            final ReadField readField;
            switch (serializableField.getKind()) {
                case BOOLEAN: {
                    readField = new BooleanReadField(serializableField);
                    break;
                }
                case BYTE: {
                    readField = new ByteReadField(serializableField);
                    break;
                }
                case CHAR: {
                    readField = new CharReadField(serializableField);
                    break;
                }
                case DOUBLE: {
                    readField = new DoubleReadField(serializableField);
                    break;
                }
                case FLOAT: {
                    readField = new FloatReadField(serializableField);
                    break;
                }
                case INT: {
                    readField = new IntReadField(serializableField);
                    break;
                }
                case LONG: {
                    readField = new LongReadField(serializableField);
                    break;
                }
                case OBJECT: {
                    readField = new ObjectReadField(serializableField);
                    break;
                }
                case SHORT: {
                    readField = new ShortReadField(serializableField);
                    break;
                }
                default: {
                    continue;
                }
            }
            readFields.put(serializableField.getName(), readField);
        }
        // read primitive fields
        for (SerializableField serializableField : fields) {
            final ReadField readField;
            switch (serializableField.getKind()) {
                case BOOLEAN: {
                    readField = new BooleanReadField(serializableField, serialUnmarshaller.readBoolean());
                    break;
                }
                case BYTE: {
                    readField = new ByteReadField(serializableField, serialUnmarshaller.readByte());
                    break;
                }
                case CHAR: {
                    readField = new CharReadField(serializableField, serialUnmarshaller.readChar());
                    break;
                }
                case DOUBLE: {
                    readField = new DoubleReadField(serializableField, serialUnmarshaller.readDouble());
                    break;
                }
                case FLOAT: {
                    readField = new FloatReadField(serializableField, serialUnmarshaller.readFloat());
                    break;
                }
                case INT: {
                    readField = new IntReadField(serializableField, serialUnmarshaller.readInt());
                    break;
                }
                case LONG: {
                    readField = new LongReadField(serializableField, serialUnmarshaller.readLong());
                    break;
                }
                case SHORT: {
                    readField = new ShortReadField(serializableField, serialUnmarshaller.readShort());
                    break;
                }
                default: {
                    continue;
                }
            }
            readFields.put(serializableField.getName(), readField);
        }
        // read object fields
        for (SerializableField serializableField : fields) {
            final ReadField readField;
            switch (serializableField.getKind()) {
                case OBJECT: {
                    readField = new ObjectReadField(serializableField, serialUnmarshaller.readObject());
                    break;
                }
                default: {
                    continue;
                }
            }
            readFields.put(serializableField.getName(), readField);
        }
        return new ObjectInputStream.GetField() {
            public ObjectStreamClass getObjectStreamClass() {
                throw new UnsupportedOperationException("getObjectStreamClass()");
            }

            public boolean defaulted(final String name) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return true;
                return field.isDefaulted();
            }

            public boolean get(final String name, final boolean val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getBoolean();
            }

            public byte get(final String name, final byte val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getByte();
            }

            public char get(final String name, final char val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getChar();
            }

            public short get(final String name, final short val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getShort();
            }

            public int get(final String name, final int val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getInt();
            }

            public long get(final String name, final long val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getLong();
            }

            public float get(final String name, final float val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getFloat();
            }

            public double get(final String name, final double val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getDouble();
            }

            public Object get(final String name, final Object val) throws IOException {
                final ReadField field = readFields.get(name);
                if (field == null) return val;
                return field.isDefaulted() ? val : field.getObject();
            }
        };
    }
}
