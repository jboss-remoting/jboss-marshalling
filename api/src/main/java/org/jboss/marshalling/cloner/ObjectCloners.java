/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling.cloner;

/**
 * A class which may be used to got cloner factory instances.
 */
public final class ObjectCloners {

    private static final ObjectClonerFactory CLONEABLE = new ObjectClonerFactory() {
        public ObjectCloner createCloner(final ClonerConfiguration configuration) {
            return new CloneableCloner(configuration);
        }
    };

    private static final ObjectClonerFactory SERIALIZING = new ObjectClonerFactory() {
        public ObjectCloner createCloner(final ClonerConfiguration configuration) {
            return new SerializingCloner(configuration);
        }
    };

    private ObjectCloners() {
    }

    /**
     * Get the cloneable object cloner factory.
     *
     * @return the cloneable object cloner factory
     */
    public static ObjectClonerFactory getCloneableObjectClonerFactory() {
        return CLONEABLE;
    }

    /**
     * Get the serializing object cloner factory.
     *
     * @return the serializing object cloner factory
     */
    public static ObjectClonerFactory getSerializingObjectClonerFactory() {
        return SERIALIZING;
    }

    /**
     * Get an object cloner source which creates cloners with a static configuration.
     *
     * @param factory the cloner factory to use
     * @param configuration the configuration to use for all cloners
     * @return the cloner source
     */
    public static ObjectClonerSource createObjectClonerSource(final ObjectClonerFactory factory, final ClonerConfiguration configuration) {
        final ClonerConfiguration finalConfig = configuration.clone();
        return new ObjectClonerSource() {
            public ObjectCloner createNew() {
                return factory.createCloner(finalConfig);
            }
        };
    }
}
