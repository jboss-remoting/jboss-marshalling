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

package org.jboss.marshalling.util;

/**
 * The kind of field.
 */
public enum Kind {
    /**
     * Field of type {@code boolean}.
     */
    BOOLEAN,
    /**
     * Field of type {@code byte}.
     */
    BYTE,
    /**
     * Field of type {@code char}.
     */
    CHAR,
    /**
     * Field of type {@code double}.
     */
    DOUBLE,
    /**
     * Field of type {@code float}.
     */
    FLOAT,
    /**
     * Field of type {@code int}.
     */
    INT,
    /**
     * Field of type {@code long}.
     */
    LONG,
    /**
     * Field of object type.
     */
    OBJECT,
    /**
     * Field of type {@code short}.
     */
    SHORT,
    ;
}
