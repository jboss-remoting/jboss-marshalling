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
