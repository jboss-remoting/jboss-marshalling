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

import java.io.IOException;

/**
 * A writer for class or object tables which simply writes a flat sequence of bytes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ByteWriter implements ObjectTable.Writer, ClassTable.Writer {
    private final byte[] bytes;

    /**
     * Construct a new instance.
     *
     * @param bytes the bytes to write
     */
    public ByteWriter(final byte... bytes) {
        this.bytes = bytes;
    }

    public void writeObject(final Marshaller marshaller, final Object object) throws IOException {
        marshaller.write(bytes);
    }

    public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
        marshaller.write(bytes);
    }
}
