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

import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.InvalidClassException;
import java.io.ObjectStreamException;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.Creator;

/**
 * An externalized object.  This wrapper allows an object that was written with an {@code Externalizer} to be read by
 * standard Java serialization.  Note that if an externalized object's child object graph ever refers to the original
 * object, there will be an error in the reconstructed object graph such that those references will refer to this
 * wrapper object rather than the properly externalized object.
 */
public final class ExternalizedObject implements Externalizable, Creator {

    private static final long serialVersionUID = -7764783599281227099L;

    private Externalizer externalizer;
    private transient Object obj;

    public ExternalizedObject() {
    }

    public ExternalizedObject(final Externalizer externalizer, final Object obj) {
        this.externalizer = externalizer;
        this.obj = obj;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(obj.getClass());
        out.writeObject(externalizer);
        externalizer.writeExternal(obj, out);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        Class<?> subject = (Class<?>) in.readObject();
        externalizer = (Externalizer) in.readObject();
        final Object o = externalizer.createExternal(subject, in, this);
        externalizer.readExternal(o, in);
        obj = o;
    }

    /**
     * Return the externalized object after {@code readExternal()} completes.
     *
     * @return the externalized object
     *
     * @throws ObjectStreamException never
     */
    protected Object readResolve() {
        return obj;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T create(final Class<T> clazz) throws InvalidClassException {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            final InvalidClassException ee = new InvalidClassException(clazz.getName(), e.getMessage());
            ee.initCause(e);
            throw ee;
        }
    }
}
