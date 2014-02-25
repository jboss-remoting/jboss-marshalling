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
 * A class table that multiplexes up to 256 class tables.  The protocol works by prepending the custom class table
 * with an identifier byte.
 */
public class ChainingClassTable implements ClassTable {

    private static Pair<ClassTable, Writer> pair(final ClassTable classTable, final Writer writer) {
        return Pair.create(classTable, writer);
    }

    private final List<Pair<ClassTable, Writer>> writers;
    private final ClassTable[] readers;

    /**
     * Construct a new instance.  The given array may be sparse, but it may not be more than
     * 256 elements in length.  Class tables are checked in order of increasing array index.
     *
     * @param classTables the class tables to delegate to
     */
    public ChainingClassTable(final ClassTable[] classTables) {
        if (classTables == null) {
            throw new NullPointerException("classTables is null");
        }
        readers = classTables.clone();
        if (readers.length > 256) {
            throw new IllegalArgumentException("Class table array is too long (limit is 256 elements)");
        }
        writers = new ArrayList<Pair<ClassTable, Writer>>();
        for (int i = 0; i < readers.length; i++) {
            final ClassTable classTable = readers[i];
            if (classTable != null) {
                final int idx = i;
                writers.add(pair(classTable, new Writer() {
                    public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
                        marshaller.writeByte(idx);
                        classTable.getClassWriter(clazz).writeClass(marshaller, clazz);
                    }
                }));
            }
        }
    }

    /** {@inheritDoc} */
    public Writer getClassWriter(final Class<?> clazz) throws IOException {
        for (Pair<ClassTable, Writer> entry : writers) {
            final ClassTable table = entry.getA();
            final Writer writer = entry.getB();
            if (table.getClassWriter(clazz) != null) {
                return writer;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        final int v = unmarshaller.readByte() & 0xff;
        final ClassTable table = readers[v];
        if (table == null) {
            throw new StreamCorruptedException(String.format("Unknown class table ID %02x encountered", Integer.valueOf(v)));
        }
        return table.readClass(unmarshaller);
    }
}
