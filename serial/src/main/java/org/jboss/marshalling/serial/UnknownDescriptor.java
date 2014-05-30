/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
