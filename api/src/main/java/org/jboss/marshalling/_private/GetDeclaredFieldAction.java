/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

package org.jboss.marshalling._private;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;

/**
 */
public class GetDeclaredFieldAction implements PrivilegedAction<Field> {
    private final Class<?> clazz;
    private final String name;

    public GetDeclaredFieldAction(final Class<?> clazz, final String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public Field run() {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new NoSuchFieldError(e.getMessage());
        }
    }
}
