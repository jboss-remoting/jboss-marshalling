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

import java.io.InputStream;
import java.io.Writer;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.Marshalling;

/**
 *
 */
public final class Serial implements ExtendedObjectStreamConstants {
    private Serial() {}

    private enum FieldType {
        BOOLEAN(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<boolean> " + Boolean.toString(dis.readBoolean());
            }
        },
        BYTE(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<byte> " + Byte.toString(dis.readByte());
            }
        },
        CHAR(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<char> " + Character.toString(dis.readChar());
            }
        },
        DOUBLE(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<double> " + Double.toString(dis.readDouble());
            }
        },
        FLOAT(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<float> " + Float.toString(dis.readFloat());
            }
        },
        INTEGER(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<int> " + Integer.toString(dis.readInt());
            }
        },
        LONG(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<long> " + Long.toString(dis.readLong());
            }
        },
        SHORT(false) {
            public Object getValueString(final DataInputStream dis) throws IOException {
                return "<short> " + Short.toString(dis.readShort());
            }
        },
        OBJECT(true) {
            public Object getValueString(final DataInputStream dis) {
                return "<object>";
            }
        }
        ;
        private final boolean obj;

        FieldType(final boolean obj) {
            this.obj = obj;
        }

        public abstract Object getValueString(final DataInputStream dis) throws IOException;

        public boolean isObject() {
            return obj;
        }

        public static FieldType fromTypeCode(int typeCode) {
            switch (typeCode) {
                case 'B': return FieldType.BYTE;
                case 'C': return FieldType.CHAR;
                case 'D': return FieldType.DOUBLE;
                case 'F': return FieldType.FLOAT;
                case 'I': return FieldType.INTEGER;
                case 'J': return FieldType.LONG;
                case 'S': return FieldType.SHORT;
                case 'Z': return FieldType.BOOLEAN;
                default: return FieldType.OBJECT;
            }
        }
    }

    private static final class FieldInfo {
        private FieldType type;
        private String name;
    }

    private static final class ClassInfo {
        private List<FieldInfo> info;
        private int flags;
        private long svu;
        private String name;
        private ClassInfo parent;
    }

    public static void dumpStream(InputStream serializedData, Writer destination) throws IOException {
        final DataInputStream dis = serializedData instanceof DataInputStream ? (DataInputStream) serializedData : new DataInputStream(serializedData);
        final BufferedWriter bw = destination instanceof BufferedWriter ? (BufferedWriter) destination : new BufferedWriter(destination);
        try {
            dumpStream(dis, bw);
        } finally {
            bw.flush();
            destination.flush();
        }
    }

    private static void dumpStream(DataInputStream serializedData, BufferedWriter destination) throws IOException {
        final AtomicInteger seq = new AtomicInteger(baseWireHandle);
        final Map<Integer, ClassInfo> descrs = new HashMap<Integer, ClassInfo>();
        if (serializedData.readShort() != STREAM_MAGIC) {
            printf(destination, 0, "Stream magic INVALID");
            return;
        } else {
            printf(destination, 0, "[%04x] Stream magic", Integer.valueOf(STREAM_MAGIC & 0xffff));
        }
        destination.write(String.format("[%04x] Stream version\n", Integer.valueOf(serializedData.readUnsignedShort())));
        // read contents
        for (;;) {
            int i = serializedData.read();
            if (i == -1) {
                destination.write("--- End of stream ---\n");
                return;
            } else {
                dumpContent(descrs, seq, serializedData, destination, 0, i);
            }
        }
    }

    private static void dumpContent(Map<Integer, ClassInfo> descrMap, final AtomicInteger seq, final DataInputStream dis, final BufferedWriter w, final int depth, final int leadByte) throws IOException {
        switch (leadByte) {
            case TC_BLOCKDATA: {
                final int len = dis.readUnsignedByte();
                printf(w, depth, "[%02x] TC_BLOCKDATA - Data block of %d bytes", Integer.valueOf(TC_BLOCKDATA), Integer.valueOf(len));
                for (int i = 0; i < len; i += dis.skipBytes(len - i));
                return;
            }
            case TC_BLOCKDATALONG: {
                final int len = dis.readInt();
                printf(w, depth, "[%02x] TC_BLOCKDATA - Data block of %d bytes", Integer.valueOf(TC_BLOCKDATA), Integer.valueOf(len));
                for (int i = 0; i < len; i += dis.skipBytes(len - i));
                return;
            }
            default: {
                dumpObject(descrMap, seq, dis, w, depth, leadByte);
                return;
            }
        }
    }

    private static void dumpObject(Map<Integer, ClassInfo> descrMap, final AtomicInteger seq, final DataInputStream dis, final BufferedWriter w, final int depth, final int leadByte) throws IOException {
        switch (leadByte) {
            // newObject
            case TC_OBJECT: {
                printf(w, depth, "[%02x] TC_OBJECT - New object", Integer.valueOf(TC_OBJECT));
                final ClassInfo classInfo = dumpDescriptor(descrMap, seq, dis, w, depth + 1, dis.readUnsignedByte());
                final int handle = seq.getAndIncrement();
                printf(w, depth + 1, "[--] New object handle is [%08x]", Integer.valueOf(handle));
                dumpFields(classInfo, descrMap, seq, dis, w, depth + 1);
                return;
            }
            case TC_OBJECTTABLE: {
                final int handle = seq.getAndIncrement();
                printf(w, depth, "[%02x] TC_OBJECTTABLE - New object from table, handle is [%08x]", Integer.valueOf(TC_OBJECTTABLE), Integer.valueOf(handle));
                dumpBlockData(descrMap, seq, dis, w, depth + 1);
            }
            // newClass
            case TC_CLASS: {
                printf(w, depth, "[%02x] TC_CLASS - New class", Integer.valueOf(TC_CLASS));
                dumpDescriptor(descrMap, seq, dis, w, depth + 1, dis.readUnsignedByte());
                final int handle = seq.getAndIncrement();
                printf(w, depth + 1, "[--] New class handle is [%08x]", Integer.valueOf(handle));
                return;
            }
            // newArray
            case TC_ARRAY: {
                printf(w, depth, "[%02x] TC_ARRAY - New array", Integer.valueOf(TC_ARRAY));
                final ClassInfo classInfo = dumpDescriptor(descrMap, seq, dis, w, depth + 1, dis.readUnsignedByte());
                final int handle = seq.getAndIncrement();
                printf(w, depth + 1, "[--] New array handle is [%08x]", Integer.valueOf(handle));
                final int len = dis.readInt();
                printf(w, depth + 1, "[%x08] Array of length %d", Integer.valueOf(len), Integer.valueOf(len));
                for (int i = 0; i < len; i ++) {
                    final FieldType fieldType = FieldType.fromTypeCode(classInfo.name.charAt(1));
                    printf(w, depth + 2, "[%d] = %s", Integer.valueOf(i), fieldType.getValueString(dis));
                    if (fieldType.isObject()) {
                        dumpObject(descrMap, seq, dis, w, depth + 3, dis.readUnsignedByte());
                    }
                }
                return;
            }
            // newString
            case TC_STRING:
            case TC_LONGSTRING: {
                dumpString(descrMap, seq, dis, w, depth, leadByte);
                return;
            }
            // newEnum
            case TC_ENUM: {
                printf(w, depth, "[%02x] TC_ENUM - New enum", Integer.valueOf(TC_ENUM));
                dumpDescriptor(descrMap, seq, dis, w, depth + 1, dis.readUnsignedByte());
                final int handle = seq.getAndIncrement();
                printf(w, depth + 1, "[--] New enum handle is [%08x], constant name follows", Integer.valueOf(handle));
                dumpString(descrMap, seq, dis, w, depth + 2, dis.readUnsignedByte());
                return;
            }
            // newClassDesc
            case TC_CLASSTABLEDESC:
            case TC_PROXYCLASSDESC:
            case TC_CLASSDESC: {
                dumpDescriptor(descrMap, seq, dis, w, depth, leadByte);
                return;
            }
            // prevObject
            case TC_REFERENCE: {
                printf(w, depth, "[%02x] TC_REFERENCE - Backreference [%08x]", Integer.valueOf(TC_REFERENCE), Integer.valueOf(dis.readInt()));
                return;
            }
            // nullReference
            case TC_NULL: {
                printf(w, depth, "[%02x] TC_NULL - Null value", Integer.valueOf(TC_NULL));
                return;
            }
            // exception
            default: {
                throw new IllegalStateException("Wrong lead byte: " + leadByte);
            }
        }
    }

    private static void dumpString(Map<Integer, ClassInfo> descrMap, final AtomicInteger seq, final DataInputStream dis, final BufferedWriter w, final int depth, final int leadByte) throws IOException {
        switch (leadByte) {
            case TC_STRING: {
                final int handle = seq.getAndIncrement();
                final String str = dis.readUTF();
                printf(w, depth, "[%02x] TC_STRING - New string, handle [%08x] = \"%s\"", Integer.valueOf(TC_STRING), Integer.valueOf(handle), str);
                return;
            }
            case TC_LONGSTRING: {
                final int handle = seq.getAndIncrement();
                final String str = UTFUtils.readUTFBytesByByteCount(Marshalling.createByteInput(dis), dis.readLong());
                printf(w, depth, "[%02x] TC_LONGSTRING - New string, handle [%08x] = \"%s\"", Integer.valueOf(TC_LONGSTRING), Integer.valueOf(handle), str);
                return;
            }
            case TC_REFERENCE: {
                printf(w, depth, "[%02x] TC_REFERENCE - Backreference [%08x]", Integer.valueOf(TC_REFERENCE), Integer.valueOf(dis.readInt()));
                return;
            }
            case TC_NULL: {
                printf(w, depth, "[%02x] TC_NULL - Null value", Integer.valueOf(TC_NULL));
                return;
            }
            default: {
                throw new IllegalStateException("Wrong lead byte: " + leadByte);
            }
        }
    }

    private static void dumpFields(final ClassInfo info, final Map<Integer, ClassInfo> map, final AtomicInteger seq, final DataInputStream dis, final BufferedWriter w, final int depth) throws IOException {
        if ((info.flags & SC_EXTERNALIZABLE) != 0) {
            printf(w, depth, "[--] Externalizable data block:");
            dumpBlockData(map, seq, dis, w, depth + 1);
        } else if ((info.flags & SC_SERIALIZABLE) != 0) {
            if (info.parent != null) {
                dumpFields(info.parent, map, seq, dis, w, depth);
            }
            printf(w, depth, "[--] Fields for class %s:", info.name);
            for (FieldInfo fieldInfo : info.info) {
                printf(w, depth + 1, "[--] Field %s = %s", fieldInfo.name, fieldInfo.type.getValueString(dis));
                if (fieldInfo.type.isObject()) {
                    dumpObject(map, seq, dis, w, depth + 2, dis.readUnsignedByte());
                }
            }
            if ((info.flags & SC_WRITE_METHOD) != 0) {
                printf(w, depth, "[--] Custom data for class %s:", info.name);
                dumpBlockData(map, seq, dis, w, depth + 1);
            }
        } else {
            if (info.parent != null) {
                dumpFields(info.parent, map, seq, dis, w, depth);
            }
            printf(w, depth, "[--] No info for class %s", info.name);
        }
    }

    private static void dumpBlockData(final Map<Integer, ClassInfo> map, final AtomicInteger seq, final DataInputStream dis, final BufferedWriter w, final int depth) throws IOException {
        for (;;) {
            final int leadByte = dis.readUnsignedByte();
            if (leadByte == TC_ENDBLOCKDATA) {
                printf(w, depth, "[%02x] TC_ENDBLOCKDATA - End block data", Byte.valueOf(TC_ENDBLOCKDATA));
                return;
            } else {
                dumpContent(map, seq, dis, w, depth, leadByte);
            }
        }
    }

    private static ClassInfo dumpDescriptor(Map<Integer, ClassInfo> descrMap, final AtomicInteger seq, final DataInputStream dis, final BufferedWriter w, final int depth, final int leadByte) throws IOException {
        switch (leadByte) {
            // prevObject
            case TC_REFERENCE: {
                final int h = dis.readInt();
                printf(w, depth, "[%02x] TC_REFERENCE - Backreference [%08x]", Integer.valueOf(TC_REFERENCE), Integer.valueOf(h));
                return descrMap.get(Integer.valueOf(h));
            }
            // nullReference
            case TC_NULL: {
                printf(w, depth, "[%02x] TC_NULL - Null value", Integer.valueOf(TC_NULL));
                return null;
            }
            case TC_CLASSTABLEDESC: {
                final int handle = seq.getAndIncrement();
                printf(w, depth, "[%02x] TC_CLASSTABLEDESC - New class descriptor from table, handle [%08x] - stream is indeterminate from this point on", Integer.valueOf(TC_CLASSTABLEDESC), Integer.valueOf(handle));
                dumpBlockData(descrMap, seq, dis, w, depth);
                return null;
            }
            case TC_CLASSDESC: {
                final String name = dis.readUTF();
                final long svu = dis.readLong();
                final int handle = seq.getAndIncrement();
                printf(w, depth, "[%02x] TC_CLASSDESC - New class descriptor, class = \"%s\", uid = %d, handle [%08x]", Integer.valueOf(TC_CLASSDESC), name, Long.valueOf(svu), Integer.valueOf(handle));
                final int flags = dis.readUnsignedByte();
                final int fieldCount = dis.readUnsignedShort();
                FieldInfo[] info = new FieldInfo[fieldCount];
                printf(w, depth + 1, "[--] Flags: (" + (((flags & SC_BLOCK_DATA) != 0) ? " SC_BLOCK_DATA" : "") + (((flags & SC_ENUM) != 0) ? " SC_ENUM" : "") + (((flags & SC_EXTERNALIZABLE) != 0) ? " SC_EXTERNALIZABLE" : "") + (((flags & SC_SERIALIZABLE) != 0) ? " SC_SERIALIZABLE" : "") + (((flags & SC_WRITE_METHOD) != 0) ? " SC_WRITE_METHOD" : "") + ")");
                printf(w, depth + 1, "[--] %d fields", Integer.valueOf(fieldCount));
                for (int i = 0; i < fieldCount; i++) {
                    final int typeCode = dis.readUnsignedByte();
                    final String fieldName = dis.readUTF();
                    printf(w, depth + 2, "[--] Field \"%s\" type code '%s'%s", fieldName, Character.toString((char)typeCode), (typeCode == '[' || typeCode == 'L' ? ", type name follows" : ""));
                    if (typeCode == '[' || typeCode == 'L') {
                        dumpString(descrMap, seq, dis, w, depth + 3, dis.readUnsignedByte());
                    }
                    info[i] = new FieldInfo();
                    info[i].name = fieldName;
                    info[i].type = FieldType.fromTypeCode(typeCode);
                }
                printf(w, depth + 1, "[--] Class desc data block:");
                dumpBlockData(descrMap, seq, dis, w, depth + 2);
                final ClassInfo classInfo = new ClassInfo();
                descrMap.put(Integer.valueOf(handle), classInfo);
                classInfo.info = Arrays.asList(info);
                classInfo.name = name;
                classInfo.flags = flags;
                classInfo.svu = svu;
                printf(w, depth + 1, "[--] Superclass descriptor follows");
                classInfo.parent = dumpDescriptor(descrMap, seq, dis, w, depth + 3, dis.readUnsignedByte());
                return classInfo;
            }
            case TC_PROXYCLASSDESC: {
                final int handle = seq.getAndIncrement();
                final int count = dis.readInt();
                printf(w, depth, "[%02x] TC_PROXYCLASSDESC - New proxy class descriptor, %d interfaces", Integer.valueOf(TC_PROXYCLASSDESC), Integer.valueOf(count));
                for (int i = 0; i < count; i ++) {
                    printf(w, depth + 1, "[--] Interface: \"%s\"", dis.readUTF());
                }
                printf(w, depth + 1, "[--] Class desc data block:");
                dumpBlockData(descrMap, seq, dis, w, depth + 2);
                final ClassInfo classInfo = new ClassInfo();
                descrMap.put(Integer.valueOf(handle), classInfo);
                classInfo.info = Collections.emptyList();
                classInfo.name = String.format("proxy class %08x", Integer.valueOf(handle));
                printf(w, depth + 2, "[--] Superclass descriptor follows");
                classInfo.parent = dumpDescriptor(descrMap, seq, dis, w, depth + 3, dis.readUnsignedByte());
                return classInfo;
            }
            default: {
                throw new IllegalStateException("Wrong lead byte: " + leadByte);
            }
        }
    }

    private static void printf(BufferedWriter w, int depth, String format, Object... args) throws IOException {
        for (int i = 0; i < depth; i++) {
            w.write("    ");
        }
        w.write(String.format(format + "\n", args));
        w.flush();
    }
}
