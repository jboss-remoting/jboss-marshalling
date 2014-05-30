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

package org.jboss.marshalling.river;

import org.jboss.marshalling.reflect.SerializableField;
import org.jboss.marshalling.reflect.SerializableClass;

/**
 *
 */
public abstract class SerializableClassDescriptor extends ClassDescriptor {

    protected SerializableClassDescriptor() {}

    public abstract Class<?> getType();

    public abstract int getTypeID();

    public Class<?> getNearestType() {
        Class<?> type = getType();
        return type == null ? getSuperClassDescriptor().getNearestType() : type;
    }

    public abstract ClassDescriptor getSuperClassDescriptor();

    public abstract SerializableField[] getFields();

    public abstract SerializableClass getSerializableClass();

    public String toString() {
        final ClassDescriptor superClassDescriptor = getSuperClassDescriptor();
        if (superClassDescriptor == null) {
            return String.format("%s for %s (id %x02)", getClass().getSimpleName(), getType(), Integer.valueOf(getTypeID()));
        } else {
            return String.format("%s for %s (id %x02) extends %s", getClass().getSimpleName(), getType(), Integer.valueOf(getTypeID()), superClassDescriptor);
        }
    }
}
