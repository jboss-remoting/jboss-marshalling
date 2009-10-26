/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

import org.jboss.marshalling.ProviderDescriptor;
import org.jboss.marshalling.MarshallerFactory;

/**
 * The River implementation of the provider descriptor interface.
 *
 * @apiviz.exclude
 */
public final class RiverProviderDescriptor implements ProviderDescriptor {

    private static final MarshallerFactory MARSHALLER_FACTORY = new RiverMarshallerFactory();
    private static final int[] VERSIONS;

    static {
        final int range = Protocol.MAX_VERSION - Protocol.MIN_VERSION + 1;
        final int[] versions = new int[range];
        for (int i = 0; i < range; i ++) {
            versions[i] = Protocol.MAX_VERSION - i;
        }
        VERSIONS = versions;
    }

    /** {@inheritDoc} */
    public String getName() {
        return "river";
    }

    /** {@inheritDoc} */
    public int[] getSupportedVersions() {
        return VERSIONS.clone();
    }

    /** {@inheritDoc} */
    public MarshallerFactory getMarshallerFactory() {
        return MARSHALLER_FACTORY;
    }
}
