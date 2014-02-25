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

import java.io.ObjectInput;
import java.io.IOException;
import java.io.ObjectOutput;

/**
 * An externalizer base class which handles object creation in a default fashion.
 */
public abstract class AbstractExternalizer implements Externalizer {
    private static final long serialVersionUID = -584504194617263431L;

    /**
     * Create an instance of a type using the provided creator.
     *
     * @param subjectType the type to create
     * @param input the object input
     * @param defaultCreator the creator
     * @return a new instance
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class could not be located
     */
    public Object createExternal(final Class<?> subjectType, final ObjectInput input, final Creator defaultCreator) throws IOException, ClassNotFoundException {
        return defaultCreator.create(subjectType);
    }

    /** {@inheritDoc}  This default implementation does nothing. */
    public void writeExternal(final Object subject, final ObjectOutput output) throws IOException {
    }

    /** {@inheritDoc}  This default implementation does nothing. */
    public void readExternal(final Object subject, final ObjectInput input) throws IOException, ClassNotFoundException {
    }
}
