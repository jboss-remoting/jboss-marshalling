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

import java.io.IOException;

/**
 * The main marshaller factory.
 * @apiviz.landmark
 */
public interface MarshallerFactory {
    /**
     * Create an unmarshaller from this configuration.
     *
     * @return an unmarshaller
     * @param configuration the marshalling configuration to use
     * @throws IOException if an error occurs
     */
    Unmarshaller createUnmarshaller(MarshallingConfiguration configuration) throws IOException;

    /**
     * Create a marshaller from this configuration.
     *
     * @return a marshaller
     * @param configuration the marshalling configuration to use
     * @throws IOException if an error occurs
     */
    Marshaller createMarshaller(MarshallingConfiguration configuration) throws IOException;
}
