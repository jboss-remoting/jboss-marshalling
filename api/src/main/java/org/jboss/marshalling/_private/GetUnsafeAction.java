/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
import java.security.PrivilegedAction;

/**
 */
final class GetUnsafeAction implements PrivilegedAction<Unsafe> {
    static final GetUnsafeAction INSTANCE = new GetUnsafeAction();

    private GetUnsafeAction() {
    }

    public Unsafe run() {
        try {
            final Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return new UnsafeWrapper((sun.misc.Unsafe) unsafeField.get(null));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    private static class UnsafeWrapper implements Unsafe {
        private final sun.misc.Unsafe delegate;

        private UnsafeWrapper(final sun.misc.Unsafe delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean getBoolean(final Object o, final long offset) {
            return delegate.getBoolean(o, offset);
        }

        @Override
        public byte getByte(final Object o, final long offset) {
            return delegate.getByte(o, offset);
        }

        @Override
        public char getChar(final Object o, final long offset) {
            return delegate.getChar(o, offset);
        }

        @Override
        public double getDouble(final Object o, final long offset) {
            return delegate.getDouble(o, offset);
        }

        @Override
        public float getFloat(final Object o, final long offset) {
            return delegate.getFloat(o, offset);
        }

        @Override
        public int getInt(final Object o, final long offset) {
            return delegate.getInt(o, offset);
        }

        @Override
        public long getLong(final Object o, final long offset) {
            return delegate.getLong(o, offset);
        }

        @Override
        public Object getObject(final Object o, final long offset) {
            return delegate.getObject(o, offset);
        }

        @Override
        public Object getObjectVolatile(final Object o, final long offset) {
            return delegate.getObjectVolatile(o, offset);
        }

        @Override
        public short getShort(final Object o, final long offset) {
            return delegate.getShort(o, offset);
        }

        @Override
        public long objectFieldOffset(final Field f) {
            return delegate.objectFieldOffset(f);
        }

        @Override
        public void putObject(final Object o, final long offset, final Object x) {
            delegate.putObject(o, offset, x);
        }

        @Override
        public void putBoolean(final Object o, final long offset, final boolean x) {
            delegate.putBoolean(o, offset, x);
        }

        @Override
        public void putByte(final Object o, final long offset, final byte x) {
            delegate.putByte(o, offset, x);
        }

        @Override
        public void putChar(final Object o, final long offset, final char x) {
            delegate.putChar(o, offset, x);
        }

        @Override
        public void putDouble(final Object o, final long offset, final double x) {
            delegate.putDouble(o, offset, x);
        }

        @Override
        public void putFloat(final Object o, final long offset, final float x) {
            delegate.putFloat(o, offset, x);
        }

        @Override
        public void putInt(final Object o, final long offset, final int x) {
            delegate.putInt(o, offset, x);
        }

        @Override
        public void putLong(final Object o, final long offset, final long x) {
            delegate.putLong(o, offset, x);
        }

        @Override
        public void putShort(final Object o, final long offset, final short x) {
            delegate.putShort(o, offset, x);
        }

        @Override
        public Object staticFieldBase(final Field f) {
            return delegate.staticFieldBase(f);
        }

        @Override
        public long staticFieldOffset(final Field f) {
            return delegate.staticFieldOffset(f);
        }
    }
}
