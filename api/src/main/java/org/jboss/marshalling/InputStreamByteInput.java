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

package org.jboss.marshalling;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * An {@code InputStream} implementing {@code ByteInput} which reads input from another {@code InputStream}.
 * Usually the {@link Marshalling#createInputStream(ByteInput)} method should be used to create instances because
 * it can detect when the target already extends {@code InputStream}.
 */
public class InputStreamByteInput extends FilterInputStream implements ByteInput {

    /**
     * Construct a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public InputStreamByteInput(final InputStream inputStream) {
        super(inputStream);
    }
}
