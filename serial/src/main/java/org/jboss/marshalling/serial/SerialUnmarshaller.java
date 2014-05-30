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

import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.AbstractUnmarshaller;
import org.jboss.marshalling.UTFUtils;
import org.jboss.marshalling.ByteInput;
import static org.jboss.marshalling.Marshalling.createOptionalDataException;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableField;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.ObjectStreamClass;
import java.io.InvalidClassException;
import java.io.WriteAbortedException;
import java.io.InvalidObjectException;
import java.io.Externalizable;
import java.io.NotSerializableException;
import java.io.ObjectInputValidation;
import java.util.ArrayList;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Collections;
import java.lang.reflect.Array;
import java.security.PrivilegedExceptionAction;
import java.security.AccessController;
import java.security.PrivilegedActionException;

/**
 *
 */
public final class SerialUnmarshaller extends AbstractUnmarshaller implements Unmarshaller, ExtendedObjectStreamConstants {

    private static final Object UNSHARED = new Object();
    private static final Object UNRESOLVED = new Object();

    private final ArrayList<Object> instanceCache;
    private final SerializableClassRegistry registry;

    private int depth = 0;

    private SerialObjectInputStream ois;
    private BlockUnmarshaller blockUnmarshaller;
    private int version;

    SerialUnmarshaller(final SerialMarshallerFactory factory, final SerializableClassRegistry registry, final MarshallingConfiguration configuration) {
        super(factory, configuration);
        this.registry = registry;
        instanceCache = new ArrayList<Object>(configuration.getInstanceCount());
    }

    protected Object doReadObject(final boolean unshared) throws ClassNotFoundException, IOException {
        final Object obj = objectPreResolver.readResolve((doReadObject(readUnsignedByte(), unshared)));
        if (depth == 0) try {
            for (Set<ObjectInputValidation> validations : validationMap.values()) {
                for (ObjectInputValidation validation : validations) {
                    validation.validateObject();
                }
            }
        } finally {
            validationMap.clear();
        }
        return obj;
    }

    String doReadString() throws IOException, ClassNotFoundException {
        final int leadByte = readUnsignedByte();
        switch (leadByte) {
            case TC_NULL: return null;
            case TC_REFERENCE: try {
                return (String) readBackReference(readInt());
            } catch (ClassCastException e) {
                throw new StreamCorruptedException("Expected a string backreference");
            }
            case TC_STRING: return (String) doReadObject(leadByte, false);
            default: throw new StreamCorruptedException("Expected a string object");
        }
    }

    Object readBackReference(int handle) throws IOException {
        final int idx = handle - baseWireHandle;
        if (idx < 0 || idx >= instanceCache.size()) {
            throw new StreamCorruptedException(String.format("Invalid backreference: %08x", Integer.valueOf(handle)));
        }
        final Object obj = instanceCache.get(idx);
        if (obj == UNSHARED) {
            throw new StreamCorruptedException(String.format("Invalid backreference to unshared instance: %08x", Integer.valueOf(handle)));
        }
        if (obj == UNRESOLVED) {
            throw new StreamCorruptedException(String.format("Invalid backreference to unresolved instance: %08x", Integer.valueOf(handle)));
        }
        return obj;
    }

    Object doReadObject(int leadByte, final boolean unshared) throws IOException, ClassNotFoundException {
        depth ++;
        try {
            final BlockUnmarshaller blockUnmarshaller = this.blockUnmarshaller;
            for (;;) switch (leadByte) {
                case TC_NULL: {
                    return null;
                }

                case TC_REFERENCE: {
                    final Object prevObj = readBackReference(readInt());
                    if (prevObj instanceof Descriptor) {
                        throw objectStreamClassException();
                    }
                    return prevObj;
                }

                case TC_CLASS: {
                    final Descriptor descriptor = readNonNullClassDescriptor();
                    final Class<?> obj = descriptor.getType();
                    instanceCache.add(obj);
                    return obj;
                }

                case TC_CLASSDESC: {
                    throw objectStreamClassException();
                }

                case TC_PROXYCLASSDESC: {
                    throw objectStreamClassException();
                }

                case TC_STRING: {
                    final int len = readUnsignedShort();
                    final String str = UTFUtils.readUTFBytesByByteCount(this, len);
                    return replaceOrReturn(unshared, str);
                }

                case TC_LONGSTRING: {
                    final long len = super.readLong();
                    final String str = UTFUtils.readUTFBytesByByteCount(this, len);
                    return replaceOrReturn(unshared, str);
                }

                case TC_ARRAY: {
                    final Descriptor descriptor = readNonNullClassDescriptor();
                    final int idx = instanceCache.size();
                    instanceCache.add(UNRESOLVED);
                    final int size = readInt();
                    final Class<?> type = descriptor.getType();
                    if (! type.isArray()) {
                        throw new InvalidClassException(type.getName(), "Expected array type");
                    }
                    final Class<?> ct = type.getComponentType();
                    if (ct.isPrimitive()) {
                        if (ct == byte.class) {
                            final byte[] bytes = new byte[size];
                            readFully(bytes);
                            return replaceOrReturn(unshared, bytes, idx);
                        } else if (ct == short.class) {
                            final short[] shorts = new short[size];
                            for (int i = 0; i < shorts.length; i++) {
                                shorts[i] = readShort();
                            }
                            return replaceOrReturn(unshared, shorts, idx);
                        } else if (ct == int.class) {
                            final int[] ints = new int[size];
                            for (int i = 0; i < ints.length; i++) {
                                ints[i] = readInt();
                            }
                            return replaceOrReturn(unshared, ints, idx);
                        } else if (ct == long.class) {
                            final long[] longs = new long[size];
                            for (int i = 0; i < longs.length; i++) {
                                longs[i] = readLong();
                            }
                            return replaceOrReturn(unshared, longs, idx);
                        } else if (ct == float.class) {
                            final float[] floats = new float[size];
                            for (int i = 0; i < floats.length; i++) {
                                floats[i] = readFloat();
                            }
                            return replaceOrReturn(unshared, floats, idx);
                        } else if (ct == double.class) {
                            final double[] doubles = new double[size];
                            for (int i = 0; i < doubles.length; i++) {
                                doubles[i] = readDouble();
                            }
                            return replaceOrReturn(unshared, doubles, idx);
                        } else if (ct == boolean.class) {
                            final boolean[] booleans = new boolean[size];
                            for (int i = 0; i < booleans.length; i++) {
                                booleans[i] = readBoolean();
                            }
                            return replaceOrReturn(unshared, booleans, idx);
                        } else if (ct == char.class) {
                            final char[] chars = new char[size];
                            for (int i = 0; i < chars.length; i++) {
                                chars[i] = readChar();
                            }
                            return replaceOrReturn(unshared, chars, idx);
                        } else {
                            throw new InvalidClassException(type.getName(), "Invalid component type");
                        }
                    } else {
                        final Object[] objects = (Object[]) Array.newInstance(ct, size);
                        instanceCache.set(idx, objects);
                        for (int i = 0; i < objects.length; i++) {
                            objects[i] = doReadObject(false);
                        }
                        return replaceOrReturn(unshared, objects, idx);
                    }
                }

                case TC_ENUM: {
                    final Descriptor descriptor = readNonNullClassDescriptor();
                    final Class<? extends Enum> enumType;
                    try {
                        enumType = descriptor.getType().asSubclass(Enum.class);
                    } catch (ClassCastException e) {
                        throw new InvalidClassException("Expected an enum class descriptor");
                    }
                    final int idx = instanceCache.size();
                    instanceCache.add(UNRESOLVED);
                    final String constName = (String) doReadObject(false);
                    final Enum obj = Enum.valueOf(enumType, constName);
                    return replaceOrReturn(unshared, obj, idx);
                }

                case TC_OBJECT: {
                    final Descriptor descriptor = readNonNullClassDescriptor();
                    if ((descriptor.getFlags() & (SC_SERIALIZABLE | SC_EXTERNALIZABLE)) == 0) {
                        throw new NotSerializableException(descriptor.getClass().getName());
                    }
                    final Object obj;
                    final int idx;
                    final Class<?> objClass = descriptor.getType();
                    final SerializableClass sc = registry.lookup(objClass);
                    if ((descriptor.getFlags() & SC_EXTERNALIZABLE) != 0) {
                        if (sc.hasObjectInputConstructor()) {
                            obj = sc.callObjectInputConstructor(blockUnmarshaller);
                        } else if (sc.hasNoArgConstructor()) {
                            obj = sc.callNoArgConstructor();
                        } else {
                            throw new InvalidClassException(objClass.getName(), "Class is non-public or has no public no-arg constructor");
                        }
                        idx = instanceCache.size();
                        instanceCache.add(unshared ? UNSHARED : obj);
                        if (obj instanceof Externalizable) {
                            final Externalizable externalizable = (Externalizable) obj;
                            if ((descriptor.getFlags() & SC_BLOCK_DATA) != 0) {
                                externalizable.readExternal(blockUnmarshaller);
                                blockUnmarshaller.readToEndBlockData();
                                blockUnmarshaller.unblock();
                            } else {
                                // data is not in block format!
                                externalizable.readExternal(this);
                            }
                        } else {
                            throw new InvalidObjectException("Created object should be Externalizable but it is not");
                        }
                    } else {
                        obj = sc.callNonInitConstructor();
                        if (obj instanceof Externalizable) {
                            throw new InvalidObjectException("Created object should not be Externalizable but it is");
                        }
                        idx = instanceCache.size();
                        instanceCache.add(unshared ? UNSHARED : obj);
                        doReadSerialObject(descriptor, obj);
                    }
                    if (sc.hasReadResolve()) {
                        final Object replacement = sc.callReadResolve(obj);
                        if (! unshared) instanceCache.set(idx, replacement);
                        return replaceOrReturn(unshared, replacement, idx);
                    }
                    return replaceOrReturn(unshared, obj, idx);
                }

                case TC_OBJECTTABLE: {
                    final int idx = instanceCache.size();
                    instanceCache.add(unshared ? UNSHARED : UNRESOLVED);
                    final Object obj = objectTable.readObject(blockUnmarshaller);
                    blockUnmarshaller.readToEndBlockData();
                    blockUnmarshaller.unblock();
                    if (! unshared) instanceCache.set(idx, obj);
                    return obj;
                }

                case TC_EXCEPTION: {
                    clearInstanceCache();
                    final IOException ex = (IOException) doReadObject(false);
                    clearInstanceCache();
                    throw new WriteAbortedException("Write aborted", ex);
                }

                case TC_BLOCKDATA:
                case TC_BLOCKDATALONG: {
                    blockUnmarshaller.readBlockHeader(leadByte);
                    throw createOptionalDataException(blockUnmarshaller.remaining());
                }

                case TC_ENDBLOCKDATA: {
                    throw createOptionalDataException(0);
                }

                case TC_RESET: {
                    if (depth > 1) {
                        throw new StreamCorruptedException("Reset token in the middle of stream processing");
                    }
                    clearInstanceCache();
                    leadByte = readByte() & 0xff;
                    continue;
                }

                default: {
                    throw badLeadByte(leadByte);
                }
            }
        } finally {
            depth --;
        }
    }

    private void doReadSerialObject(final Descriptor descriptor, final Object obj) throws ClassNotFoundException, IOException {
        final Descriptor parent = descriptor.getParent();
        if (parent != null) {
            doReadSerialObject(parent, obj);
        }
        Class<?> type = descriptor.getType();
        descriptor.readSerial(this, type == null ? null : registry.lookup(type), obj);
    }

    private static InvalidClassException objectStreamClassException() {
        return new InvalidClassException(ObjectStreamClass.class.getName(), "Cannot read ObjectStreamClass instances");
    }

    private Descriptor readNonNullClassDescriptor() throws ClassNotFoundException, IOException {
        final Descriptor desc = readClassDescriptor(true);
        if (desc == null) {
            throw new StreamCorruptedException("Unexpected null class descriptor");
        }
        return desc;
    }

    private Descriptor readClassDescriptor(final boolean required) throws ClassNotFoundException, IOException {
        return readClassDescriptor(readUnsignedByte(), required);
    }

    private static final int[] EMPTY_INTS = new int[0];
    private static final String[] EMPTY_STRINGS = new String[0];

    private Descriptor readClassDescriptor(int leadByte, final boolean required) throws IOException, ClassNotFoundException {
        final BlockUnmarshaller blockUnmarshaller = this.blockUnmarshaller;
        switch (leadByte) {
            case TC_CLASSTABLEDESC: {
                final Class<?> clazz = classTable.readClass(blockUnmarshaller);
                final int idx = instanceCache.size();
                instanceCache.add(UNRESOLVED);
                blockUnmarshaller.readToEndBlockData();
                blockUnmarshaller.unblock();
                Descriptor descriptor = descriptorForClass(clazz);
                instanceCache.set(idx, descriptor);
                return descriptor;
            }
            case TC_CLASSDESC: {
                final String className = readUTF();
                final long svu = readLong();
                final int idx = instanceCache.size();
                instanceCache.add(UNRESOLVED);
                final int descFlags = readUnsignedByte();
                final int fieldCount = readUnsignedShort();
                final int[] typecodes;
                final String[] names;
                final String[] fieldSignatures;
                if (fieldCount == 0) {
                    // either no fields or it's not serializable
                    typecodes = EMPTY_INTS;
                    names = EMPTY_STRINGS;
                    fieldSignatures = EMPTY_STRINGS;
                } else {
                    typecodes = new int[fieldCount];
                    names = new String[fieldCount];
                    fieldSignatures = new String[fieldCount];
                    for (int i = 0; i < fieldCount; i ++) {
                        typecodes[i] = readUnsignedByte();
                        names[i] = readUTF();
                        if (typecodes[i] == '[' || typecodes[i] == 'L') {
                            fieldSignatures[i] = doReadString();
                        }
                    }
                }
                Class<?> clazz = null;
                try {
                    clazz = classResolver.resolveClass(blockUnmarshaller, className, svu);
                } catch (ClassNotFoundException cnfe) {
                    if (required) throw cnfe;
                }
                blockUnmarshaller.readToEndBlockData();
                blockUnmarshaller.unblock();
                final Descriptor superDescr = readClassDescriptor(false);
                final Descriptor descriptor;
                if (clazz == null) {
                    if (descFlags == 0) {
                        descriptor = superDescr;
                    } else {
                        // HAS FIELDS - discard content
                        final SerializableField[] fields = new SerializableField[fieldCount];
                        for (int i = 0; i < fieldCount; i ++) {
                            final Class<?> fieldType;
                            switch (typecodes[i]) {
                                case 'B': {
                                    fieldType = byte.class;
                                    break;
                                }
                                case 'C': {
                                    fieldType = char.class;
                                    break;
                                }
                                case 'D': {
                                    fieldType = double.class;
                                    break;
                                }
                                case 'F': {
                                    fieldType = float.class;
                                    break;
                                }
                                case 'I': {
                                    fieldType = int.class;
                                    break;
                                }
                                case 'J': {
                                    fieldType = long.class;
                                    break;
                                }
                                case 'S': {
                                    fieldType = short.class;
                                    break;
                                }
                                case 'Z': {
                                    fieldType = boolean.class;
                                    break;
                                }
                                case 'L':
                                case '[': {
                                    fieldType = Object.class;
                                    break;
                                }
                                default: {
                                    throw new StreamCorruptedException("Invalid field typecode " + typecodes[i]);
                                }
                            }
                            fields[i] = new SerializableField(fieldType, names[i], false);
                        }
                        descriptor = new UnknownDescriptor(superDescr, fields, descFlags);
                    }
                } else {
                    final Class<?> superClazz = clazz.getSuperclass();
                    if (superDescr == null || superDescr.getNearestType().isAssignableFrom(superClazz)) {
                        if (descFlags == 0) {
                            descriptor = new NoDataDescriptor(clazz, bridge(superDescr, superClazz));
                        } else {
                            // HAS FIELDS
                            final SerializableField[] fields = new SerializableField[fieldCount];
                            final SerializableClass sc = registry.lookup(clazz);
                            for (int i = 0; i < fieldCount; i ++) {
                                final Class<?> fieldType;
                                switch (typecodes[i]) {
                                    case 'B': {
                                        fieldType = byte.class;
                                        break;
                                    }
                                    case 'C': {
                                        fieldType = char.class;
                                        break;
                                    }
                                    case 'D': {
                                        fieldType = double.class;
                                        break;
                                    }
                                    case 'F': {
                                        fieldType = float.class;
                                        break;
                                    }
                                    case 'I': {
                                        fieldType = int.class;
                                        break;
                                    }
                                    case 'J': {
                                        fieldType = long.class;
                                        break;
                                    }
                                    case 'S': {
                                        fieldType = short.class;
                                        break;
                                    }
                                    case 'Z': {
                                        fieldType = boolean.class;
                                        break;
                                    }
                                    case 'L':
                                    case '[': {
                                        fieldType = Object.class;
                                        break;
                                    }
                                    default: {
                                        throw new StreamCorruptedException("Invalid field typecode " + typecodes[i]);
                                    }
                                }
                                fields[i] = sc.getSerializableField(names[i], fieldType, false);
                            }
                            descriptor = new PlainDescriptor(clazz, bridge(superDescr, superClazz), fields, descFlags);
                        }
                    } else {
                        throw new InvalidClassException(clazz.getName(), "Class hierarchy mismatch");
                    }
                }
                instanceCache.set(idx, descriptor);
                return descriptor;
            }
            case TC_PROXYCLASSDESC: {
                final int idx = instanceCache.size();
                instanceCache.add(UNRESOLVED);
                final int cnt = readInt();
                final String[] interfaces = new String[cnt];
                for (int i = 0; i < interfaces.length; i++) {
                    interfaces[i] = readUTF();
                }
                final Class<?> clazz = classResolver.resolveProxyClass(blockUnmarshaller, interfaces);
                blockUnmarshaller.readToEndBlockData();
                blockUnmarshaller.unblock();
                final Descriptor superDescr = readClassDescriptor(true);
                final ProxyDescriptor descr = new ProxyDescriptor(clazz, superDescr, interfaces);
                instanceCache.set(idx, descr);
                return descr;
            }
            case TC_NULL: {
                return null;
            }
            case TC_REFERENCE: {
                try {
                    return (Descriptor) readBackReference(readInt());
                } catch (ClassCastException e) {
                    throw new StreamCorruptedException("Backreference was not a resolved class descriptor");
                }
            }
            case TC_EXCEPTION: {
                clearInstanceCache();
                final IOException ex = (IOException) doReadObject(false);
                clearInstanceCache();
                throw new WriteAbortedException("Write aborted", ex);
            }
            default:
                throw badLeadByte(leadByte);
        }
    }

    private Descriptor bridge(final Descriptor descriptor, final Class<?> type) {
        if (descriptor == null) return null;
        final Class<?> superDescrClazz = descriptor.getType();
        if (superDescrClazz == null) {
            return descriptor;
        }
        final Class<?> typeSuperclass = type.getSuperclass();
        if (type == superDescrClazz) {
            return descriptor;
        } else {
            return new NoDataDescriptor(type, bridge(descriptor, typeSuperclass));
        }
    }

    private StreamCorruptedException badLeadByte(final int leadByte) {
        return new StreamCorruptedException("Unexpected lead byte " + leadByte);
    }

    public void clearInstanceCache() throws IOException {
        instanceCache.clear();
    }

    public void clearClassCache() throws IOException {
        instanceCache.clear();
    }

    public void start(final ByteInput byteInput) throws IOException {
        depth = 0;
        blockUnmarshaller = new BlockUnmarshaller(this);
        super.start(byteInput);
        int version = readUnsignedShort();
        if (version > 5) {
            throw new IOException("Unsupported protocol version " + version);
        }
        this.version = version;
    }

    public void finish() throws IOException {
        super.finish();
        blockUnmarshaller = null;
        depth = 0;
    }

    public void close() throws IOException {
        finish();
    }

    BlockUnmarshaller getBlockUnmarshaller() {
        return blockUnmarshaller;
    }

    private final PrivilegedExceptionAction<SerialObjectInputStream> createObjectOutputStreamAction = new PrivilegedExceptionAction<SerialObjectInputStream>() {
        public SerialObjectInputStream run() throws IOException {
            return new SerialObjectInputStream(SerialUnmarshaller.this);
        }
    };

    private SerialObjectInputStream createObjectInputStream() throws IOException {
        try {
            return AccessController.doPrivileged(createObjectOutputStreamAction);
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    SerialObjectInputStream getObjectInputStream() throws IOException {
        return ois == null ? (ois = createObjectInputStream()) : ois;
    }

    private final SortedMap<Integer, Set<ObjectInputValidation>> validationMap = new TreeMap<Integer, Set<ObjectInputValidation>>(Collections.reverseOrder());

    void addValidation(final ObjectInputValidation validation, final int prio) {
        final Set<ObjectInputValidation> validations;
        final Integer prioKey = Integer.valueOf(prio);
        if (validationMap.containsKey(prioKey)) {
            validations = validationMap.get(prioKey);
        } else {
            validations = new HashSet<ObjectInputValidation>();
            validationMap.put(prioKey, validations);
        }
        validations.add(validation);
    }

    public Descriptor descriptorForClass(final Class<?> clazz) {
        if (Externalizable.class.isAssignableFrom(clazz)) {
            // todo - make WRITE_METHOD depend on block mode
            return new PlainDescriptor(clazz, null, SerializableClass.NOFIELDS, SC_EXTERNALIZABLE | SC_BLOCK_DATA);
        } else if (serializabilityChecker.isSerializable(clazz)) {
            final Class<?> superclass = clazz.getSuperclass();
            final Descriptor parent;
            if (superclass != null && serializabilityChecker.isSerializable(superclass)) {
                parent = descriptorForClass(superclass);
            } else {
                parent = null;
            }
            final SerializableClass serializableClass = registry.lookup(clazz);
            return new PlainDescriptor(clazz, parent, serializableClass.getFields(), SC_SERIALIZABLE | (serializableClass.hasWriteObject() ? SC_WRITE_METHOD : 0));
        } else {
            return new NoDataDescriptor(clazz, null);
        }
    }

    private Object replaceOrReturn(boolean unshared, Object object) {
        final int idx = instanceCache.size();
        instanceCache.add(UNSHARED);
        return replaceOrReturn(unshared, object, idx);
    }

    private Object replaceOrReturn(boolean unshared, Object object, int idx) {
        Object toReturn = object;
        final Object replacement = objectResolver.readResolve(object);
        if (replacement != null && replacement != object) {
            toReturn = replacement;
        }
        if (unshared) {
            instanceCache.set(idx, null);
        } else {
            instanceCache.set(idx, toReturn);
        }
        return toReturn;
    }
}
