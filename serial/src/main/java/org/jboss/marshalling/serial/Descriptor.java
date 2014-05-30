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

import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableField;
import java.io.IOException;

/**
 *
 */
abstract class Descriptor implements ExtendedObjectStreamConstants {
    private final Descriptor parent;
    private final Class<?> type;

    protected Descriptor(final Descriptor parent, final Class<?> type) {
        this.parent = parent;
        this.type = type;
    }

    protected Descriptor getParent() {
        return parent;
    }

    protected Class<?> getType() {
        return type;
    }

    protected Class<?> getNearestType() {
        return type != null ? type : parent != null ? parent.getNearestType() : Object.class;
    }

    protected abstract void readSerial(SerialUnmarshaller serialUnmarshaller, SerializableClass sc, Object subject) throws IOException, ClassNotFoundException;

    public int getFlags() {
        return 0;
    }

    public SerializableField[] getFields() {
        return SerializableClass.NOFIELDS;
    }
}
