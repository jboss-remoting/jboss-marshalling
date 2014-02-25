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

import org.jboss.marshalling.MarshallerObjectOutputStream;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.util.BooleanFieldPutter;
import org.jboss.marshalling.util.ByteFieldPutter;
import org.jboss.marshalling.util.CharFieldPutter;
import org.jboss.marshalling.util.DoubleFieldPutter;
import org.jboss.marshalling.util.FloatFieldPutter;
import org.jboss.marshalling.util.IntFieldPutter;
import org.jboss.marshalling.util.LongFieldPutter;
import org.jboss.marshalling.util.ShortFieldPutter;
import org.jboss.marshalling.util.ObjectFieldPutter;
import org.jboss.marshalling.util.FieldPutter;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.reflect.SerializableClass;
import java.io.IOException;
import java.io.NotActiveException;

/**
 *
 */
public class RiverObjectOutputStream extends MarshallerObjectOutputStream {
    private static final int OFF = 0;
    private static final int UNWRITTEN_FIELDS = 1;
    private static final int ON = 2;

    private final RiverMarshaller marshaller;

    private int state;
    private RiverPutField putField;
    private SerializableClass serializableClass;
    private Object current;

    protected RiverObjectOutputStream(final Marshaller delegateMarshaller, final RiverMarshaller marshaller) throws IOException, SecurityException {
        super(delegateMarshaller);
        this.marshaller = marshaller;
    }

    private boolean compareAndSetState(int expect, int set) {
        if (state == expect) {
            state = set;
            return true;
        }
        return false;
    }

    private int getAndSetState(int set) {
        try {
            return state;
        } finally {
            state = set;
        }
    }

    public void writeFields() throws IOException {
        final RiverPutField putField = this.putField;
        if (putField == null) {
            throw new NotActiveException("no current PutField object");
        }
        if (! compareAndSetState(UNWRITTEN_FIELDS, ON)) {
            throw new NotActiveException("writeFields() may only be called when the fields have not yet been written");
        }
        this.putField = null;
        putField.write(marshaller);
    }

    public PutField putFields() throws IOException {
        if (state == OFF) {
            throw new NotActiveException("No object is currently being serialized");
        }
        if (putField == null) {
            final SerializableField[] serializableFields = serializableClass.getFields();
            final FieldPutter[] fields;
            final String[] names;
            final int cnt = serializableFields.length;
            fields = new FieldPutter[cnt];
            names = new String[cnt];
            for (int i = 0; i < cnt; i ++) {
                final SerializableField field = serializableFields[i];
                names[i] = field.getName();
                switch (field.getKind()) {
                    case BOOLEAN: {
                        fields[i] = new BooleanFieldPutter();
                        break;
                    }
                    case BYTE: {
                        fields[i] = new ByteFieldPutter();
                        break;
                    }
                    case CHAR: {
                        fields[i] = new CharFieldPutter();
                        break;
                    }
                    case DOUBLE: {
                        fields[i] = new DoubleFieldPutter();
                        break;
                    }
                    case FLOAT: {
                        fields[i] = new FloatFieldPutter();
                        break;
                    }
                    case INT: {
                        fields[i] = new IntFieldPutter();
                        break;
                    }
                    case LONG: {
                        fields[i] = new LongFieldPutter();
                        break;
                    }
                    case OBJECT: {
                        fields[i] = new ObjectFieldPutter(field.isUnshared());
                        break;
                    }
                    case SHORT: {
                        fields[i] = new ShortFieldPutter();
                        break;
                    }
                }
            }
            putField = new RiverPutField(fields, names);
        }
        return putField;
    }

    protected SerializableClass swapClass(SerializableClass newSerializableClass) {
        try {
            return serializableClass;
        } finally {
            serializableClass = newSerializableClass;
        }
    }

    protected Object swapCurrent(Object current) {
        try {
            return this.current;
        } finally {
            this.current = current;
        }
    }

    public void defaultWriteObject() throws IOException {
        if (! compareAndSetState(UNWRITTEN_FIELDS, ON)) {
            throw new NotActiveException("writeFields() may only be called when the fields have not yet been written");
        }
        try {
            marshaller.doWriteFields(serializableClass, current);
        } finally {
            putField = null;
            serializableClass = null;
            current = null;
        }
    }

    protected int start() throws IOException {
        return getAndSetState(UNWRITTEN_FIELDS);
    }

    protected void writeObjectOverride(final Object obj) throws IOException {
        checkState();
        super.writeObjectOverride(obj);
    }

    public void writeUnshared(final Object obj) throws IOException {
        checkState();
        super.writeUnshared(obj);
    }

    public void write(final int val) throws IOException {
        checkState();
        super.write(val);
    }

    public void write(final byte[] buf) throws IOException {
        checkState();
        super.write(buf);
    }

    public void write(final byte[] buf, final int off, final int len) throws IOException {
        checkState();
        super.write(buf, off, len);
    }

    public void writeBoolean(final boolean val) throws IOException {
        checkState();
        super.writeBoolean(val);
    }

    public void writeByte(final int val) throws IOException {
        checkState();
        super.writeByte(val);
    }

    public void writeShort(final int val) throws IOException {
        checkState();
        super.writeShort(val);
    }

    public void writeChar(final int val) throws IOException {
        checkState();
        super.writeChar(val);
    }

    public void writeInt(final int val) throws IOException {
        checkState();
        super.writeInt(val);
    }

    public void writeLong(final long val) throws IOException {
        checkState();
        super.writeLong(val);
    }

    public void writeFloat(final float val) throws IOException {
        checkState();
        super.writeFloat(val);
    }

    public void writeDouble(final double val) throws IOException {
        checkState();
        super.writeDouble(val);
    }

    public void writeBytes(final String str) throws IOException {
        checkState();
        super.writeBytes(str);
    }

    public void writeChars(final String str) throws IOException {
        checkState();
        super.writeChars(str);
    }

    public void writeUTF(final String str) throws IOException {
        checkState();
        super.writeUTF(str);
    }

    protected void finish(int restoreState) throws IOException {
        try {
            if (state == UNWRITTEN_FIELDS) {
                marshaller.doWriteEmptyFields(serializableClass);
            }
        } finally {
            state = restoreState;
        }
    }

    private void checkState() throws IOException {
        int state = this.state;
        if (state == ON) {
            return;
        } else if (state == OFF) {
            throw new NotActiveException("Object stream not active");
        } else if (state == UNWRITTEN_FIELDS) {
            this.state = ON;
            marshaller.doWriteEmptyFields(serializableClass);
            return;
        } else {
            throw new IllegalStateException("Unknown state");
        }
    }

    protected void fullReset() {
        state = OFF;
        putField = null;
        serializableClass = null;
        current = null;
    }
}
