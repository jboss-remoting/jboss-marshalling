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
