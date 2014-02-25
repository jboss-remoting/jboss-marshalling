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

package org.jboss.marshalling.river;

import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.MarshallingConfiguration;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The River marshaller factory implementation.
 */
public class RiverMarshallerFactory extends AbstractMarshallerFactory {
    private final SerializableClassRegistry registry;

    /**
     * Construct a new instance of a River marshaller factory.
     */
    public RiverMarshallerFactory() {
        registry = AccessController.doPrivileged(new PrivilegedAction<SerializableClassRegistry>() {
            public SerializableClassRegistry run() {
                return SerializableClassRegistry.getInstance();
            }
        });
    }

    /** {@inheritDoc} */
    public Unmarshaller createUnmarshaller(final MarshallingConfiguration configuration) throws IOException {
        return new RiverUnmarshaller(this, registry, configuration);
    }

    /** {@inheritDoc} */
    public Marshaller createMarshaller(final MarshallingConfiguration configuration) throws IOException {
        return new RiverMarshaller(this, registry, configuration);
    }

    protected int getDefaultVersion() {
        return 3;
    }
}
