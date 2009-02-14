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
        return 1;
    }
}
