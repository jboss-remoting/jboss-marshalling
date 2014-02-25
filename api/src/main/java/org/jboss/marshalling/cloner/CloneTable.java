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

/**
 * An interface which allows extending a cloner to types that it would not otherwise support.
 */
public interface CloneTable {

    /**
     * Attempt to clone the given object.  If no clone can be made or acquired from this table, return {@code null}.
     *
     * @param original the original
     * @param objectCloner the object cloner
     * @param classCloner the class cloner
     * @return the clone or {@code null} if none can be acquired
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class is not found
     */
    Object clone(Object original, ObjectCloner objectCloner, ClassCloner classCloner) throws IOException, ClassNotFoundException;

    /**
     * A null clone table.
     */
    CloneTable NULL = new CloneTable() {
        public Object clone(final Object original, final ObjectCloner objectCloner, final ClassCloner classCloner) throws IOException, ClassNotFoundException {
            return null;
        }
    };
}
