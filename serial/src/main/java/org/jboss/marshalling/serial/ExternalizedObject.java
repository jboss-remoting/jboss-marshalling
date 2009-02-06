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

import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.InvalidClassException;
import java.io.ObjectStreamException;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.Creator;

/**
 * An externalized object.  This wrapper allows an object that was written with an {@code Externalizer} to be
 * read by standard Java serialization.  Note that if an externalized object's child object graph ever refers
 * to the original object, there will be an error in the reconstructed object graph such that those references
 * will refer to this wrapper object rather than the properly externalized object.
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

    /** {@inheritDoc} */
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(externalizer);
        externalizer.writeExternal(obj, out);
    }

    /** {@inheritDoc} */
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        externalizer = (Externalizer) in.readObject();
        final Object o = externalizer.createExternal(getClass(), in, this);
        externalizer.readExternal(o, in);
        obj = o;
    }

    /**
     * Return the externalized object after {@code readExternal()} completes.
     *
     * @return the externalized object
     * @throws ObjectStreamException never
     */
    public Object readResolve() throws ObjectStreamException {
        return obj;
    }

    /** {@inheritDoc} */
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
