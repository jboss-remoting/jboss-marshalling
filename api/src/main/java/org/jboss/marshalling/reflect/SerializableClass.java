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

package org.jboss.marshalling.reflect;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Reflection information about a serializable class.  Intended for use by implementations of the Marshalling API.
 */
public final class SerializableClass {

    private final WeakReference<Class<?>> subjectRef;
    private final LazyWeakMethodRef writeObject;
    private final LazyWeakMethodRef writeReplace;
    private final LazyWeakMethodRef readObject;
    private final LazyWeakMethodRef readObjectNoData;
    private final LazyWeakMethodRef readResolve;
    private final SerializableField[] fields;
    private final long effectiveSerialVersionUID;

    private static final Comparator<? super SerializableField> NAME_COMPARATOR = new Comparator<SerializableField>() {
        public int compare(final SerializableField o1, final SerializableField o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    SerializableClass(Class<?> subject) {
        final WeakReference<Class<?>> subjectRef = new WeakReference<Class<?>>(subject);
        this.subjectRef = subjectRef;
        writeObject = LazyWeakMethodRef.getInstance(new MethodFinder() {
            public Method get(Class<?> clazz) {
                return lookupPrivateMethod(clazz, "writeObject", ObjectOutputStream.class);
            }
        }, subjectRef);
        readObject = LazyWeakMethodRef.getInstance(new MethodFinder() {
            public Method get(final Class<?> clazz) {
                return lookupPrivateMethod(clazz, "readObject", ObjectInputStream.class);
            }
        }, subjectRef);
        readObjectNoData = LazyWeakMethodRef.getInstance(new MethodFinder() {
            public Method get(final Class<?> clazz) {
                return lookupPrivateMethod(clazz, "readObjectNoData");
            }
        }, subjectRef);
        writeReplace = LazyWeakMethodRef.getInstance(new MethodFinder() {
            public Method get(final Class<?> clazz) {
                return lookupInheritableMethod(clazz, "writeReplace");
            }
        }, subjectRef);
        readResolve = LazyWeakMethodRef.getInstance(new MethodFinder() {
            public Method get(final Class<?> clazz) {
                return lookupInheritableMethod(clazz, "readResolve");
            }
        }, subjectRef);
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

    private static Method lookupPrivateMethod(final Class<?> subject, final String name, final Class<?>... params) {
        return AccessController.doPrivileged(new PrivilegedAction<Method>() {
            public Method run() {
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
        });
    }

    private static Method lookupInheritableMethod(final Class<?> subject, final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<Method>() {
            public Method run() {
                Class<?> foundClass;
                Method method = null;
                for (foundClass = subject; foundClass != null; foundClass = foundClass.getSuperclass()) {
                    try {
                        method = foundClass.getDeclaredMethod(name);
                    } catch (NoSuchMethodException e) {
                        continue;
                    }
                }
                if (method == null) {
                    // missing
                    return null;
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
        });
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

    private interface MethodFinder {
        Method get(Class<?> clazz);
    }

    static Class<?> dereference(final WeakReference<Class<?>> classRef) throws ClassNotFoundException {
        final Class<?> clazz = classRef.get();
        if (clazz == null) {
            throw new ClassNotFoundException("Class was unloaded");
        }
        return clazz;
    }

    private static class LazyWeakMethodRef {
        private final AtomicReference<WeakReference<Method>> ref;
        private final MethodFinder finder;
        private final WeakReference<Class<?>> classRef;

        private LazyWeakMethodRef(final MethodFinder finder, final Method initial, final WeakReference<Class<?>> classRef) {
            this.finder = finder;
            this.classRef = classRef;
            ref = new AtomicReference<WeakReference<Method>>(new WeakReference<Method>(initial));
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
            final WeakReference<Method> weakReference = ref.get();
            if (weakReference != null) {
                final Method method = weakReference.get();
                if (method != null) {
                    return method;
                }
            }
            final Class<?> clazz = dereference(classRef);
            final Method method = finder.get(clazz);
            if (method == null) {
                throw new NullPointerException("method is null (was non-null on last check)");
            }
            final WeakReference<Method> newVal = new WeakReference<Method>(method);
            ref.compareAndSet(weakReference, newVal);
            return method;
        }
    }
}
