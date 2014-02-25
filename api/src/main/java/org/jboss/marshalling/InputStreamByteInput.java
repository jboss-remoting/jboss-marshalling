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
