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

package org.jboss.marshalling;

/**
 * A provider descriptor for automatically-discovered marshalling factory types.  Since instances of this interface are
 * constructed automatically, implementing classes should have a no-arg constructor.
 * <p>
 * To add an automatically-discovered marshaller, create a file called {@code "META-INF/services/org.jboss.marshalling.ProviderDescriptor"}
 * and populate it with the names of classes that implement this interface.
 *
 * @see java.util.ServiceLoader
 * @apiviz.landmark
 */
public interface ProviderDescriptor {

    /**
     * Get the name of this provider.
     *
     * @return the provider name
     */
    String getName();

    /**
     * Get the supported wire protocol versions for this provider, in descending order.
     *
     * @return the supported versions in descending order
     */
    int[] getSupportedVersions();

    /**
     * Get the marshaller factory instance for this provider.
     *
     * @return the marshaller factory
     */
    MarshallerFactory getMarshallerFactory();
}
