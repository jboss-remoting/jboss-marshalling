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
import java.lang.ref.WeakReference;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.ObjectStreamClass;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import sun.reflect.ReflectionFactory;

/**
 * Reflection information about a serializable class.  Intended for use by implementations of the Marshalling API.
 */
public final class SerializableClass {
    private static final ReflectionFactory reflectionFactory = (ReflectionFactory) AccessController.doPrivileged(new ReflectionFactory.GetReflectionFactoryAction());

    private final WeakReference<Class<?>> subjectRef;
    private final LazyWeakMethodRef writeObject;
    private final LazyWeakMethodRef writeReplace;
    private final LazyWeakMethodRef readObject;
    private final LazyWeakMethodRef readObjectNoData;
    private final LazyWeakMethodRef readResolve;
    private final LazyWeakConstructorRef noArgConstructor;
    private final LazyWeakConstructorRef objectInputConstructor;
    private final LazyWeakConstructorRef nonInitConstructor;
    private final SerializableField[] fields;
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
        final WeakReference<Class<?>> subjectRef = new WeakReference<Class<?>>(subject);
        this.subjectRef = subjectRef;
        writeObject = LazyWeakMethodRef.getInstance(WRITE_OBJECT_FINDER, subjectRef);
        readObject = LazyWeakMethodRef.getInstance(READ_OBJECT_FINDER, subjectRef);
        readObjectNoData = LazyWeakMethodRef.getInstance(READ_OBJECT_NO_DATA_FINDER, subjectRef);
        writeReplace = LazyWeakMethodRef.getInstance(WRITE_REPLACE_FINDER, subjectRef);
        readResolve = LazyWeakMethodRef.getInstance(READ_RESOLVE_FINDER, subjectRef);
        noArgConstructor = LazyWeakConstructorRef.getInstance(SIMPLE_CONSTRUCTOR_FINDER, subjectRef);
        nonInitConstructor = LazyWeakConstructorRef.getInstance(NON_INIT_CONSTRUCTOR_FINDER, subjectRef);
        objectInputConstructor = LazyWeakConstructorRef.getInstance(OBJECT_INPUT_CONSTRUCTOR_FINDER, subjectRef);
        final ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(subject);
        effectiveSerialVersionUID = objectStreamClass == null ? 0L : objectStreamClass.getSerialVersionUID(); // todo find a better solution
        fields = getSerializableFields(subject);
    }

    private static SerializableField[] getSerializableFields(Class<?> clazz) {
        final ObjectStreamField[] objectStreamFields = getDeclaredSerialPersistentFields(clazz);
        if (objectStreamFields != null) {
            SerializableField[] fields = new SerializableField[objectStreamFields.length];
            for (int i = 0; i < objectStreamFields.length; i++) {
                ObjectStreamField field = objectStreamFields[i];
                fields[i] = new SerializableField(clazz, field.getType(), field.getName(), field.isUnshared());
            }
            Arrays.sort(fields, NAME_COMPARATOR);
            return fields;
        }
        // not declared; we'll have to dig through the class's fields
        final Field[] declaredFields = clazz.getDeclaredFields();
        final ArrayList<SerializableField> fields = new ArrayList<SerializableField>(declaredFields.length);
        for (Field field : declaredFields) {
            if ((field.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
                fields.add(new SerializableField(clazz, field.getType(), field.getName(), false));
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
        return new SerializableField(getSubjectClass(), fieldType, name, unshared);
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
            writeObject.getMethod().invoke(object, outputStream);
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
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class is unexpectedly missing or changed");
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
            readObject.getMethod().invoke(object, inputStream);
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
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class is unexpectedly missing or changed");
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
            readObjectNoData.getMethod().invoke(object);
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
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class is unexpectedly missing or changed");
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
            return writeReplace.getMethod().invoke(object);
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
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class is unexpectedly missing or changed");
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
            return readResolve.getMethod().invoke(object);
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
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class is unexpectedly missing or changed");
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

    private static Object invokeConstructor(LazyWeakConstructorRef ref, Object... args) throws IOException {
        try {
            return ref.getConstructor().newInstance(args);
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
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class is unexpectedly missing or changed");
        } catch (InstantiationException e) {
            throw new IllegalStateException("Instantiation failed unexpectedly");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Constructor is unexpectedly inaccessible");
        }
    }

    private static Object invokeConstructorNoException(LazyWeakConstructorRef ref, Object... args) {
        try {
            return ref.getConstructor().newInstance(args);
        } catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te instanceof RuntimeException) {
                throw (RuntimeException)te;
            } else if (te instanceof Error) {
                throw (Error)te;
            } else {
                throw new IllegalStateException("Unexpected exception", te);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class is unexpectedly missing or changed");
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
     * @throws ClassNotFoundException if the class was unloaded
     */
    public Class<?> getSubjectClass() throws ClassNotFoundException {
        return dereference(subjectRef);
    }

    private static <T> Constructor<T> lookupPublicConstructor(final Class<T> subject, final Class<?>... params) {
        try {
            Constructor<T> constructor = subject.getConstructor(params);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method lookupPrivateMethod(final Class<?> subject, final String name, final Class<?>... params) {
        try {
            final Method method = subject.getDeclaredMethod(name, params);
            final int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PRIVATE) == 0) {
                // must be private...
                return null;
            } else if ((modifiers & Modifier.STATIC) != 0) {
                // must NOT be static...
                return null;
            } else {
                method.setAccessible(true);
                return method;
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method lookupInheritableMethod(final Class<?> subject, final String name) {
        Class<?> foundClass = subject;
        Method method = null;
        while (method == null) {
            try {
                if (foundClass == null) {
                    return null;
                }
                method = foundClass.getDeclaredMethod(name);
                if (method == null) {
                    foundClass = foundClass.getSuperclass();
                }
            } catch (NoSuchMethodException e) {
                foundClass = foundClass.getSuperclass();
                continue;
            }
        }
        final int modifiers = method.getModifiers();
        if ((modifiers & Modifier.STATIC) != 0) {
            // must NOT be static..
            return null;
        } else if ((modifiers & Modifier.ABSTRACT) != 0) {
            // must NOT be abstract...
            return null;
        } else if ((modifiers & Modifier.PRIVATE) != 0 && foundClass != subject) {
            // not visible to the actual class
            return null;
        } else if ((modifiers & (Modifier.PROTECTED | Modifier.PUBLIC)) != 0 || isSamePackage(foundClass, subject)) {
            // visible!
            method.setAccessible(true);
            return method;
        } else {
            // package private, but not the same package
            return null;
        }
    }

    // the package is the same if the name and classloader are both the same
    private static boolean isSamePackage(Class<?> a, Class<?> b) {
        return a.getClassLoader() == b.getClassLoader() && getPackageName(a).equals(getPackageName(b));
    }

    private static String getPackageName(Class<?> c) {
        String name = c.getName();
        // skip array part
        int idx = name.lastIndexOf('[');
        if (idx > -1) {
            // [[[[Lfoo.bar.baz.Blah;
            // skip [ and also the L
            name = name.substring(idx + 2);
        }
        idx = name.lastIndexOf('.');
        if (idx > -1) {
            // foo.bar.baz.Blah;
            name = name.substring(0, idx);
            return name;
        } else {
            // no package
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> getNoInitConstructor() {
        try {
            return (Constructor<T>) nonInitConstructor.getConstructor();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private interface MethodFinder {
        Method get(Class<?> clazz);
    }

    private interface ConstructorFinder {
        <T> Constructor<T> get(Class<T> clazz);
    }

    static Class<?> dereference(final WeakReference<Class<?>> classRef) throws ClassNotFoundException {
        final Class<?> clazz = classRef.get();
        if (clazz == null) {
            throw new ClassNotFoundException("Class was unloaded");
        }
        return clazz;
    }

    private static class LazyWeakMethodRef {
        private volatile WeakReference<Method> ref;
        private final MethodFinder finder;
        private final WeakReference<Class<?>> classRef;

        @SuppressWarnings("unchecked")
        private static final AtomicReferenceFieldUpdater<LazyWeakMethodRef, WeakReference> refUpdater = AtomicReferenceFieldUpdater.newUpdater(LazyWeakMethodRef.class, WeakReference.class, "ref");

        private LazyWeakMethodRef(final MethodFinder finder, final Method initial, final WeakReference<Class<?>> classRef) {
            this.finder = finder;
            this.classRef = classRef;
            ref = new WeakReference<Method>(initial);
        }

        private static LazyWeakMethodRef getInstance(MethodFinder finder, WeakReference<Class<?>> classRef) {
            final Class<?> clazz = classRef.get();
            if (clazz == null) {
                throw new NullPointerException("clazz is null (no strong reference held to class when serialization info was acquired");
            }
            final Method method = finder.get(clazz);
            if (method == null) {
                return null;
            }
            return new LazyWeakMethodRef(finder, method, classRef);
        }

        private Method getMethod() throws ClassNotFoundException {
            final WeakReference<Method> weakReference = ref;
            if (weakReference != null) {
                final Method method = weakReference.get();
                if (method != null) {
                    return method;
                }
            }
            final Class<?> clazz = dereference(classRef);
            final SecurityManager sm = System.getSecurityManager();
            final Method method;
            if (sm != null) {
                method = AccessController.doPrivileged(new MethodFinderAction(finder, clazz));
            } else {
                method = finder.get(clazz);
            }
            if (method == null) {
                throw new NullPointerException("method is null (was non-null on last check)");
            }
            final WeakReference<Method> newVal = new WeakReference<Method>(method);
            refUpdater.compareAndSet(this, weakReference, newVal);
            return method;
        }
    }

    private static class LazyWeakConstructorRef {
        private volatile WeakReference<Constructor<?>> ref;
        private final ConstructorFinder finder;
        private final WeakReference<Class<?>> classRef;

        @SuppressWarnings("unchecked")
        private static final AtomicReferenceFieldUpdater<LazyWeakConstructorRef, WeakReference> refUpdater = AtomicReferenceFieldUpdater.newUpdater(LazyWeakConstructorRef.class, WeakReference.class, "ref");

        private LazyWeakConstructorRef(final ConstructorFinder finder, final Constructor<?> initial, final WeakReference<Class<?>> classRef) {
            this.finder = finder;
            this.classRef = classRef;
            ref = new WeakReference<Constructor<?>>(initial);
        }

        private static LazyWeakConstructorRef getInstance(ConstructorFinder finder, WeakReference<Class<?>> classRef) {
            final Class<?> clazz = classRef.get();
            if (clazz == null) {
                throw new NullPointerException("clazz is null (no strong reference held to class when serialization info was acquired");
            }
            final Constructor<?> constructor = finder.get(clazz);
            if (constructor == null) {
                return null;
            }
            return new LazyWeakConstructorRef(finder, constructor, classRef);
        }

        private Constructor<?> getConstructor() throws ClassNotFoundException {
            final WeakReference<Constructor<?>> weakReference = ref;
            if (weakReference != null) {
                final Constructor<?> method = weakReference.get();
                if (method != null) {
                    return method;
                }
            }
            final Class<?> clazz = dereference(classRef);
            final Constructor<?> constructor;
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                constructor = doAction(finder, clazz);
            } else {
                constructor = finder.get(clazz);
            }
            if (constructor == null) {
                throw new NullPointerException("constructor is null (was non-null on last check)");
            }
            final WeakReference<Constructor<?>> newVal = new WeakReference<Constructor<?>>(constructor);
            refUpdater.compareAndSet(this, weakReference, newVal);
            return constructor;
        }

        private static <T> Constructor<T> doAction(ConstructorFinder finder, Class<T> clazz) {
            return AccessController.doPrivileged(new ConstructorFinderAction<T>(finder, clazz));
        }
    }

    private static final MethodFinder WRITE_OBJECT_FINDER = new PrivateMethodFinder("writeObject", ObjectOutputStream.class);
    private static final MethodFinder READ_OBJECT_FINDER = new PrivateMethodFinder("readObject", ObjectInputStream.class);
    private static final MethodFinder READ_OBJECT_NO_DATA_FINDER = new PrivateMethodFinder("readObjectNoData");
    private static final MethodFinder WRITE_REPLACE_FINDER = new InheritableMethodFinder("writeReplace");
    private static final MethodFinder READ_RESOLVE_FINDER = new InheritableMethodFinder("readResolve");
    private static final ConstructorFinder SIMPLE_CONSTRUCTOR_FINDER = new PublicConstructorFinder();
    private static final ConstructorFinder OBJECT_INPUT_CONSTRUCTOR_FINDER = new PublicConstructorFinder(ObjectInput.class);
    private static final ConstructorFinder NON_INIT_CONSTRUCTOR_FINDER = new NoInitConstructorFinder();

    private static final class PrivateMethodFinder implements MethodFinder {
        private final String name;
        private final Class<?>[] params;

        private PrivateMethodFinder(final String name, final Class<?>... params) {
            this.name = name;
            this.params = params;
        }

        public Method get(final Class<?> clazz) {
            return lookupPrivateMethod(clazz, name, params);
        }
    }

    private static final class InheritableMethodFinder implements MethodFinder {
        private final String name;

        private InheritableMethodFinder(final String name) {
            this.name = name;
        }

        public Method get(final Class<?> clazz) {
            return lookupInheritableMethod(clazz, name);
        }
    }

    private static final class MethodFinderAction implements PrivilegedAction<Method> {
        private final MethodFinder finder;
        private final Class<?> clazz;

        private MethodFinderAction(final MethodFinder finder, final Class<?> clazz) {
            this.finder = finder;
            this.clazz = clazz;
        }

        public Method run() {
            return finder.get(clazz);
        }
    }

    private static final class PublicConstructorFinder implements ConstructorFinder {
        private final Class<?>[] params;

        private PublicConstructorFinder(final Class<?>... params) {
            this.params = params;
        }

        public <T> Constructor<T> get(final Class<T> clazz) {
            return lookupPublicConstructor(clazz, params);
        }
    }

    private static final class NoInitConstructorFinder implements ConstructorFinder {

        private NoInitConstructorFinder() {
        }

        @SuppressWarnings("unchecked")
        public <T> Constructor<T> get(final Class<T> clazz) {
            Class<? super T> current = clazz;
            for (; Serializable.class.isAssignableFrom(current); current = current.getSuperclass());
            final Constructor<? super T> topConstructor;
            try {
                topConstructor = current.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                return null;
            }
            topConstructor.setAccessible(true);
            final Constructor<T> generatedConstructor = (Constructor<T>) reflectionFactory.newConstructorForSerialization(clazz, topConstructor);
            generatedConstructor.setAccessible(true);
            return generatedConstructor;
        }
    }

    private static final class ConstructorFinderAction<T> implements PrivilegedAction<Constructor<T>> {
        private final ConstructorFinder finder;
        private final Class<T> clazz;

        private ConstructorFinderAction(final ConstructorFinder finder, final Class<T> clazz) {
            this.finder = finder;
            this.clazz = clazz;
        }

        public Constructor<T> run() {
            return finder.get(clazz);
        }
    }

    public String toString() {
        try {
            return String.format("Serializable %s", getSubjectClass());
        } catch (ClassNotFoundException e) {
            return "Unloaded serializable class";
        }
    }
}
