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
import java.io.ObjectStreamConstants;
import java.io.StreamCorruptedException;
import java.io.ObjectStreamClass;
import java.io.InvalidClassException;
import java.io.WriteAbortedException;
import java.io.InvalidObjectException;
import java.io.Externalizable;
import java.io.NotSerializableException;
import java.io.ObjectInputValidation;
import java.io.Serializable;
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
public final class SerialUnmarshaller extends AbstractUnmarshaller implements Unmarshaller, ObjectStreamConstants {

    private static final Object UNSHARED = new Object();
    private static final Object UNRESOLVED = new Object();

    private final ArrayList<Object> instanceCache;
    private final SerializableClassRegistry registry;

    private int depth = 0;

    private SerialObjectInputStream ois;
    private BlockUnmarshaller blockUnmarshaller;

    SerialUnmarshaller(final SerialMarshallerFactory factory, final SerializableClassRegistry registry, final MarshallingConfiguration configuration) {
        super(factory, configuration);
        this.registry = registry;
        instanceCache = new ArrayList<Object>(configuration.getInstanceCount());
    }

    protected Object doReadObject(final boolean unshared) throws ClassNotFoundException, IOException {
        final Object obj = doReadObject(readUnsignedByte(), unshared);
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
        if (idx < 0 || idx > instanceCache.size()) {
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
                    instanceCache.add(unshared ? UNSHARED : str);
                    return str;
                }

                case TC_LONGSTRING: {
                    final long len = super.readLong();
                    final String str = UTFUtils.readUTFBytesByByteCount(this, len);
                    instanceCache.add(unshared ? UNSHARED : str);
                    return str;
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
                            instanceCache.set(idx, bytes);
                            return bytes;
                        } else if (ct == short.class) {
                            final short[] shorts = new short[size];
                            for (int i = 0; i < shorts.length; i++) {
                                shorts[i] = readShort();
                            }
                            instanceCache.set(idx, shorts);
                            return shorts;
                        } else if (ct == int.class) {
                            final int[] ints = new int[size];
                            for (int i = 0; i < ints.length; i++) {
                                ints[i] = readInt();
                            }
                            instanceCache.set(idx, ints);
                            return ints;
                        } else if (ct == long.class) {
                            final long[] longs = new long[size];
                            for (int i = 0; i < longs.length; i++) {
                                longs[i] = readLong();
                            }
                            instanceCache.set(idx, longs);
                            return longs;
                        } else if (ct == float.class) {
                            final float[] floats = new float[size];
                            for (int i = 0; i < floats.length; i++) {
                                floats[i] = readFloat();
                            }
                            instanceCache.set(idx, floats);
                            return floats;
                        } else if (ct == double.class) {
                            final double[] doubles = new double[size];
                            for (int i = 0; i < doubles.length; i++) {
                                doubles[i] = readDouble();
                            }
                            instanceCache.set(idx, doubles);
                            return doubles;
                        } else if (ct == boolean.class) {
                            final boolean[] booleans = new boolean[size];
                            for (int i = 0; i < booleans.length; i++) {
                                booleans[i] = readBoolean();
                            }
                            instanceCache.set(idx, booleans);
                            return booleans;
                        } else if (ct == char.class) {
                            final char[] chars = new char[size];
                            for (int i = 0; i < chars.length; i++) {
                                chars[i] = readChar();
                            }
                            instanceCache.set(idx, chars);
                            return chars;
                        } else {
                            throw new InvalidClassException(type.getName(), "Invalid component type");
                        }
                    } else {
                        final Object[] objects = (Object[]) Array.newInstance(ct, size);
                        instanceCache.set(idx, objects);
                        for (int i = 0; i < objects.length; i++) {
                            objects[i] = doReadObject(false);
                        }
                        return objects;
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
                    final int h = instanceCache.size();
                    instanceCache.add(UNRESOLVED);
                    final String constName = (String) doReadObject(false);
                    final Enum obj = Enum.valueOf(enumType, constName);
                    instanceCache.set(h, obj);
                    return obj;
                }

                case TC_OBJECT: {
                    final Descriptor descriptor = readNonNullClassDescriptor();
                    if ((descriptor.getFlags() & (SC_SERIALIZABLE | SC_EXTERNALIZABLE)) == 0) {
                        throw new NotSerializableException(descriptor.getClass().getName());
                    }
                    final Object obj = creator.create(descriptor.getType());
                    instanceCache.add(unshared ? UNSHARED : obj);
                    if ((descriptor.getFlags() & SC_EXTERNALIZABLE) != 0) {
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
                    } else if (obj instanceof Externalizable) {
                        throw new InvalidObjectException("Created object should not be Externalizable but it is");
                    } else {
                        doReadSerialObject(descriptor, obj);
                    }
                    return obj;
                }

                case TC_EXCEPTION: {
                    clearInstanceCache();
                    final IOException ex = (IOException) doReadObject(false);
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
        descriptor.readSerial(this, registry.lookup(descriptor.getType()), obj);
    }

    private static InvalidClassException objectStreamClassException() {
        return new InvalidClassException(ObjectStreamClass.class.getName(), "Cannot read ObjectStreamClass instances");
    }

    private Descriptor readNonNullClassDescriptor() throws ClassNotFoundException, IOException {
        final Descriptor desc = readClassDescriptor();
        if (desc == null) {
            throw new StreamCorruptedException("Unexpected null class descriptor");
        }
        return desc;
    }

    private Descriptor readClassDescriptor() throws ClassNotFoundException, IOException {
        return readClassDescriptor(readUnsignedByte());
    }

    private Descriptor readClassDescriptor(int leadByte) throws IOException, ClassNotFoundException {
        switch (leadByte) {
            case TC_CLASSDESC: {
                final String className = readUTF();
                final long svu = readLong();
                final int idx = instanceCache.size();
                instanceCache.add(null);
                final int descFlags = readUnsignedByte();
                final int fieldCount = readUnsignedShort();
                final int[] typecodes = new int[fieldCount];
                final String[] names = new String[fieldCount];
                final String[] fieldSignatures = new String[fieldCount];
                for (int i = 0; i < fieldCount; i ++) {
                    typecodes[i] = readUnsignedByte();
                    names[i] = readUTF();
                    if (typecodes[i] == '[' || typecodes[i] == 'L') {
                        fieldSignatures[i] = doReadString();
                    }
                }
                final Class<?> clazz = classResolver.resolveClass(blockUnmarshaller, className, svu);
                blockUnmarshaller.readToEndBlockData();
                final SerializableClass sc = registry.lookup(clazz);
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
                    fields[i] = sc.getSerializableField(names[i], fieldType, false);
                }
                final Descriptor superDescr = readClassDescriptor();
                final Class<?> superClazz = clazz.getSuperclass();
                final Descriptor descriptor;
                if (superDescr == null || superDescr.getType().isAssignableFrom(superClazz)) {
                    descriptor = descFlags == 0 ? new NoDataDescriptor(clazz, bridge(superDescr, superClazz)) : new PlainDescriptor(clazz, bridge(superDescr, superClazz), fields, descFlags);
                } else {
                    throw new InvalidClassException(clazz.getName(), "Class hierarchy mismatch");
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
                final Descriptor superDescr = readClassDescriptor();
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
            default:
                throw badLeadByte(leadByte);
        }
    }

    private Descriptor bridge(final Descriptor descriptor, final Class<?> type) {
        final Class<?> superDescrClazz = descriptor == null ? null : descriptor.getType();
        final Class<?> typeSuperclass = type.getSuperclass();
        if (type == superDescrClazz || descriptor == null && (typeSuperclass == null || Serializable.class.isAssignableFrom(typeSuperclass))) {
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
}
