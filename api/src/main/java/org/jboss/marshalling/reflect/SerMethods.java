package org.jboss.marshalling.reflect;

import static java.security.AccessController.doPrivileged;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedAction;

import sun.reflect.ReflectionFactory;

/**
 *
 */
final class SerMethods {
    private static final ReflectionFactory reflectionFactory = doPrivileged((PrivilegedAction<ReflectionFactory>) ReflectionFactory::getReflectionFactory);

    private final MethodHandle readObject;
    private final MethodHandle readObjectNoData;
    private final MethodHandle writeObject;
    private final MethodHandle readResolve;
    private final MethodHandle writeReplace;
    private final Constructor<?> noArgConstructor;
    private final Constructor<?> objectInputConstructor;

    SerMethods(Class<?> clazz) {
        readObject = reflectionFactory.readObjectForSerialization(clazz);
        readObjectNoData = reflectionFactory.readObjectNoDataForSerialization(clazz);
        writeObject = reflectionFactory.writeObjectForSerialization(clazz);
        readResolve = reflectionFactory.readResolveForSerialization(clazz);
        writeReplace = reflectionFactory.writeReplaceForSerialization(clazz);
        Constructor<?> ctor;
        Constructor<?> noArgConstructor = null;
        try {
            ctor = clazz.getDeclaredConstructor();
            noArgConstructor = reflectionFactory.newConstructorForSerialization(clazz, ctor);
        } catch (NoSuchMethodException ignored) {
        }
        this.noArgConstructor = noArgConstructor;
        Constructor<?> objectInputConstructor = null;
        try {
            ctor = clazz.getDeclaredConstructor(ObjectInput.class);
            objectInputConstructor = reflectionFactory.newConstructorForSerialization(clazz, ctor);
        } catch (NoSuchMethodException ignored) {
        }
        this.objectInputConstructor = objectInputConstructor;
    }

    static Constructor<?> newConstructorForSerialization(Class<?> classToInstantiate, Constructor<?> constructorToCall) {
        return reflectionFactory.newConstructorForSerialization(classToInstantiate, constructorToCall);
    }

    boolean hasWriteObject() {
        return writeObject != null;
    }

    void callWriteObject(Object object, ObjectOutputStream outputStream) throws IOException {
        try {
            writeObject.invoke(object, outputStream);
        } catch (IOException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    boolean hasReadObject() {
        return readObject != null;
    }

    void callReadObject(Object object, ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        try {
            readObject.invoke(object, inputStream);
        } catch (IOException | ClassNotFoundException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    boolean hasReadObjectNoData() {
        return readObjectNoData != null;
    }

    void callReadObjectNoData(Object object) throws ObjectStreamException {
        try {
            readObjectNoData.invoke(object);
        } catch (ObjectStreamException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    boolean hasWriteReplace() {
        return writeReplace != null;
    }

    Object callWriteReplace(Object object) throws ObjectStreamException {
        try {
            return writeReplace.invoke(object);
        } catch (ObjectStreamException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    boolean hasReadResolve() {
        return readResolve != null;
    }

    Object callReadResolve(Object object) throws ObjectStreamException {
        try {
            return readResolve.invoke(object);
        } catch (ObjectStreamException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    Constructor<?> getNoArgConstructor() {
        return noArgConstructor;
    }

    Constructor<?> getObjectInputConstructor() {
        return objectInputConstructor;
    }
}
