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
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class RiverObjectOutputStream extends MarshallerObjectOutputStream {
    protected enum State {
        OFF,
        UNWRITTEN_FIELDS,
        ON,
        ;
    }

    private final AtomicReference<State> state = new AtomicReference<State>(State.OFF);
    private final RiverMarshaller marshaller;
    private final Marshaller delegateMarshaller;

    private RiverPutField putField;
    private SerializableClass serializableClass;
    private Object current;

    protected RiverObjectOutputStream(final Marshaller delegateMarshaller, final RiverMarshaller marshaller) throws IOException, SecurityException {
        super(delegateMarshaller);
        this.marshaller = marshaller;
        this.delegateMarshaller = delegateMarshaller;
    }

    public void writeFields() throws IOException {
        final RiverPutField putField = this.putField;
        if (putField == null) {
            throw new NotActiveException("no current PutField object");
        }
        if (! state.compareAndSet(State.UNWRITTEN_FIELDS, State.ON)) {
            throw new NotActiveException("writeFields() may only be called when the fields have not yet been written");
        }
        this.putField = null;
        putField.write(marshaller);
    }

    public PutField putFields() throws IOException {
        if (state.get() == State.OFF) {
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
        if (! state.compareAndSet(State.UNWRITTEN_FIELDS, State.ON)) {
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

    protected State start() throws IOException {
        return state.getAndSet(State.UNWRITTEN_FIELDS);
    }

    protected void finish(State restoreState) throws IOException {
        switch (state.getAndSet(restoreState)) {
            case UNWRITTEN_FIELDS:
            	if(serializableClass.getFields().length > 0) {
            		throw new NotActiveException("Fields were never written");
            	}
        }
    }

    protected void fullReset() {
        state.set(State.OFF);
        putField = null;
        serializableClass = null;
        current = null;
    }

    private void checkState() throws NotActiveException {
        switch (state.get()) {
            case OFF:
                throw new NotActiveException("Object stream not active");
            case ON:
                return;
            case UNWRITTEN_FIELDS:
                throw new NotActiveException("Fields not yet written");
            default:
                throw new IllegalStateException("Unknown state");
        }
    }
}
