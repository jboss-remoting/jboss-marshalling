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

package org.jboss.marshalling.reflect;

import java.io.ObjectInput;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.ObjectStreamClass;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import sun.reflect.ReflectionFactory;

/**
 * Reflection information about a serializable class.  Intended for use by implementations of the Marshalling API.
 */
public final class SerializableClass {
    private static final ReflectionFactory reflectionFactory = (ReflectionFactory) AccessController.doPrivileged(new ReflectionFactory.GetReflectionFactoryAction());

    private final Class<?> subject;
    private final Method writeObject;
    private final Method writeReplace;
    private final Method readObject;
    private final Method readObjectNoData;
    private final Method readResolve;
    private final Constructor<?> noArgConstructor;
    private final Constructor<?> objectInputConstructor;
    private final Constructor<?> nonInitConstructor;
    private final SerializableField[] fields;
    private final Map<String, SerializableField> fieldsByName;
    private final long effectiveSerialVersionUID;

    private static final Comparator<? super SerializableField> NAME_COMPARATOR = new Comparator<SerializableField>() {
        public int compare(final SerializableField o1, final SerializableField o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    /**
     * An empty array of fields.
     */
    public static final SerializableField[] NOFIELDS = new SerializableField[0];

    SerializableClass(Class<?> subject) {
        this.subject = subject;
        // private methods
        Method writeObject = null;
        Method readObject = null;
        Method readObjectNoData = null;
        Method writeReplace = null;
        Method readResolve = null;
        for (Method method : subject.getDeclaredMethods()) {
            final int modifiers = method.getModifiers();
            final String methodName = method.getName();
            final Class<?> methodReturnType = method.getReturnType();
            if (! Modifier.isStatic(modifiers)) {
                if (Modifier.isPrivate(modifiers) && methodReturnType == void.class) {
                    if (methodName.equals("writeObject")) {
                        final Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1 && parameterTypes[0] == ObjectOutputStream.class) {
                            writeObject = method;
                            writeObject.setAccessible(true);
                        }
                    } else if (methodName.equals("readObject")) {
                        final Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1 && parameterTypes[0] == ObjectInputStream.class) {
                            readObject = method;
                            readObject.setAccessible(true);
                        }
                    } else if (methodName.equals("readObjectNoData")) {
                        final Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 0) {
                            readObjectNoData = method;
                            readObjectNoData.setAccessible(true);
                        }
                    }
                } else if (methodReturnType == Object.class) {
                    // inheritable
                    if (methodName.equals("writeReplace")) {
                        final Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 0) {
                            writeReplace = method;
                            writeReplace.setAccessible(true);
                        }
                    } else if (methodName.equals("readResolve")) {
                        final Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 0) {
                            readResolve = method;
                            readResolve.setAccessible(true);
                        }
                    }
                }
            }
        }
        if (readResolve == null || writeReplace == null) {
            final Class<?> superclass = subject.getSuperclass();
            if (superclass != null) {
                final SerializableClass superInfo = SerializableClassRegistry.getInstanceUnchecked().lookup(superclass);
                final Method otherReadResolve = superInfo.readResolve;
                if (readResolve == null && otherReadResolve != null && ! Modifier.isPrivate(otherReadResolve.getModifiers())) {
                    readResolve = otherReadResolve;
                }
                final Method otherWriteReplace = superInfo.writeReplace;
                if (writeReplace == null && otherWriteReplace != null && ! Modifier.isPrivate(otherWriteReplace.getModifiers())) {
                    writeReplace = otherWriteReplace;
                }
            }
        }
        Constructor<?> noArgConstructor = null;
        Constructor<?> objectInputConstructor = null;
        for (Constructor<?> constructor : subject.getConstructors()) {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 0) {
                noArgConstructor = constructor;
                noArgConstructor.setAccessible(true);
            } else if (parameterTypes.length == 1 && parameterTypes[0] == ObjectInput.class) {
                objectInputConstructor = constructor;
                objectInputConstructor.setAccessible(true);
            }
        }
        this.writeObject = writeObject;
        this.readObject = readObject;
        this.readObjectNoData = readObjectNoData;
        this.noArgConstructor = noArgConstructor;
        this.objectInputConstructor = objectInputConstructor;
        this.readResolve = readResolve;
        this.writeReplace = writeReplace;
        nonInitConstructor = lookupNonInitConstructor(subject);
        final ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(subject);
        effectiveSerialVersionUID = objectStreamClass == null ? 0L : objectStreamClass.getSerialVersionUID(); // todo find a better solution
        final HashMap<String, SerializableField> fieldsByName = new HashMap<String, SerializableField>();
        for (SerializableField serializableField : fields = getSerializableFields(subject)) {
            fieldsByName.put(serializableField.getName(), serializableField);
        }
        this.fieldsByName = fieldsByName;
    }

    private static SerializableField[] getSerializableFields(Class<?> clazz) {
        final Field[] declaredFields = clazz.getDeclaredFields();
        final ObjectStreamField[] objectStreamFields = getDeclaredSerialPersistentFields(clazz);
        if (objectStreamFields != null) {
            final Map<String, Field> map = new HashMap<String, Field>();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                map.put(field.getName(), field);
            }
            SerializableField[] fields = new SerializableField[objectStreamFields.length];
            for (int i = 0; i < objectStreamFields.length; i++) {
                ObjectStreamField field = objectStreamFields[i];
                final String name = field.getName();
                final Field realField = map.get(name);
                if (realField != null && realField.getType() == field.getType()) {
                    // allow direct updating of the field data since the types match
                    fields[i] = new SerializableField(field.getType(), name, field.isUnshared(), realField);
                } else {
                    // no direct update possible
                    fields[i] = new SerializableField(field.getType(), name, field.isUnshared(), null);
                }
            }
            Arrays.sort(fields, NAME_COMPARATOR);
            return fields;
        }
        final ArrayList<SerializableField> fields = new ArrayList<SerializableField>(declaredFields.length);
        for (Field field : declaredFields) {
            if ((field.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
                field.setAccessible(true);
                fields.add(new SerializableField(field.getType(), field.getName(), false, field));
            }
        }
        Collections.sort(fields, NAME_COMPARATOR);
        return fields.toArray(new SerializableField[fields.size()]);
    }

    private static ObjectStreamField[] getDeclaredSerialPersistentFields(Class<?> clazz) {
        final Field field;
        try {
            field = clazz.getDeclaredField("serialPersistentFields");
        } catch (NoSuchFieldException e) {
            return null;
        }
        if (field == null) {
            return null;
        }
        final int requiredModifiers = Modifier.STATIC | Modifier.PRIVATE | Modifier.FINAL;
        if ((field.getModifiers() & requiredModifiers) != requiredModifiers) {
            return null;
        }
        field.setAccessible(true);
        try {
            return (ObjectStreamField[]) field.get(null);
        } catch (IllegalAccessException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Get the serializable fields of this class.  The returned array is a direct reference, so care should be taken
     * not to modify it.
     *
     * @return the fields
     */
    public SerializableField[] getFields() {
        return fields;
    }

    /**
     * Create a synthetic field for this object class.
     *
     * @param name the name of the field
     * @param fieldType the field type
     * @param unshared {@code true} if the field should be unshared
     * @return the field
     * @throws ClassNotFoundException if a class was not found while looking up the subject class
     */
    public SerializableField getSerializableField(String name, Class<?> fieldType, boolean unshared) throws ClassNotFoundException {
        final SerializableField serializableField = fieldsByName.get(name);
        if (serializableField != null) {
            return serializableField;
        }
        return new SerializableField(fieldType, name, unshared, null);
    }

    /**
     * Determine whether this class has a {@code writeObject()} method.
     *
     * @return {@code true} if there is a {@code writeObject()} method
     */
    public boolean hasWriteObject() {
        return writeObject != null;
    }

    /**
     * Invoke the {@code writeObject()} method for an object.
     *
     * @param object the object to invoke on
     * @param outputStream the object output stream to pass in
     * @throws IOException if an I/O error occurs
     */
    public void callWriteObject(Object object, ObjectOutputStream outputStream) throws IOException {
        try {
            writeObject.invoke(object, outputStream);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof IOException) {
                throw (IOException)te;
            } else if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method is unexpectedly inaccessible");
        }
    }

    /**
     * Determine whether this class has a {@code readObject()} method.
     *
     * @return {@code true} if there is a {@code readObject()} method
     */
    public boolean hasReadObject() {
        return readObject != null;
    }

    /**
     * Invoke the {@code readObject()} method for an object.
     *
     * @param object the object to invoke on
     * @param inputStream the object input stream to pass in
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class was not able to be loaded
     */
    public void callReadObject(Object object, ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        try {
            readObject.invoke(object, inputStream);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof IOException) {
                throw (IOException)te;
            } else if (te instanceof ClassNotFoundException) {
                throw (ClassNotFoundException)te;
            } else if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method is unexpectedly inaccessible");
        }
    }

    /**
     * Determine whether this class has a {@code readObjectNoData()} method.
     *
     * @return {@code true} if there is a {@code readObjectNoData()} method
     */
    public boolean hasReadObjectNoData() {
        return readObjectNoData != null;
    }

    /**
     * Invoke the {@code readObjectNoData()} method for an object.
     *
     * @param object the object to invoke on
     * @throws ObjectStreamException if an I/O error occurs
     */
    public void callReadObjectNoData(Object object) throws ObjectStreamException {
        try {
            readObjectNoData.invoke(object);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof ObjectStreamException) {
                throw (ObjectStreamException)te;
            } else if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method is unexpectedly inaccessible");
        }
    }

    /**
     * Determine whether this class has a {@code writeReplace()} method.
     *
     * @return {@code true} if there is a {@code writeReplace()} method
     */
    public boolean hasWriteReplace() {
        return writeReplace != null;
    }

    /**
     * Invoke the {@code writeReplace()} method for an object.
     *
     * @param object the object to invoke on
     * @return the nominated replacement object 
     * @throws ObjectStreamException if an I/O error occurs
     */
    public Object callWriteReplace(Object object) throws ObjectStreamException {
        try {
            return writeReplace.invoke(object);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof ObjectStreamException) {
                throw (ObjectStreamException)te;
            } else if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method is unexpectedly inaccessible");
        }
    }

    /**
     * Determine whether this class has a {@code readResolve()} method.
     *
     * @return {@code true} if there is a {@code readResolve()} method
     */
    public boolean hasReadResolve() {
        return readResolve != null;
    }

    /**
     * Invoke the {@code readResolve()} method for an object.
     *
     * @param object the object to invoke on
     * @return the original object
     * @throws ObjectStreamException if an I/O error occurs
     */
    public Object callReadResolve(Object object) throws ObjectStreamException {
        try {
            return readResolve.invoke(object);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof ObjectStreamException) {
                throw (ObjectStreamException)te;
            } else if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method is unexpectedly inaccessible");
        }
    }

    /**
     * Determine whether this class has a public no-arg constructor.
     *
     * @return {@code true} if there is such a constructor
     */
    public boolean hasNoArgConstructor() {
        return noArgConstructor != null;
    }

    /**
     * Invoke the public no-arg constructor on this class.
     *
     * @return the new instance
     * @throws IOException if an I/O error occurs
     */
    public Object callNoArgConstructor() throws IOException {
        return invokeConstructor(noArgConstructor);
    }

    /**
     * Determine whether this class has a public constructor accepting an ObjectInput.
     *
     * @return {@code true} if there is such a constructor
     */
    public boolean hasObjectInputConstructor() {
        return objectInputConstructor != null;
    }

    /**
     * Invoke the public constructor accepting an ObjectInput.
     *
     * @param objectInput the ObjectInput to pass to the constructor
     * @return the new instance
     * @throws IOException if an I/O error occurs
     */
    public Object callObjectInputConstructor(final ObjectInput objectInput) throws IOException {
        return invokeConstructor(objectInputConstructor, objectInput);
    }

    /**
     * Determine whether this class has a non-init constructor.
     *
     * @return whether this class has a non-init constructor
     */
    public boolean hasNoInitConstructor() {
        return nonInitConstructor != null;
    }

    /**
     * Invoke the non-init constructor on this class.
     *
     * @return the new instance
     */
    public Object callNonInitConstructor() {
        return invokeConstructorNoException(nonInitConstructor);
    }

    private static Object invokeConstructor(Constructor<?> constructor, Object... args) throws IOException {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof IOException) {
                throw (IOException)te;
            } else if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (InstantiationException e) {
            throw new IllegalStateException("Instantiation failed unexpectedly");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Constructor is unexpectedly inaccessible");
        }
    }

    private static Object invokeConstructorNoException(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (InstantiationException e) {
            throw new IllegalStateException("Instantiation failed unexpectedly");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Constructor is unexpectedly inaccessible");
        }
    }

    /**
     * Get the effective serial version UID of this class.
     *
     * @return the serial version UID
     */
    public long getEffectiveSerialVersionUID() {
        return effectiveSerialVersionUID;
    }

    /**
     * Get the {@code Class} of this class.
     *
     * @return the subject class
     */
    public Class<?> getSubjectClass() {
        return subject;
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> lookupNonInitConstructor(final Class<T> subject) {
        Class<? super T> current = subject;
        for (; Serializable.class.isAssignableFrom(current); current = current.getSuperclass());
        final Constructor<? super T> topConstructor;
        try {
            topConstructor = current.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        }
        topConstructor.setAccessible(true);
        final Constructor<T> generatedConstructor = (Constructor<T>) reflectionFactory.newConstructorForSerialization(subject, topConstructor);
        generatedConstructor.setAccessible(true);
        return generatedConstructor;
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> getNoInitConstructor() {
        return (Constructor<T>) nonInitConstructor;
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> getNoArgConstructor() {
        return (Constructor<T>) noArgConstructor;
    }

    public String toString() {
        return String.format("Serializable %s", getSubjectClass());
    }
}
