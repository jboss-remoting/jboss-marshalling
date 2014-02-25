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
