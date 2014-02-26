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
