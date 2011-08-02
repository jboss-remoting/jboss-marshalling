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

package org.jboss.marshalling.util;

import org.jboss.marshalling.Marshaller;
import java.io.IOException;

/**
 * A field putter for int-type fields.
 */
public class IntFieldPutter extends FieldPutter {
    private int value;

    /** {@inheritDoc} */
    public void write(final Marshaller marshaller) throws IOException {
        marshaller.writeInt(value);
    }

    /** {@inheritDoc} */
    public Kind getKind() {
        return Kind.INT;
    }

    /** {@inheritDoc} */
    public int getInt() {
        return value;
    }

    /** {@inheritDoc} */
    public void setInt(final int value) {
        this.value = value;
    }
}