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
public class RiverPutField extends ObjectOutputStream.PutField {

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
