/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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
