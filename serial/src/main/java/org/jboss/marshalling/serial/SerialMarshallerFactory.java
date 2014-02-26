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

package org.jboss.marshalling.serial;

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 *
 */
public final class SerialMarshallerFactory extends AbstractMarshallerFactory implements MarshallerFactory {
    private final SerializableClassRegistry registry;

    private static final StreamHeader defaultHeader = Marshalling.streamHeader(new byte[] {
            (byte) 0xac, (byte) 0xed
    });

    /**
     * Construct a new instance of a River marshaller factory.
     */
    public SerialMarshallerFactory() {
        registry = AccessController.doPrivileged(new PrivilegedAction<SerializableClassRegistry>() {
            public SerializableClassRegistry run() {
                return SerializableClassRegistry.getInstance();
            }
        });
    }

    protected StreamHeader getDefaultStreamHeader() {
        return defaultHeader;
    }

    protected int getDefaultVersion() {
        return 5;
    }

    public Unmarshaller createUnmarshaller(final MarshallingConfiguration configuration) throws IOException {
        return new SerialUnmarshaller(this, registry, configuration);
    }

    public Marshaller createMarshaller(final MarshallingConfiguration configuration) throws IOException {
        return new SerialMarshaller(this, registry, configuration);
    }
}
