/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
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

package org.jboss.marshalling._private;

import java.lang.reflect.Field;

import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

/**
 */
public interface Unsafe {

    Unsafe INSTANCE = getSecurityManager() == null ? GetUnsafeAction.INSTANCE.run() : doPrivileged(GetUnsafeAction.INSTANCE);

    boolean getBoolean(Object o, long offset);
    byte getByte(Object o, long offset);
    char getChar(Object o, long offset);
    double getDouble(Object o, long offset);
    float getFloat(Object o, long offset);
    int getInt(Object o, long offset);
    long getLong(Object o, long offset);
    Object getObject(Object o, long offset);
    Object getObjectVolatile(Object o, long offset);
    short getShort(Object o, long offset);
    long objectFieldOffset(Field f);
    void putBoolean(Object o, long offset, boolean x);
    void putByte(Object o, long offset, byte x);
    void putChar(Object o, long offset, char x);
    void putDouble(Object o, long offset, double x);
    void putFloat(Object o, long offset, float x);
    void putInt(Object o, long offset, int x);
    void putLong(Object o, long offset, long x);
    void putObject(Object o, long offset, Object x);
    void putShort(Object o, long offset, short x);
    Object staticFieldBase(Field f);
    long staticFieldOffset(Field f);

}
