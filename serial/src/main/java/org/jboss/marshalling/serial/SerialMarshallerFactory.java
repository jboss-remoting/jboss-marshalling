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
            (byte) 0xac, (byte) 0xed, 0x00, 0x05,
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

    public Unmarshaller createUnmarshaller(final MarshallingConfiguration configuration) throws IOException {
        return new SerialUnmarshaller(this, registry, configuration);
    }

    public Marshaller createMarshaller(final MarshallingConfiguration configuration) throws IOException {
        return new SerialMarshaller(this, registry, configuration);
    }
}
