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

import java.io.IOException;
import java.io.ObjectStreamConstants;

import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.util.Kind;

/**
 *
 */
class UnknownDescriptor extends Descriptor implements ObjectStreamConstants {
    private final SerializableField[] fields;
    private final int flags;

    protected UnknownDescriptor(final Descriptor parent, final SerializableField[] fields, final int flags) {
        super(parent, null);
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
        discardFields(serialUnmarshaller);
        if ((flags & SC_WRITE_METHOD) != 0) {
            final BlockUnmarshaller blockUnmarshaller = serialUnmarshaller.getBlockUnmarshaller();
            blockUnmarshaller.readToEndBlockData();
            blockUnmarshaller.unblock();
        }
    }

    void discardFields(final SerialUnmarshaller serialUnmarshaller) throws IOException, ClassNotFoundException {
        // first primitive fields
        for (SerializableField serializableField : fields) {
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
        // next object fields
        for (SerializableField serializableField : fields) {
            if (serializableField.getKind() == Kind.OBJECT) {
                serialUnmarshaller.readObject();
            }
        }
    }
}
