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

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.ByteInput;
import java.io.IOException;

/**
 *
 */
public final class RiverVersionZeroMarshallerFactoryTestMarshallerProviderImpl extends MarshallerFactoryTestMarshallerProvider {

    public RiverVersionZeroMarshallerFactoryTestMarshallerProviderImpl(final MarshallerFactory factory) {
        super(factory, 0);
    }

    public Marshaller create(final MarshallingConfiguration config, final ByteOutput target) throws IOException {
        final StreamHeader header = config.getStreamHeader();
        config.setStreamHeader(new StreamHeader() {
            public void readHeader(final ByteInput input) throws IOException {
                if (header != null) header.readHeader(input);
            }

            public void writeHeader(final ByteOutput output) throws IOException {
                if (header != null) header.writeHeader(output);
                output.write(0);
            }
        });
        return super.create(config, target);
    }
}
