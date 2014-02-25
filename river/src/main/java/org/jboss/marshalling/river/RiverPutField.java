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

package org.jboss.marshalling.river;

import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.IOException;
import java.util.Arrays;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.TraceInformation;
import org.jboss.marshalling.util.FieldPutter;

/**
 *
 */
class RiverPutField extends ObjectOutputStream.PutField {

    private final FieldPutter[] fields;
    private final String[] names;

    public RiverPutField(final FieldPutter[] fields, final String[] names) {
        this.fields = fields;
        this.names = names;
    }

    private FieldPutter find(final String name) {
        final int pos = Arrays.binarySearch(names, name);
        if (pos < 0) {
            throw new IllegalArgumentException("No field named '" + name + "' could be found");
        }
        return fields[pos];
    }

    public void put(final String name, final boolean val) {
        find(name).setBoolean(val);
    }

    public void put(final String name, final byte val) {
        find(name).setByte(val);
    }

    public void put(final String name, final char val) {
        find(name).setChar(val);
    }

    public void put(final String name, final short val) {
        find(name).setShort(val);
    }

    public void put(final String name, final int val) {
        find(name).setInt(val);
    }

    public void put(final String name, final long val) {
        find(name).setLong(val);
    }

    public void put(final String name, final float val) {
        find(name).setFloat(val);
    }

    public void put(final String name, final double val) {
        find(name).setDouble(val);
    }

    public void put(final String name, final Object val) {
        find(name).setObject(val);
    }

    @Deprecated
    public final void write(final ObjectOutput out) throws IOException {
        throw new UnsupportedOperationException("write(ObjectOutput)");
    }

    protected final void write(final Marshaller marshaller) throws IOException {
        final FieldPutter[] fields = this.fields;
        final int len = fields.length;
        for (int i = 0; i < len; i++) try {
            fields[i].write(marshaller);
        } catch (IOException e) {
            TraceInformation.addFieldInformation(e, names[i]);
            throw e;
        } catch (RuntimeException e) {
            TraceInformation.addFieldInformation(e, names[i]);
            throw e;
        }
    }
}
