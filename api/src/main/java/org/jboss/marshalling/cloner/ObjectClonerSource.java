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
 * A source for object cloners.  Use in applications where the configuration of cloners must be abstracted
 * away from their creation.
 */
public interface ObjectClonerSource {

    /**
     * Create a new object cloner.
     *
     * @return a new object cloner
     */
    ObjectCloner createNew();

    /**
     * An object cloner which returns the identity cloner.
     *
     * @see ObjectCloner#IDENTITY
     */
    ObjectClonerSource IDENTITY = new ObjectClonerSource() {
        public ObjectCloner createNew() {
            return ObjectCloner.IDENTITY;
        }
    };
}
