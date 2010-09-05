/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling.cloner;

import java.io.ByteArrayInputStream;
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
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.Arrays;
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
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.PublicReflectiveCreator;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
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
    private final ClassCloner classCloner;
    private final Creator externalizedCreator;
    private final Creator serializedCreator;
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
        final ClassCloner classCloner = configuration.getClassCloner();
        this.classCloner = classCloner == null ? ClassCloner.IDENTITY : classCloner;
        final Creator externalizedCreator = configuration.getExternalizedCreator();
        this.externalizedCreator = externalizedCreator == null ? new PublicReflectiveCreator() : externalizedCreator;
        final Creator serializedCreator = configuration.getSerializedCreator();
        this.serializedCreator = serializedCreator == null ? new SunReflectiveCreator() : serializedCreator;
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
        final ClassCloner classCloner = this.classCloner;
        if (orig instanceof Class) {
            final Class classObj = (Class) orig;
            final Class<?> clonedClass = Proxy.isProxyClass(classObj) ? classCloner.cloneProxy(classObj) : classCloner.clone(classObj);
            clones.put(orig, clonedClass);
            return clonedClass;
        }
        if ((cached = delegate.clone(orig, this, classCloner)) != null) {
            clones.put(orig, cached);
            return cached;
        }
        final Class<? extends Object> objClass = orig.getClass();
        if (orig instanceof Enum) {
            final Class<? extends Enum> cloneClass = ((Class<?>)clone(objClass)).asSubclass(Enum.class);
            if (cloneClass == orig) {
                // same class means same enum constants
                return orig;
            } else {
                return Enum.valueOf(cloneClass, ((Enum) orig).name());
            }
        }
        final Class<?> clonedClass = (Class<?>) clone(objClass);
        if (Proxy.isProxyClass(objClass)) {
            return Proxy.newProxyInstance(clonedClass.getClassLoader(), clonedClass.getInterfaces(), (InvocationHandler) clone(getInvocationHandler(orig)));
        }
        if (UNCLONED.contains(objClass)) {
            return orig;
        }
        if (objClass.isArray()) {
            Object simpleClone = simpleClone(orig, objClass);
            if (simpleClone != null) return simpleClone;
            // must be an object array
            final Object[] origArray = (Object[]) orig;
            final int len = origArray.length;
            final boolean sameClass = objClass == clonedClass;
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
        final SerializableClass info = registry.lookup(objClass);
        if (replace) {
            Object replaced = orig;
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
        // Now check the serializable types
        final Object clone;
        if (orig instanceof Externalizable) {
            final Externalizable externalizable = (Externalizable) orig;
            clone = externalizedCreator.create((Class<?>) clone(objClass));
            clones.put(orig, clone);
            final Queue<Step> steps = new ArrayDeque<Step>();
            externalizable.writeExternal(new StepObjectOutput(steps));
            ((Externalizable) clone).readExternal(new StepObjectInput(steps));
        } else if (orig instanceof Serializable) {
            clone = serializedCreator.create((Class<?>) clone(objClass));
            final Class<?> cloneClass = clone.getClass();
            if (! (clone instanceof Serializable)) {
                throw new NotSerializableException(cloneClass.getName());
            }
            clones.put(orig, clone);
            initSerializableClone(orig, info, clone, cloneClass);
        } else {
            throw new NotSerializableException(objClass.getName());
        }
        Object replaced = clone;
        if (info.hasReadResolve()) {
            replaced = info.callReadResolve(replaced);
        }
        replaced = objectResolver.readResolve(replaced);
        if (replaced != clone) clones.put(orig, replaced);
        return replaced;
    }

    private void initSerializableClone(final Object orig, final SerializableClass info, final Object clone, final Class<?> cloneClass) throws IOException, ClassNotFoundException {

        final Class<?> objClass = info.getSubjectClass();
        if (! Serializable.class.isAssignableFrom(cloneClass)) {
            throw new NotSerializableException(cloneClass.getName());
        }
        final SerializableClass cloneInfo = registry.lookup(cloneClass);
        final Class<?> cloneSuperClass = cloneClass.getSuperclass();
        if (cloneClass != clone(objClass)) {
            // try superclass first, then fill in "no data"
            initSerializableClone(orig, info, clone, cloneSuperClass);
            if (cloneInfo.hasReadObjectNoData()) {
                cloneInfo.callReadObjectNoData(clone);
            }
            return;
        }
        // first, init the serializable superclass, if any
        final Class<?> superClass = objClass.getSuperclass();
        if (Serializable.class.isAssignableFrom(superClass) || Serializable.class.isAssignableFrom(cloneSuperClass)) {
            initSerializableClone(orig, registry.lookup(superClass), clone, cloneSuperClass);
        }
        if (! Serializable.class.isAssignableFrom(objClass)) {
            if (cloneInfo.hasReadObjectNoData()) {
                cloneInfo.callReadObjectNoData(clone);
            }
            return;
        }
        final ClonerPutField fields = new ClonerPutField();
        fields.defineFields(info);
        if (info.hasWriteObject()) {
            final Queue<Step> steps = new ArrayDeque<Step>();
            info.callWriteObject(orig, new StepObjectOutputStream(steps, fields, orig));
            if (cloneInfo.hasReadObject()) {
                cloneInfo.callReadObject(clone, new StepObjectInputStream(steps, fields, clone, cloneInfo));
            } else {
                cloneFields(fields);
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
                final SerializableField field = defMap.get(name);
                switch (field.getKind()) {
                    case BOOLEAN: map.put(name, new BooleanReadField(field, field.getField().getBoolean(subject))); continue;
                    case BYTE:    map.put(name, new ByteReadField(field, field.getField().getByte(subject))); continue;
                    case CHAR:    map.put(name, new CharReadField(field, field.getField().getChar(subject))); continue;
                    case DOUBLE:  map.put(name, new DoubleReadField(field, field.getField().getDouble(subject))); continue;
                    case FLOAT:   map.put(name, new FloatReadField(field, field.getField().getFloat(subject))); continue;
                    case INT:     map.put(name, new IntReadField(field, field.getField().getInt(subject))); continue;
                    case LONG:    map.put(name, new LongReadField(field, field.getField().getLong(subject))); continue;
                    case OBJECT:  map.put(name, new ObjectReadField(field, field.getField().get(subject))); continue;
                    case SHORT:   map.put(name, new ShortReadField(field, field.getField().getShort(subject))); continue;
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
                switch (cloneField.getKind()) {
                    case BOOLEAN: cloneField.getField().setBoolean(clone, field == null ? false : field.getBoolean()); continue;
                    case BYTE:    cloneField.getField().setByte(clone, field == null ? 0 : field.getByte()); continue;
                    case CHAR:    cloneField.getField().setChar(clone, field == null ? 0 : field.getChar()); continue;
                    case DOUBLE:  cloneField.getField().setDouble(clone, field == null ? 0 : field.getDouble()); continue;
                    case FLOAT:   cloneField.getField().setFloat(clone, field == null ? 0 : field.getFloat()); continue;
                    case INT:     cloneField.getField().setInt(clone, field == null ? 0 : field.getInt()); continue;
                    case LONG:    cloneField.getField().setLong(clone, field == null ? 0 : field.getLong()); continue;
                    case OBJECT:  cloneField.getField().set(clone, field == null ? null : field.getObject()); continue;
                    case SHORT:   cloneField.getField().setShort(clone, field == null ? 0 : field.getShort()); continue;
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

    private static abstract class Step {

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

    private class StepObjectOutput extends AbstractObjectOutput implements Marshaller {

        private final Queue<Step> steps;
        private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        private final ByteOutput byteOutput = Marshalling.createByteOutput(byteArrayOutputStream);

        public StepObjectOutput(final Queue<Step> steps) throws IOException {
            super(SerializingCloner.this.bufferSize);
            this.steps = steps;
            start(byteOutput);
        }

        protected void doWriteObject(final Object obj, final boolean unshared) throws IOException {
            final ByteArrayOutputStream baos = byteArrayOutputStream;
            if (baos.size() > 0) {
                steps.add(new ByteDataStep(baos.toByteArray()));
                baos.reset();
            }
            steps.add(new CloneStep(obj));
        }

        public void clearInstanceCache() throws IOException {
        }

        public void clearClassCache() throws IOException {
        }

        public void start(final ByteOutput byteOutput) throws IOException {
            super.start(byteOutput);
        }

        public void finish() throws IOException {
            super.finish();
        }

        public void flush() throws IOException {
            final ByteArrayOutputStream baos = byteArrayOutputStream;
            steps.add(new ByteDataStep(baos.toByteArray()));
            baos.reset();
        }
    }

    private class StepObjectOutputStream extends MarshallerObjectOutputStream {

        private final Queue<Step> steps;
        private final ClonerPutField clonerPutField;
        private final Object subject;

        protected StepObjectOutputStream(final Queue<Step> steps, final ClonerPutField clonerPutField, final Object subject) throws IOException {
            super(new StepObjectOutput(steps));
            this.steps = steps;
            this.clonerPutField = clonerPutField;
            this.subject = subject;
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
                throw new IllegalStateException("putFields may not be called in this context");
            }
            final Object subject = this.subject;
            final SerializingCloner.ClonerPutField fields = clonerPutField;
            prepareFields(subject, fields);
        }
    }

    private class StepObjectInputStream extends MarshallerObjectInputStream {

        private final ClonerPutField clonerPutField;
        private final Object clone;
        private final SerializableClass cloneInfo;

        public StepObjectInputStream(final Queue<Step> steps, final ClonerPutField clonerPutField, final Object clone, final SerializableClass cloneInfo) throws IOException {
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

        public void registerValidation(final ObjectInputValidation obj, final int prio) throws NotActiveException, InvalidObjectException {
        }
    }

    private class ClonerPutField extends ObjectOutputStream.PutField {
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

        public void write(final ObjectOutput out) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    private class StepObjectInput extends AbstractObjectInput implements Unmarshaller {

        private final Queue<Step> steps;

        public StepObjectInput(final Queue<Step> steps) throws IOException {
            super(bufferSize);
            this.steps = steps;
            if (steps.peek() instanceof ByteDataStep) {
                final ByteDataStep step = (ByteDataStep) steps.poll();
                start(Marshalling.createByteInput(new ByteArrayInputStream(step.getBytes())));
            }
        }

        protected Object doReadObject(final boolean unshared) throws ClassNotFoundException, IOException {
            finish();
            Step step;
            do {
                step = steps.poll();
            } while (step instanceof ByteDataStep);
            if (step == null) {
                throw new EOFException();
            }
            final Object clone = SerializingCloner.this.clone(((CloneStep) step).getOrig());
            step = steps.peek();
            if (step instanceof ByteDataStep) {
                start(Marshalling.createByteInput(new ByteArrayInputStream(((ByteDataStep) steps.poll()).getBytes())));
            }
            return clone;
        }

        public void finish() throws IOException {
            super.finish();
        }

        public void start(final ByteInput byteInput) throws IOException {
            super.start(byteInput);
        }

        public void clearInstanceCache() throws IOException {
        }

        public void clearClassCache() throws IOException {
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
