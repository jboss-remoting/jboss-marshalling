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

package org.jboss.marshalling;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.StreamCorruptedException;

/**
 * An object table that multiplexes up to 256 class tables.  The protocol works by prepending the custom object table
 * with an identifier byte.
 */
public class ChainingObjectTable implements ObjectTable {
    private static Pair<ObjectTable, Writer> pair(final ObjectTable objectTable, final Writer writer) {
        return Pair.create(objectTable, writer);
    }

    private final List<Pair<ObjectTable, Writer>> writers;
    private final ObjectTable[] readers;

    /**
     * Construct a new instance.  The given array may be sparse, but it may not be more than
     * 256 elements in length.  Object tables are checked in order of increasing array index.
     *
     * @param objectTables the object tables to delegate to
     */
    public ChainingObjectTable(final ObjectTable[] objectTables) {
        if (objectTables == null) {
            throw new NullPointerException("objectTables is null");
        }
        readers = objectTables.clone();
        if (readers.length > 256) {
            throw new IllegalArgumentException("Object table array is too long (limit is 256 elements)");
        }
        writers = new ArrayList<Pair<ObjectTable, Writer>>();
        for (int i = 0; i < readers.length; i++) {
            final ObjectTable objectTable = readers[i];
            if (objectTable != null) {
                final int idx = i;
                writers.add(pair(objectTable, new Writer() {
                    public void writeObject(final Marshaller marshaller, final Object obj) throws IOException {
                        marshaller.writeByte(idx);
                        objectTable.getObjectWriter(obj).writeObject(marshaller, obj);
                    }
                }));
            }
        }
    }

    /** {@inheritDoc} */
    public Writer getObjectWriter(final Object obj) throws IOException {
        for (Pair<ObjectTable, Writer> entry : writers) {
            final ObjectTable table = entry.getA();
            final Writer writer = entry.getB();
            if (table.getObjectWriter(obj) != null) {
                return writer;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public Object readObject(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        final int v = unmarshaller.readByte() & 0xff;
        final ObjectTable table = readers[v];
        if (table == null) {
            throw new StreamCorruptedException(String.format("Unknown object table ID %02x encountered", Integer.valueOf(v)));
        }
        return table.readObject(unmarshaller);
    }
}