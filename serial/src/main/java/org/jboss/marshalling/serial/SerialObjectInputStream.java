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

import org.jboss.marshalling.MarshallerObjectInputStream;
import org.jboss.marshalling.reflect.SerializableClass;
import java.io.IOException;
import java.io.ObjectInputValidation;
import java.io.NotActiveException;
import java.io.InvalidObjectException;

/**
 *
 */
public final class SerialObjectInputStream extends MarshallerObjectInputStream {
    private final SerialUnmarshaller serialUnmarshaller;
    private PlainDescriptor currentDescriptor;
    private SerializableClass currentSerializableClass;
    private Object currentSubject;
    private State state = State.OFF;

    enum State {
        OFF,
        NEW,
        ON,
        ;
    }

    SerialObjectInputStream(final SerialUnmarshaller serialUnmarshaller) throws IOException, SecurityException {
        super(serialUnmarshaller.getBlockUnmarshaller());
        this.serialUnmarshaller = serialUnmarshaller;
    }

    PlainDescriptor saveCurrentDescriptor(PlainDescriptor currentDescriptor) {
        try {
            return this.currentDescriptor;
        } finally {
            this.currentDescriptor = currentDescriptor;
        }
    }

    void setCurrentDescriptor(final PlainDescriptor currentDescriptor) {
        this.currentDescriptor = currentDescriptor;
    }

    Object saveCurrentSubject(Object currentSubject) {
        try {
            return this.currentSubject;
        } finally {
            this.currentSubject = currentSubject;
        }
    }

    void setCurrentSubject(final Object currentSubject) {
        this.currentSubject = currentSubject;
    }

    SerializableClass saveCurrentSerializableClass(SerializableClass currentSerializableClass) {
        try {
            return this.currentSerializableClass;
        } finally {
            this.currentSerializableClass = currentSerializableClass;
        }
    }

    void setCurrentSerializableClass(final SerializableClass currentSerializableClass) {
        this.currentSerializableClass = currentSerializableClass;
    }

    State saveState() {
        try {
            return state;
        } finally {
            state = State.NEW;
        }
    }

    State restoreState(final State state) {
        try {
            return this.state;
        } finally {
            this.state = state;
        }
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (state != State.NEW) {
            throw new IllegalStateException("Fields may not be read now");
        }
        final BlockUnmarshaller blockUnmarshaller = serialUnmarshaller.getBlockUnmarshaller();
        final int cnt = blockUnmarshaller.available();
        if (cnt == -1) {
            blockUnmarshaller.unblock();
        }
        state = State.ON;
        currentDescriptor.defaultReadFields(serialUnmarshaller, currentSubject);
        if (cnt == -1) {
            blockUnmarshaller.endOfStream();
        }
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
        if (state != State.NEW) {
            throw new IllegalStateException("Fields may not be read now");
        }
        final BlockUnmarshaller blockUnmarshaller = serialUnmarshaller.getBlockUnmarshaller();
        final int cnt = blockUnmarshaller.available();
        if (cnt == -1) {
            blockUnmarshaller.unblock();
        }
        state = State.ON;
        try {
            return currentDescriptor.getField(serialUnmarshaller, currentSerializableClass);
        } finally {
            if (cnt == -1) {
                blockUnmarshaller.endOfStream();
            }
        }
    }

    public void registerValidation(final ObjectInputValidation obj, final int prio) throws NotActiveException, InvalidObjectException {
        serialUnmarshaller.addValidation(obj, prio);
    }
}
