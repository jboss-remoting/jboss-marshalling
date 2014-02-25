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

import org.jboss.marshalling.AbstractMarshaller;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.util.IdentityIntMap;
import org.jboss.marshalling.util.FieldPutter;
import org.jboss.marshalling.util.Kind;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableField;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.Externalizable;
import java.io.NotSerializableException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;
import java.security.AccessController;
import java.security.PrivilegedActionException;

/**
 *
 */
public final class SerialMarshaller extends AbstractMarshaller implements Marshaller, ExtendedObjectStreamConstants {

    private static final int MIN_BUFFER_SIZE = 16;

    private final SerializableClassRegistry registry;
    private final IdentityIntMap<Object> instanceCache;
    private final IdentityIntMap<Class<?>> descriptorCache;
    private final IdentityHashMap<Object, Object> replacementCache;
    private final IdentityHashMap<Class<?>, Externalizer> externalizers;
    private final int bufferSize;

    private SerialObjectOutputStream oos;
    private BlockMarshaller blockMarshaller;
    private int instanceSeq;

    SerialMarshaller(final AbstractMarshallerFactory marshallerFactory, final SerializableClassRegistry registry, final MarshallingConfiguration configuration) throws IOException {
        super(marshallerFactory, configuration);
        if (configuredVersion != 5) {
            throw new IOException("Only protocol version 5 is supported for writing");
        }
        this.registry = registry;
        instanceCache = new IdentityIntMap<Object>(configuration.getInstanceCount());
        descriptorCache = new IdentityIntMap<Class<?>>(configuration.getClassCount());
        replacementCache = new IdentityHashMap<Object, Object>(configuration.getInstanceCount());
        externalizers = new IdentityHashMap<Class<?>, Externalizer>(configuration.getClassCount());
        bufferSize = configuration.getBufferSize();
    }

    protected void doWriteObject(final Object orig, final boolean unshared) throws IOException {
        if (orig == null) {
            write(TC_NULL);
            return;
        }
        final IdentityHashMap<Object, Object> replacementCache = this.replacementCache;
        Object obj;
        if (replacementCache.containsKey(orig)) {
            obj = replacementCache.get(orig);
        } else {
            obj = orig;
        }
        final IdentityIntMap<Object> instanceCache = this.instanceCache;
        int v;
        obj = objectPreResolver.writeReplace(obj);
        final ObjectTable.Writer writer;
        // - first check for cached objects, Classes, or ObjectStreamClass
        if (! unshared && (v = instanceCache.get(obj, -1)) != -1) {
            write(TC_REFERENCE);
            writeInt(v + baseWireHandle);
            return;
        } else if ((writer = objectTable.getObjectWriter(obj)) != null) {
            write(TC_OBJECTTABLE);
            final int id = instanceSeq++;
            if (! unshared) instanceCache.put(obj, id);
            writer.writeObject(blockMarshaller, obj);
            doEndBlock();
            return;
        } else if (obj instanceof Class) {
            write(TC_CLASS);
            writeNewClassDescFor((Class<?>)obj);
            final int id = instanceSeq++;
            if (! unshared) instanceCache.put(obj, id);
            return;
        } else if (obj instanceof ObjectStreamClass) {
            throw new NotSerializableException(ObjectStreamClass.class.getName());
        }
        // - next check for replacements
        //   - first, check for implemented writeReplace method on the object
        final SerializableClassRegistry registry = this.registry;
        for (;;) {
            final Class<? extends Object> objClass = obj.getClass();
            final SerializableClass sc = registry.lookup(objClass);
            if (!sc.hasWriteReplace()) {
                break;
            }
            final Object replacement = sc.callWriteReplace(obj);
            try {
                if (replacement == null || replacement == obj || obj.getClass() == objClass) {
                    break;
                }
            } finally {
                obj = replacement;
            }
        }
        obj = objectResolver.writeReplace(obj);
        // - next, if the object was replaced we do our original checks again...
        if (obj != orig) {
            // cache the replacement
            replacementCache.put(orig, obj);
            if (obj == null) {
                write(TC_NULL);
                return;
            } else if (! unshared && (v = instanceCache.get(obj, -1)) != -1) {
                write(TC_REFERENCE);
                writeInt(v);
                return;
            } else if (obj instanceof Class) {
                write(TC_CLASS);
                writeNewClassDescFor((Class<?>)obj);
                final int id = instanceSeq++;
                if (! unshared) instanceCache.put(obj, id);
                return;
            } else if (obj instanceof ObjectStreamClass) {
                throw new NotSerializableException(ObjectStreamClass.class.getName());
            }
        }
        // - next check for other special types
        if (obj instanceof String) {
            final String string = (String) obj;
            final long len = UTFUtils.getLongUTFLength(string);
            if (len < 65536L) {
                write(TC_STRING);
                final int id = instanceSeq++;
                if (! unshared) instanceCache.put(obj, id);
                writeShort((int) len);
                UTFUtils.writeUTFBytes(this, string);
                return;
            } else {
                write(TC_LONGSTRING);
                final int id = instanceSeq++;
                if (! unshared) instanceCache.put(obj, id);
                writeLong(len);
                UTFUtils.writeUTFBytes(this, string);
                return;
            }
        }
        final Class<?> objClass = obj.getClass();
        if (obj instanceof Enum) {
            write(TC_ENUM);
            final Enum theEnum = (Enum) obj;
            writeClassDescFor(theEnum.getDeclaringClass());
            final int id = instanceSeq++;
            if (! unshared) instanceCache.put(obj, id);
            doWriteObject(theEnum.name(), false);
            return;
        } else if (objClass.isArray()) {
            write(TC_ARRAY);
            writeClassDescFor(objClass);
            final int id = instanceSeq++;
            if (! unshared) instanceCache.put(obj, id);
            if (obj instanceof byte[]) {
                final byte[] bytes = (byte[]) obj;
                writeInt(bytes.length);
                write(bytes);
            } else if (obj instanceof short[]) {
                final short[] shorts = (short[]) obj;
                writeInt(shorts.length);
                for (short s : shorts) {
                    writeShort(s);
                }
            } else if (obj instanceof int[]) {
                final int[] ints = (int[]) obj;
                writeInt(ints.length);
                for (int s : ints) {
                    writeInt(s);
                }
            } else if (obj instanceof long[]) {
                final long[] longs = (long[]) obj;
                writeInt(longs.length);
                for (long s : longs) {
                    writeLong(s);
                }
            } else if (obj instanceof float[]) {
                final float[] floats = (float[]) obj;
                writeInt(floats.length);
                for (float s : floats) {
                    writeFloat(s);
                }
            } else if (obj instanceof double[]) {
                final double[] doubles = (double[]) obj;
                writeInt(doubles.length);
                for (double s : doubles) {
                    writeDouble(s);
                }
            } else if (obj instanceof boolean[]) {
                final boolean[] booleans = (boolean[]) obj;
                writeInt(booleans.length);
                for (boolean s : booleans) {
                    writeBoolean(s);
                }
            } else if (obj instanceof char[]) {
                final char[] chars = (char[]) obj;
                writeInt(chars.length);
                for (char s : chars) {
                    writeChar(s);
                }
            } else {
                final Object[] objs = (Object[]) obj;
                writeInt(objs.length);
                for (Object o : objs) {
                    doWriteObject(o, false);
                }
            }
            return;
        }
        Externalizer externalizer;
        if (externalizers.containsKey(objClass)) {
            externalizer = externalizers.get(objClass);
        } else {
            externalizer = classExternalizerFactory.getExternalizer(objClass);
            externalizers.put(objClass, externalizer);
        }
        if (externalizer != null) {
            final ExternalizedObject eo = new ExternalizedObject(externalizer, obj);
            doWriteObject(eo, unshared);
            replacementCache.put(obj, eo);
            return;
        } else if (obj instanceof Externalizable) {
            write(TC_OBJECT);
            writeClassDescFor(objClass);
            final int id = instanceSeq++;
            if (! unshared) instanceCache.put(obj, id);
            final Externalizable externalizable = (Externalizable) obj;
            externalizable.writeExternal(blockMarshaller);
            doEndBlock();
            return;
        } else if (serializabilityChecker.isSerializable(objClass)) {
            write(TC_OBJECT);
            writeClassDescFor(objClass);
            final int id = instanceSeq++;
            if (! unshared) instanceCache.put(obj, id);
            writeSerialData(objClass, obj);
            return;
        } else {
            throw new NotSerializableException(objClass.getName());
        }
    }

    private void writeSerialData(Class<?> objClass, Object obj) throws IOException {
        final Class<?> superClass = objClass.getSuperclass();
        if (superClass != null && serializabilityChecker.isSerializable(superClass)) {
            writeSerialData(superClass, obj);
        }
        final SerializableClass sc = registry.lookup(objClass);
        if (sc.hasWriteObject()) {
            final SerialObjectOutputStream oos = getObjectOutputStream();
            final Object oldObj = oos.saveCurrentObject(obj);
            final SerializableClass oldSc = oos.saveCurrentSerializableClass(sc);
            final Map<String,FieldPutter> map = oos.saveCurrentFieldMap();
            final SerialObjectOutputStream.State oldState = oos.saveState();
            try {
                sc.callWriteObject(obj, oos);
            } finally {
                oos.setCurrentObject(oldObj);
                oos.setCurrentSerializableClass(oldSc);
                oos.setCurrentFieldMap(map);
                oos.restoreState(oldState);
            }
            doEndBlock();
        } else {
            doWriteFields(sc, obj);
        }
    }

    private final PrivilegedExceptionAction<SerialObjectOutputStream> createObjectOutputStreamAction = new PrivilegedExceptionAction<SerialObjectOutputStream>() {
        public SerialObjectOutputStream run() throws IOException {
            return new SerialObjectOutputStream(SerialMarshaller.this, blockMarshaller);
        }
    };

    private SerialObjectOutputStream createObjectOutputStream() throws IOException {
        try {
            return AccessController.doPrivileged(createObjectOutputStreamAction);
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    private SerialObjectOutputStream getObjectOutputStream() throws IOException {
        if (oos == null) {
            oos = createObjectOutputStream();
        }
        return oos;
    }

    protected void doWriteFields(final SerializableClass info, final Object obj) throws IOException {
        final SerializableField[] serializableFields = info.getFields();
        for (SerializableField serializableField : serializableFields) {
            try {
                final Field field = serializableField.getField();
                switch (serializableField.getKind()) {
                    case BOOLEAN: {
                        writeBoolean(field == null ? false : field.getBoolean(obj));
                        break;
                    }
                    case BYTE: {
                        writeByte(field == null ? 0 : field.getByte(obj));
                        break;
                    }
                    case SHORT: {
                        writeShort(field == null ? 0 : field.getShort(obj));
                        break;
                    }
                    case INT: {
                        writeInt(field == null ? 0 : field.getInt(obj));
                        break;
                    }
                    case CHAR: {
                        writeChar(field == null ? 0 : field.getChar(obj));
                        break;
                    }
                    case LONG: {
                        writeLong(field == null ? 0 : field.getLong(obj));
                        break;
                    }
                    case DOUBLE: {
                        writeDouble(field == null ? 0 : field.getDouble(obj));
                        break;
                    }
                    case FLOAT: {
                        writeFloat(field == null ? 0 : field.getFloat(obj));
                        break;
                    }
                }
            } catch (IllegalAccessException e) {
                final InvalidObjectException ioe = new InvalidObjectException("Unexpected illegal access exception");
                ioe.initCause(e);
                throw ioe;
            }
        }
        for (SerializableField serializableField : serializableFields) {
            try {
                final Field field = serializableField.getField();
                Kind i = serializableField.getKind();
                if (i == Kind.OBJECT) {
                    doWriteObject(field == null ? null : field.get(obj), serializableField.isUnshared());
                }
            } catch (IllegalAccessException e) {
                final InvalidObjectException ioe = new InvalidObjectException("Unexpected illegal access exception");
                ioe.initCause(e);
                throw ioe;
            }
        }
    }

    private void writeClassDescFor(final Class<?> forClass) throws IOException {
        if (forClass == null) {
            write(TC_NULL);
        } else {
            final int id = descriptorCache.get(forClass, -1);
            if (id == -1) {
                writeNewClassDescFor(forClass);
            } else {
                write(TC_REFERENCE);
                writeInt(id + baseWireHandle);
            }
        }
    }

    private void writeNewClassDescFor(final Class<?> forClass) throws IOException {
        if (Proxy.isProxyClass(forClass)) {
            writeNewProxyClassDesc(forClass);
        } else {
            writeNewPlainClassDesc(forClass);
        }
    }

    private void writeNewProxyClassDesc(final Class<?> forClass) throws IOException {
        write(TC_PROXYCLASSDESC);
        descriptorCache.put(forClass, instanceSeq++);
        final String[] names = classResolver.getProxyInterfaces(forClass);
        writeInt(names.length);
        for (String name : names) {
            writeUTF(name);
        }
        classResolver.annotateProxyClass(blockMarshaller, forClass);
        doEndBlock();
        writeClassDescFor(forClass.getSuperclass());
    }

    private void writeNewPlainClassDesc(final Class<?> forClass) throws IOException {
        final ClassTable.Writer writer = classTable.getClassWriter(forClass);
        if (writer != null) {
            write(TC_CLASSTABLEDESC);
            descriptorCache.put(forClass, instanceSeq++);
            writer.writeClass(blockMarshaller, forClass);
            doEndBlock();
            return;
        }
        write(TC_CLASSDESC);
        writeUTF(classResolver.getClassName(forClass));
        descriptorCache.put(forClass, instanceSeq++);
        if (forClass.isEnum()) {
            writeLong(0L);
            write(SC_SERIALIZABLE | SC_ENUM);
            writeShort(0);
        } else if (serializabilityChecker.isSerializable(forClass)) {
            final SerializableClass sc = registry.lookup(forClass);
            final long svu = sc.getEffectiveSerialVersionUID();
            writeLong(svu);
            if (Externalizable.class.isAssignableFrom(forClass)) {
                // todo: add a protocol_1 option?
                write(SC_EXTERNALIZABLE + SC_BLOCK_DATA);
                writeShort(0);
            } else {
                if (sc.hasWriteObject()) {
                    write(SC_WRITE_METHOD + SC_SERIALIZABLE);
                } else {
                    write(SC_SERIALIZABLE);
                }
                final SerializableField[] fields = sc.getFields();
                writeShort(fields.length);
                // first write primitive fields, then object fields
                for (SerializableField field : fields) {
                    final Kind kind = field.getKind();
                    final String name = field.getName();
                    final Class<?> type;
                    try {
                        type = field.getType();
                    } catch (ClassNotFoundException e) {
                        // not possible
                        throw new InvalidClassException(forClass.getName(), "Field " + name + "'s class was not found");
                    }
                    if (kind != Kind.OBJECT) {
                        write(primitives.get(type, -1));
                        writeUTF(name);
                    }
                }
                for (SerializableField field : fields) {
                    final Kind kind = field.getKind();
                    final String name = field.getName();
                    final Class<?> type;
                    try {
                        type = field.getType();
                    } catch (ClassNotFoundException e) {
                        // not possible
                        throw new InvalidClassException(forClass.getName(), "Field " + name + "'s class was not found");
                    }
                    if (kind == Kind.OBJECT) {
                        final String signature = getSignature(type).intern();
                        write(signature.charAt(0));
                        writeUTF(name);
                        writeObject(signature);
                    }
                }
            }
        } else {
            writeLong(0L);
            write(0);
            writeShort(0);
        }
        classResolver.annotateClass(blockMarshaller, forClass);
        doEndBlock();
        final Class<?> sc = forClass.getSuperclass();
        if (sc != null && serializabilityChecker.isSerializable(sc) && ! forClass.isEnum()) {
            writeClassDescFor(sc);
        } else {
            write(TC_NULL);
        }
    }

    private void doEndBlock() throws IOException {
        blockMarshaller.flush();
        write(TC_ENDBLOCKDATA);
    }

    private static final IdentityIntMap<Class<?>> primitives;

    static {
        primitives = new IdentityIntMap<Class<?>>(32);
        primitives.put(byte.class, 'B');
        primitives.put(char.class, 'C');
        primitives.put(double.class, 'D');
        primitives.put(float.class, 'F');
        primitives.put(int.class, 'I');
        primitives.put(long.class, 'J');
        primitives.put(short.class, 'S');
        primitives.put(boolean.class, 'Z');
        primitives.put(void.class, 'V');
    }

    private static String getSignature(final Class<?> type) {
        final int id;
        if ((id = primitives.get(type, -1)) != -1) {
            return Character.toString((char)id);
        } else if (type.isArray()) {
            return "[" + getSignature(type.getComponentType());
        } else {
            return "L" + type.getName().replace('.', '/') + ";";
        }
    }

    public void clearInstanceCache() throws IOException {
        instanceCache.clear();
        descriptorCache.clear();
        replacementCache.clear();
        externalizers.clear();
        instanceSeq = 0;
        if (byteOutput != null) {
            write(TC_RESET);
        }
    }

    public void clearClassCache() throws IOException {
        clearInstanceCache();
    }

    public void start(final ByteOutput byteOutput) throws IOException {
        blockMarshaller = new BlockMarshaller(this, bufferSize < MIN_BUFFER_SIZE ? MIN_BUFFER_SIZE : bufferSize);
        super.start(byteOutput);
        writeShort(configuredVersion);
    }

    public void finish() throws IOException {
        try {
            super.finish();
        } finally {
            blockMarshaller = null;
            oos = null;
        }
    }

    public void flush() throws IOException {
        final BlockMarshaller blockMarshaller = this.blockMarshaller;
        if (blockMarshaller != null) {
            blockMarshaller.flush();
        }
        super.flush();
    }
    
    void writeNoBlockFlush(final byte[] bytes, final int off, int len) throws IOException {
        super.flush();
        byteOutput.write(bytes, off, len);
    }
}
