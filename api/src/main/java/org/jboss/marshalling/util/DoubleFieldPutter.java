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

package org.jboss.marshalling.util;

import org.jboss.marshalling.Marshaller;
import java.io.IOException;

/**
 * A field putter for double-type fields.
 */
public class DoubleFieldPutter extends FieldPutter {
    private double value;

    /** {@inheritDoc} */
    public void write(final Marshaller marshaller) throws IOException {
        marshaller.writeDouble(value);
    }

    /** {@inheritDoc} */
    public Kind getKind() {
        return Kind.DOUBLE;
    }

    /** {@inheritDoc} */
    public double getDouble() {
        return value;
    }

    /** {@inheritDoc} */
    public void setDouble(final double value) {
        this.value = value;
    }
}