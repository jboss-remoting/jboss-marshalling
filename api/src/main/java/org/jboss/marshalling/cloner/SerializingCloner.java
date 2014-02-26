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

package org.jboss.marshalling.cloner;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.NotSerializableException;
import java.io.ObjectInputValidation;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import org.jboss.marshalling.AbstractObjectInput;
import org.jboss.marshalling.AbstractObjectOutput;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerObjectInputStream;
import org.jboss.marshalling.MarshallerObjectOutputStream;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.SerializabilityChecker;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.util.BooleanReadField;
import org.jboss.marshalling.util.ByteReadField;
import org.jboss.marshalling.util.CharReadField;
import org.jboss.marshalling.util.DoubleReadField;
import org.jboss.marshalling.util.FloatReadField;
import org.jboss.marshalling.util.IdentityIntMap;
import org.jboss.marshalling.util.IntReadField;
import org.jboss.marshalling.util.Kind;
import org.jboss.marshalling.util.LongReadField;
import org.jboss.marshalling.util.ObjectReadField;
import org.jboss.marshalling.util.ReadField;
import org.jboss.marshalling.util.ShortReadField;

/**
 * An object cloner which uses serialization methods to clone objects.
 */
class SerializingCloner implements ObjectCloner {
    private final CloneTable delegate;
    private final ObjectResolver objectResolver;
    private final ObjectResolver objectPreResolver;
    private final ClassCloner classCloner;
    private final SerializabilityChecker serializabilityChecker;
    private final int bufferSize;

    private final SerializableClassRegistry registry;

    /**
     * Create a new instance.
     *
     * @param configuration the configuration to use
     */
    SerializingCloner(final ClonerConfiguration configuration) {
        final CloneTable delegate = configuration.getCloneTable();
        this.delegate = delegate == null ? CloneTable.NULL : delegate;
        final ObjectResolver objectResolver = configuration.getObjectResolver();
        this.objectResolver = objectResolver == null ? Marshalling.nullObjectResolver() : objectResolver;
        final ObjectResolver objectPreResolver = configuration.getObjectPreResolver();
        this.objectPreResolver = objectPreResolver == null ? Marshalling.nullObjectResolver() : objectPreResolver;
        final ClassCloner classCloner = configuration.getClassCloner();
        this.classCloner = classCloner == null ? ClassCloner.IDENTITY : classCloner;
        final SerializabilityChecker serializabilityChecker = configuration.getSerializabilityChecker();
        this.serializabilityChecker = serializabilityChecker == null ? SerializabilityChecker.DEFAULT : serializabilityChecker;
        final int bufferSize = configuration.getBufferSize();
        this.bufferSize = bufferSize < 1 ? 8192 : bufferSize;

        registry = SerializableClassRegistry.getInstance();
    }

    private final IdentityHashMap<Object, Object> clones = new IdentityHashMap<Object, Object>();

    public void reset() {
        synchronized (this) {
            clones.clear();
        }
    }

    public Object clone(final Object orig) throws IOException, ClassNotFoundException {
        synchronized (this) {
            boolean ok = false;
            try {
                final Object clone = clone(orig, true);
                ok = true;
                return clone;
            } finally {
                if (! ok) {
                    reset();
                }
            }
        }
    }

    private Object clone(final Object orig, final boolean replace) throws IOException, ClassNotFoundException {
        if (orig == null) {
            return null;
        }
        final IdentityHashMap<Object, Object> clones = this.clones;
        Object cached = clones.get(orig);
        if (cached != null) {
            return cached;
        }
        Object replaced = orig;
        replaced = objectPreResolver.writeReplace(replaced);
        final ClassCloner classCloner = this.classCloner;
        if (replaced instanceof Class) {
            final Class<?> classObj = (Class<?>) replaced;
            final Class<?> clonedClass = Proxy.isProxyClass(classObj) ? classCloner.cloneProxy(classObj) : classCloner.clone(classObj);
            clones.put(replaced, clonedClass);
            return clonedClass;
        }
        if ((cached = delegate.clone(replaced, this, classCloner)) != null) {
            clones.put(replaced, cached);
            return cached;
        }
        final Class<? extends Object> objClass = replaced.getClass();
        final SerializableClass info = registry.lookup(objClass);
        if (replace) {
            if (info.hasWriteReplace()) {
                replaced = info.callWriteReplace(replaced);
            }
            replaced = objectResolver.writeReplace(replaced);
            if (replaced != orig) {
                Object clone = clone(replaced, false);
                clones.put(orig, clone);
                return clone;
            }
        }
        if (orig instanceof Enum) {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum> cloneClass = ((Class<?>)clone(objClass)).asSubclass(Enum.class);

            if (cloneClass == objClass) {
                // same class means same enum constants
                return orig;
            } else {
                @SuppressWarnings("unchecked")
                final Class<? extends Enum> cloneEnumClass;
                //the actual object class may be a sub class of the enum class
                final Class<?> enumClass = ((Enum<?>) orig).getDeclaringClass();
                if(enumClass == objClass) {
                    cloneEnumClass = cloneClass;
                } else{
                    cloneEnumClass = ((Class<?>)clone(enumClass)).asSubclass(Enum.class);
                }
                return Enum.valueOf(cloneEnumClass, ((Enum<?>) orig).name());
            }
        }
        final Class<?> clonedClass = (Class<?>) clone(objClass);
        if (Proxy.isProxyClass(objClass)) {
            return Proxy.newProxyInstance(clonedClass.getClassLoader(), clonedClass.getInterfaces(), (InvocationHandler) clone(getInvocationHandler(orig)));
        }
        if (UNCLONED.contains(objClass)) {
            return orig;
        }
        final boolean sameClass = objClass == clonedClass;
        if (objClass.isArray()) {
            Object simpleClone = simpleClone(orig, objClass);
            if (simpleClone != null) return simpleClone;
            // must be an object array
            final Object[] origArray = (Object[]) orig;
            final int len = origArray.length;
            if (sameClass && len == 0) {
                clones.put(orig, orig);
                return orig;
            }
            if (UNCLONED.contains(objClass.getComponentType())) {
                final Object[] clone = origArray.clone();
                clones.put(orig, clone);
                return clone;
            }
            final Object[] clone;
            if (sameClass) {
                clone = origArray.clone();
            } else {
                clone = (Object[])Array.newInstance(clonedClass.getComponentType(), len);
            }
            // deep clone
            clones.put(orig, clone);
            for (int i = 0; i < len; i++) {
                clone[i] = clone(origArray[i]);
            }
            return clone;
        }
        final SerializableClass cloneInfo = sameClass ? info : registry.lookup(clonedClass);
        // Now check the serializable types
        final Object clone;
        if (orig instanceof Externalizable) {
            final Externalizable externalizable = (Externalizable) orig;
            clone = cloneInfo.callNoArgConstructor();
            clones.put(orig, clone);
            final Queue<Step> steps = new ArrayDeque<Step>();
            final StepObjectOutput soo = new StepObjectOutput(steps);
            externalizable.writeExternal(soo);
            soo.doFinish();
            ((Externalizable) clone).readExternal(new StepObjectInput(steps));
        } else if (serializabilityChecker.isSerializable(objClass)) {
            clone = cloneInfo.callNonInitConstructor();
            if (! (serializabilityChecker.isSerializable(clonedClass))) {
                throw new NotSerializableException(clonedClass.getName());
            }
            clones.put(orig, clone);
            initSerializableClone(orig, info, clone, cloneInfo);
        } else {
            throw new NotSerializableException(objClass.getName());
        }
        replaced = clone;
        if (cloneInfo.hasReadResolve()) {
            replaced = cloneInfo.callReadResolve(replaced);
        }
        replaced = objectPreResolver.readResolve(objectResolver.readResolve(replaced));
        if (replaced != clone) clones.put(orig, replaced);
        return replaced;
    }

    private void initSerializableClone(final Object orig, final SerializableClass origInfo, final Object clone, final SerializableClass cloneInfo) throws IOException, ClassNotFoundException {

        final Class<?> cloneClass = cloneInfo.getSubjectClass();
        if (! serializabilityChecker.isSerializable(cloneClass)) {
            throw new NotSerializableException(cloneClass.getName());
        }
        final Class<?> cloneSuperClass = cloneClass.getSuperclass();
        final Class<?> origClass = origInfo.getSubjectClass();
        if (cloneClass != clone(origClass)) {
            // try superclass first, then fill in "no data"
            initSerializableClone(orig, origInfo, clone, cloneInfo);
            if (cloneInfo.hasReadObjectNoData()) {
                cloneInfo.callReadObjectNoData(clone);
            }
            return;
        }
        // first, init the serializable superclass, if any
        final Class<?> origSuperClass = origClass.getSuperclass();
        if (serializabilityChecker.isSerializable(origSuperClass) || serializabilityChecker.isSerializable(cloneSuperClass)) {
            initSerializableClone(orig, registry.lookup(origSuperClass), clone, registry.lookup(cloneSuperClass));
        }
        if (! serializabilityChecker.isSerializable(origClass)) {
            if (cloneInfo.hasReadObjectNoData()) {
                cloneInfo.callReadObjectNoData(clone);
            }
            return;
        }
        final ClonerPutField fields = new ClonerPutField();
        fields.defineFields(origInfo);
        if (origInfo.hasWriteObject()) {
            final Queue<Step> steps = new ArrayDeque<Step>();
            final StepObjectOutputStream stepObjectOutputStream = new StepObjectOutputStream(steps, fields, orig);
            origInfo.callWriteObject(orig, stepObjectOutputStream);
            stepObjectOutputStream.flush();
            stepObjectOutputStream.doFinish();
            cloneFields(fields);
            if (cloneInfo.hasReadObject()) {
                cloneInfo.callReadObject(clone, new StepObjectInputStream(steps, fields, clone, cloneInfo));
            } else {
                storeFields(cloneInfo, clone, fields);
            }
        } else {
            prepareFields(orig, fields);
            cloneFields(fields);
            if (cloneInfo.hasReadObject()) {
                cloneInfo.callReadObject(clone, new StepObjectInputStream(new ArrayDeque<Step>(), fields, clone, cloneInfo));
            } else {
                storeFields(cloneInfo, clone, fields);
            }
        }
    }

    private void prepareFields(final Object subject, final ClonerPutField fields) throws InvalidObjectException {
        final Map<String, SerializableField> defMap = fields.fieldDefMap;
        final Map<String, ReadField> map = fields.fieldMap;
        try {
            for (String name : defMap.keySet()) {
                final SerializableField serializableField = defMap.get(name);
                final Field realField = serializableField.getField();
                if (realField != null) switch (serializableField.getKind()) {
                    case BOOLEAN: map.put(name, new BooleanReadField(serializableField, realField.getBoolean(subject))); continue;
                    case BYTE:    map.put(name, new ByteReadField(serializableField, realField.getByte(subject))); continue;
                    case CHAR:    map.put(name, new CharReadField(serializableField, realField.getChar(subject))); continue;
                    case DOUBLE:  map.put(name, new DoubleReadField(serializableField, realField.getDouble(subject))); continue;
                    case FLOAT:   map.put(name, new FloatReadField(serializableField, realField.getFloat(subject))); continue;
                    case INT:     map.put(name, new IntReadField(serializableField, realField.getInt(subject))); continue;
                    case LONG:    map.put(name, new LongReadField(serializableField, realField.getLong(subject))); continue;
                    case OBJECT:  map.put(name, new ObjectReadField(serializableField, realField.get(subject))); continue;
                    case SHORT:   map.put(name, new ShortReadField(serializableField, realField.getShort(subject))); continue;
                    default: throw new IllegalStateException();
                }
            }
        } catch (IllegalAccessException e) {
            throw new InvalidObjectException("Cannot access write field: " + e);
        }
    }

    private void cloneFields(final ClonerPutField fields) throws IOException, ClassNotFoundException {
        final Map<String, SerializableField> defMap = fields.fieldDefMap;
        final Map<String, ReadField> map = fields.fieldMap;
        for (String name : defMap.keySet()) {
            final SerializableField field = defMap.get(name);
            if (field.getKind() == Kind.OBJECT) {
                map.put(name, new ObjectReadField(field, clone(map.get(name).getObject())));
                continue;
            }
        }
    }

    private void storeFields(final SerializableClass cloneInfo, final Object clone, final ClonerPutField fields) throws IOException {
        final Map<String, ReadField> map = fields.fieldMap;
        try {
            for (SerializableField cloneField : cloneInfo.getFields()) {
                final String name = cloneField.getName();
                final ReadField field = map.get(name);
                final Field realField = cloneField.getField();
                if (realField != null) switch (cloneField.getKind()) {
                    case BOOLEAN: realField.setBoolean(clone, field == null ? false : field.getBoolean()); continue;
                    case BYTE:    realField.setByte(clone, field == null ? 0 : field.getByte()); continue;
                    case CHAR:    realField.setChar(clone, field == null ? 0 : field.getChar()); continue;
                    case DOUBLE:  realField.setDouble(clone, field == null ? 0 : field.getDouble()); continue;
                    case FLOAT:   realField.setFloat(clone, field == null ? 0 : field.getFloat()); continue;
                    case INT:     realField.setInt(clone, field == null ? 0 : field.getInt()); continue;
                    case LONG:    realField.setLong(clone, field == null ? 0 : field.getLong()); continue;
                    case OBJECT:  realField.set(clone, field == null ? null : field.getObject()); continue;
                    case SHORT:   realField.setShort(clone, field == null ? 0 : field.getShort()); continue;
                    default: throw new IllegalStateException();
                }
            }
        } catch (IllegalAccessException e) {
            throw new InvalidObjectException("Cannot access write field: " + e);
        }
    }

    private static Object simpleClone(final Object orig, final Class<? extends Object> objClass) {
        final int idx = PRIMITIVE_ARRAYS.get(objClass, -1);
        switch (idx) {
            case 0: {
                final boolean[] booleans = (boolean[]) orig;
                return booleans.length == 0 ? orig : booleans.clone();
            }
            case 1: {
                final byte[] bytes = (byte[]) orig;
                return bytes.length == 0 ? orig : bytes.clone();
            }
            case 2: {
                final short[] shorts = (short[]) orig;
                return shorts.length == 0 ? orig : shorts.clone();
            }
            case 3: {
                final int[] ints = (int[]) orig;
                return ints.length == 0 ? orig : ints.clone();
            }
            case 4: {
                final long[] longs = (long[]) orig;
                return longs.length == 0 ? orig : longs.clone();
            }
            case 5: {
                final float[] floats = (float[]) orig;
                return floats.length == 0 ? orig : floats.clone();
            }
            case 6: {
                final double[] doubles = (double[]) orig;
                return doubles.length == 0 ? orig : doubles.clone();
            }
            case 7: {
                final char[] chars = (char[]) orig;
                return chars.length == 0 ? orig : chars.clone();
            }
            default: return null; // fall out
        }
    }

    private static InvocationHandler getInvocationHandler(final Object orig) {
        try {
            return (InvocationHandler) proxyInvocationHandler.get(orig);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    private abstract static class Step {

    }

    private static final Set<Class<?>> UNCLONED;
    private static final IdentityIntMap<Class<?>> PRIMITIVE_ARRAYS;

    private static final Field proxyInvocationHandler;

    static {
        final Set<Class<?>> set = new HashSet<Class<?>>();
        // List of final, immutable, serializable JDK classes with no external references to mutable items
        // These items can be sent back by reference and the caller will never know
        set.add(Boolean.class);
        set.add(Byte.class);
        set.add(Short.class);
        set.add(Integer.class);
        set.add(Long.class);
        set.add(Float.class);
        set.add(Double.class);
        set.add(Character.class);
        set.add(String.class);
        set.add(StackTraceElement.class);
        set.add(BigInteger.class);
        set.add(BigDecimal.class);
        set.add(Pattern.class);
        set.add(File.class);
        set.add(Collections.emptyList().getClass());
        set.add(Collections.emptySet().getClass());
        set.add(Collections.emptyMap().getClass());
        UNCLONED = set;
        final IdentityIntMap<Class<?>> map = new IdentityIntMap<Class<?>>();
        // List of cloneable, non-extensible, serializable JDK classes with no external references to mutable items
        // These items can be deeply cloned by a single method call, without worrying about the classloader
        map.put(boolean[].class, 0);
        map.put(byte[].class, 1);
        map.put(short[].class, 2);
        map.put(int[].class, 3);
        map.put(long[].class, 4);
        map.put(float[].class, 5);
        map.put(double[].class, 6);
        map.put(char[].class, 7);
        PRIMITIVE_ARRAYS = map;
        proxyInvocationHandler = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    final Field field = Proxy.class.getDeclaredField("h");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new NoSuchFieldError(e.getMessage());
                }
            }
        });
    }

    class StepObjectOutput extends AbstractObjectOutput implements Marshaller {

        private final Queue<Step> steps;
        private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        StepObjectOutput(final Queue<Step> steps) throws IOException {
            super(SerializingCloner.this.bufferSize);
            this.steps = steps;
            super.start(Marshalling.createByteOutput(byteArrayOutputStream));
        }

        protected void doWriteObject(final Object obj, final boolean unshared) throws IOException {
            super.flush();
            final ByteArrayOutputStream baos = byteArrayOutputStream;
            if (baos.size() > 0) {
                steps.add(new ByteDataStep(baos.toByteArray()));
                baos.reset();
            }
            steps.add(new CloneStep(obj));
        }

        public void clearInstanceCache() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void clearClassCache() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void start(final ByteOutput byteOutput) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void finish() throws IOException {
            throw new UnsupportedOperationException();
        }

        void doFinish() throws IOException {
            super.finish();
        }

        public void flush() throws IOException {
            super.flush();
            final ByteArrayOutputStream baos = byteArrayOutputStream;
            if (baos.size() > 0) {
                steps.add(new ByteDataStep(baos.toByteArray()));
                baos.reset();
            }
        }
    }

    class StepObjectOutputStream extends MarshallerObjectOutputStream {

        private final Queue<Step> steps;
        private final ClonerPutField clonerPutField;
        private final Object subject;
        private final StepObjectOutput output;

        private StepObjectOutputStream(StepObjectOutput output, final Queue<Step> steps, final ClonerPutField clonerPutField, final Object subject) throws IOException {
            super(output);
            this.output = output;
            this.steps = steps;
            this.clonerPutField = clonerPutField;
            this.subject = subject;
        }

        StepObjectOutputStream(final Queue<Step> steps, final ClonerPutField clonerPutField, final Object subject) throws IOException {
            this(new StepObjectOutput(steps), steps, clonerPutField, subject);
        }

        public void writeFields() throws IOException {
            if (! steps.isEmpty()) {
                throw new IllegalStateException("writeFields may not be called in this context");
            }
        }

        public PutField putFields() throws IOException {
            if (! steps.isEmpty()) {
                throw new IllegalStateException("putFields may not be called in this context");
            }
            return clonerPutField;
        }

        public void defaultWriteObject() throws IOException {
            if (! steps.isEmpty()) {
                throw new IllegalStateException("defaultWriteObject may not be called in this context");
            }
            final Object subject = this.subject;
            final SerializingCloner.ClonerPutField fields = clonerPutField;
            prepareFields(subject, fields);
        }

        void doFinish() throws IOException {
            output.doFinish();
        }
    }

    class StepObjectInputStream extends MarshallerObjectInputStream {

        private final ClonerPutField clonerPutField;
        private final Object clone;
        private final SerializableClass cloneInfo;

        StepObjectInputStream(final Queue<Step> steps, final ClonerPutField clonerPutField, final Object clone, final SerializableClass cloneInfo) throws IOException {
            super(new StepObjectInput(steps));
            this.clonerPutField = clonerPutField;
            this.clone = clone;
            this.cloneInfo = cloneInfo;
        }

        public void defaultReadObject() throws IOException, ClassNotFoundException {
            storeFields(cloneInfo, clone, clonerPutField);
        }

        public GetField readFields() throws IOException, ClassNotFoundException {
            return new GetField() {
                public ObjectStreamClass getObjectStreamClass() {
                    throw new UnsupportedOperationException();
                }

                public boolean defaulted(final String name) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted();
                }

                public boolean get(final String name, final boolean val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getBoolean();
                }

                public byte get(final String name, final byte val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getByte();
                }

                public char get(final String name, final char val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getChar();
                }

                public short get(final String name, final short val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getShort();
                }

                public int get(final String name, final int val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getInt();
                }

                public long get(final String name, final long val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getLong();
                }

                public float get(final String name, final float val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getFloat();
                }

                public double get(final String name, final double val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getDouble();
                }

                public Object get(final String name, final Object val) throws IOException {
                    final ReadField field = clonerPutField.fieldMap.get(name);
                    return field == null || field.isDefaulted() ? val : field.getObject();
                }
            };
        }

        public void registerValidation(final ObjectInputValidation obj, final int priority) throws NotActiveException, InvalidObjectException {
        }
    }

    class ClonerPutField extends ObjectOutputStream.PutField {
        private final Map<String, SerializableField> fieldDefMap = new HashMap<String, SerializableField>();
        private final Map<String, ReadField> fieldMap = new HashMap<String, ReadField>();

        private SerializableField getField(final String name, final Kind kind) {
            final SerializableField field = fieldDefMap.get(name);
            if (field == null) {
                throw new IllegalArgumentException("No field named '" + name + "' could be found");
            }
            if (field.getKind() != kind) {
                throw new IllegalArgumentException("Field '" + name + "' is the wrong type (expected " + kind + ", got " + field.getKind() + ")");
            }
            return field;
        }

        private void defineFields(final SerializableClass clazz) {
            for (SerializableField field : clazz.getFields()) {
                fieldDefMap.put(field.getName(), field);
            }
        }

        public void put(final String name, final boolean val) {
            fieldMap.put(name, new BooleanReadField(getField(name, Kind.BOOLEAN), val));
        }

        public void put(final String name, final byte val) {
            fieldMap.put(name, new ByteReadField(getField(name, Kind.BYTE), val));
        }

        public void put(final String name, final char val) {
            fieldMap.put(name, new CharReadField(getField(name, Kind.CHAR), val));
        }

        public void put(final String name, final short val) {
            fieldMap.put(name, new ShortReadField(getField(name, Kind.SHORT), val));
        }

        public void put(final String name, final int val) {
            fieldMap.put(name, new IntReadField(getField(name, Kind.INT), val));
        }

        public void put(final String name, final long val) {
            fieldMap.put(name, new LongReadField(getField(name, Kind.LONG), val));
        }

        public void put(final String name, final float val) {
            fieldMap.put(name, new FloatReadField(getField(name, Kind.FLOAT), val));
        }

        public void put(final String name, final double val) {
            fieldMap.put(name, new DoubleReadField(getField(name, Kind.DOUBLE), val));
        }

        public void put(final String name, final Object val) {
            fieldMap.put(name, new ObjectReadField(getField(name, Kind.OBJECT), val));
        }

        @Deprecated
        public void write(final ObjectOutput out) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    class StepObjectInput extends AbstractObjectInput implements Unmarshaller {

        private final Queue<Step> steps;
        private Step current;
        private int idx;

        StepObjectInput(final Queue<Step> steps) throws IOException {
            super(bufferSize);
            this.steps = steps;
            current = steps.poll();
            super.start(new ByteInput() {

                public int read() throws IOException {
                    while (current != null) {
                        if (current instanceof ByteDataStep) {
                            final ByteDataStep step = (ByteDataStep) current;
                            final byte[] bytes = step.getBytes();
                            if (idx == bytes.length) {
                                current = steps.poll();
                                idx = 0;
                            } else {
                                final byte b = bytes[idx++];
                                return b & 0xff;
                            }
                        } else {
                            // an object is pending
                            return -1;
                        }
                    }
                    return -1;
                }

                public int read(final byte[] b) throws IOException {
                    return read(b, 0, b.length);
                }

                public int read(final byte[] b, int off, int len) throws IOException {
                    if (len == 0) return 0;
                    int t = 0;
                    while (current != null && len > 0) {
                        if (current instanceof ByteDataStep) {
                            final ByteDataStep step = (ByteDataStep) current;
                            final byte[] bytes = step.getBytes();
                            final int blen = bytes.length;
                            if (idx == blen) {
                                current = steps.poll();
                                idx = 0;
                            } else {
                                final int c = Math.min(blen - idx, len);
                                System.arraycopy(bytes, idx, b, off, c);
                                idx += c;
                                off += c;
                                len -= c;
                                t += c;
                                if (idx == blen) {
                                    current = steps.poll();
                                    idx = 0;
                                }
                            }
                        } else {
                            // an object is pending
                            return t == 0 ? -1 : t;
                        }
                    }
                    return t == 0 ? -1 : t;
                }

                public int available() throws IOException {
                    return current instanceof ByteDataStep ? ((ByteDataStep) current).getBytes().length - idx : 0;
                }

                public long skip(long n) throws IOException {
                    long t = 0;
                    while (current != null && n > 0) {
                        if (current instanceof ByteDataStep) {
                            final ByteDataStep step = (ByteDataStep) current;
                            final byte[] bytes = step.getBytes();
                            final int blen = bytes.length;
                            if (idx == blen) {
                                current = steps.poll();
                                idx = 0;
                            } else {
                                final int c = (int) Math.min((long) blen - idx, n);
                                idx += c;
                                n -= c;
                                if (idx == blen) {
                                    current = steps.poll();
                                    idx = 0;
                                }
                            }
                        } else {
                            // an object is pending
                            return t;
                        }
                    }
                    return t;
                }

                public void close() throws IOException {
                    current = null;
                }
            });
        }

        protected Object doReadObject(final boolean unshared) throws ClassNotFoundException, IOException {
            Step step = current;
            while (step instanceof ByteDataStep) {
                step = steps.poll();
            }
            if (step == null) {
                current = null;
                throw new EOFException();
            }
            current = steps.poll();
            // not really true, just IDEA being silly again
            @SuppressWarnings("UnnecessaryThis")
            final Object clone = SerializingCloner.this.clone(((CloneStep) step).getOrig());
            return clone;
        }

        public void finish() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void start(final ByteInput byteInput) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void clearInstanceCache() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void clearClassCache() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    private static final class ByteDataStep extends Step {
        private final byte[] bytes;

        private ByteDataStep(final byte[] bytes) {
            this.bytes = bytes;
        }

        byte[] getBytes() {
            return bytes;
        }
    }

    private static final class CloneStep extends Step {
        private final Object orig;

        private CloneStep(final Object orig) {
            this.orig = orig;
        }

        Object getOrig() {
            return orig;
        }
    }
}
