/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.marshalling.cloner;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DateFieldType implements Serializable {

    private static final long serialVersionUID = -884445687188992945L;

    private final Date date;
    private transient boolean foo;

    public DateFieldType(final Date date) {
        this.date = date;
    }

    public DateFieldType(final Date date, final boolean foo) {
        this.date = date;
        this.foo = foo;
    }

    public Date getDate() {
        return date;
    }

    public boolean equals(Object other) {
        return other instanceof DateFieldType && equals((DateFieldType)other);
    }

    public boolean equals(DateFieldType other) {
        return this == other || other != null && date.equals(other.date) && foo == other.foo;
    }

    public int hashCode() {
        return date.hashCode() ^ Boolean.valueOf(foo).hashCode();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        foo = ois.readBoolean();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeBoolean(foo);
    }
}
