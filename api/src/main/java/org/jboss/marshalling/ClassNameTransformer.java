/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
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
 * A class name transformer. Allows to remap one java type name to another java type name.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ClassNameTransformer {

    /**
     * Transforms unmarshaller input stream java type names.
     * @param className candidate to be translated
     * @return either original or new class name
     */
    String transformInput(String className);

    /**
     * Transforms marshaller output stream java type names.
     * @param className candidate to be translated
     * @return either original or new class name
     */
    String transformOutput(String className);

    /**
     * Translates all incoming Java EE 8- types to Jakarta EE 9+ types and all outgoing Jakarta EE 9+ types to Java EE 8- types.
     */
    ClassNameTransformer JAVAEE_TO_JAKARTAEE = new JavaEE2JakartaEEClassNameTransformer();

}
