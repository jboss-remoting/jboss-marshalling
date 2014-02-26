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

package org.jboss.test.marshalling;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EqualableArrayDeque<X> extends ArrayDeque<X> {

    private static final long serialVersionUID = 5029699446105430182L;

    public boolean equals(final Object obj) {
        return obj instanceof EqualableArrayDeque && equals((EqualableArrayDeque<?>)obj);
    }

    public boolean equals(final EqualableArrayDeque<?> obj) {
        Iterator<X> i1 = iterator();
        Iterator<?> i2 = obj.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            X o1 = i1.next();
            Object o2 = i2.next();
            if (o1 == null ? o2 != null : ! o1.equals(o2)) {
                return false;
            }
        }
        return i1.hasNext() == i2.hasNext();
    }
}
