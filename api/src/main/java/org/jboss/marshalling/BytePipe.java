/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * A paired {@link ByteInput} and {@link ByteOutput}.  Each end must be used from a different thread, otherwise a deadlock
 * condition will occur.
 */
public final class BytePipe {
    private final ByteInput input;
    private final ByteOutput output;

    /**
     * Construct a new instance.
     */
    public BytePipe() {
        final PipedOutputStream output = new PipedOutputStream();
        final PipedInputStream input;
        try {
            input = new PipedInputStream(output);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.input = Marshalling.createByteInput(input);
        this.output = Marshalling.createByteOutput(output);
    }

    /**
     * Get the input side of this pipe.
     *
     * @return the input side
     */
    public ByteInput getInput() {
        return input;
    }

    /**
     * Get the output side of this pipe.
     *
     * @return the output side
     */
    public ByteOutput getOutput() {
        return output;
    }
}
