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

import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.MarshallerFactory;
import java.io.IOException;

/**
 *
 */
public final class MarshallerFactoryTestUnmarshallerProvider implements TestUnmarshallerProvider {
    private final MarshallerFactory marshallerFactory;
    private final int version;

    public MarshallerFactoryTestUnmarshallerProvider(final MarshallerFactory factory) {
        marshallerFactory = factory;
        version = -1;
    }

    public MarshallerFactoryTestUnmarshallerProvider(final MarshallerFactory factory, final int version) {
        marshallerFactory = factory;
        this.version = version;
    }

    public Unmarshaller create(final MarshallingConfiguration config, final ByteInput source) throws IOException {
        if (version != -1) {
            config.setVersion(version);
        }
        final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(config);
        unmarshaller.start(source);
        return unmarshaller;
    }
}
