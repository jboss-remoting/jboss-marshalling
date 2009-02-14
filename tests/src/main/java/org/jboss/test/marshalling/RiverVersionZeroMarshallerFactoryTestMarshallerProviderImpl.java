/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
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
