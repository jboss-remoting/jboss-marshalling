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
