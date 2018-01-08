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
        // first primitive fields
        for (SerializableField serializableField : fields) {
            if (serializableField.isAccessible()) {
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        serializableField.setBoolean(subject, serialUnmarshaller.readBoolean());
                        break;
                    }
                    case BYTE: {
                        serializableField.setByte(subject, serialUnmarshaller.readByte());
                        break;
                    }
                    case CHAR: {
                        serializableField.setChar(subject, serialUnmarshaller.readChar());
                        break;
                    }
                    case DOUBLE: {
                        serializableField.setDouble(subject, serialUnmarshaller.readDouble());
                        break;
                    }
                    case FLOAT: {
                        serializableField.setFloat(subject, serialUnmarshaller.readFloat());
                        break;
                    }
                    case INT: {
                        serializableField.setInt(subject, serialUnmarshaller.readInt());
                        break;
                    }
                    case LONG: {
                        serializableField.setLong(subject, serialUnmarshaller.readLong());
                        break;
                    }
                    case SHORT: {
                        serializableField.setShort(subject, serialUnmarshaller.readShort());
                        break;
                    }
                }
            } else {
                // throw away - see JBMAR-185
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        serialUnmarshaller.readBoolean();
                        break;
                    }
                    case BYTE: {
                        serialUnmarshaller.readByte();
                        break;
                    }
                    case CHAR: {
                        serialUnmarshaller.readChar();
                        break;
                    }
                    case DOUBLE: {
                        serialUnmarshaller.readDouble();
                        break;
                    }
                    case FLOAT: {
                        serialUnmarshaller.readFloat();
                        break;
                    }
                    case INT: {
                        serialUnmarshaller.readInt();
                        break;
                    }
                    case LONG: {
                        serialUnmarshaller.readLong();
                        break;
                    }
                    case SHORT: {
                        serialUnmarshaller.readShort();
                        break;
                    }
                }
            }
        }
        // next object fields
        for (SerializableField serializableField : fields) {
            if (serializableField.getKind() == Kind.OBJECT) {
                if (serializableField.isAccessible()) {
                    serializableField.setObject(subject, serialUnmarshaller.readObject());
                } else {
                    // throw away - see JBMAR-185
                    serialUnmarshaller.readObject();
                }
            }
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
                return field == null || field.isDefaulted();
            }

            public boolean get(final String name, final boolean val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getBoolean();
            }

            public byte get(final String name, final byte val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getByte();
            }

            public char get(final String name, final char val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getChar();
            }

            public short get(final String name, final short val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getShort();
            }

            public int get(final String name, final int val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getInt();
            }

            public long get(final String name, final long val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getLong();
            }

            public float get(final String name, final float val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getFloat();
            }

            public double get(final String name, final double val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getDouble();
            }

            public Object get(final String name, final Object val) throws IOException {
                final ReadField field = readFields.get(name);
                return field == null || field.isDefaulted() ? val : field.getObject();
            }
        };
    }
}
