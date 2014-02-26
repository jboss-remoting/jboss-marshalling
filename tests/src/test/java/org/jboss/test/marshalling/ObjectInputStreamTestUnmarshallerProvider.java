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

package org.jboss.test.marshalling;

import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.ObjectInputStreamUnmarshaller;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.SimpleClassResolver;
import org.testng.SkipException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.ObjectStreamClass;

/**
 *
 */
public final class ObjectInputStreamTestUnmarshallerProvider implements TestUnmarshallerProvider {

    public Unmarshaller create(final MarshallingConfiguration config, final ByteInput source) throws IOException {
        final MyObjectInputStream ois = new MyObjectInputStream(config, Marshalling.createInputStream(source));
        return ois.unmarshaller;
    }

    private static final class MyObjectInputStream extends ObjectInputStream {
        private final ObjectResolver objectResolver;
        private final ClassResolver classResolver;
        private final Unmarshaller unmarshaller;

        private MyObjectInputStream(final MarshallingConfiguration config, final InputStream in) throws IOException {
            super(in);
            if (config.getClassTable() != null) {
                throw new SkipException("class tables not supported");
            }
            if (config.getObjectTable() != null) {
                throw new SkipException("object tables not supported");
            }
            final ObjectResolver objectResolver = config.getObjectResolver();
            this.objectResolver = objectResolver == null ? Marshalling.nullObjectResolver() : objectResolver;
            final ClassResolver classResolver = config.getClassResolver();
            this.classResolver = classResolver == null ? new SimpleClassResolver(getClass().getClassLoader()) : classResolver;
            enableResolveObject(true);
            //noinspection ThisEscapedInObjectConstruction
            unmarshaller = new ObjectInputStreamUnmarshaller(this);
        }

        protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return classResolver.resolveClass(unmarshaller, desc.getName(), desc.getSerialVersionUID());
        }

        protected Class<?> resolveProxyClass(final String[] interfaces) throws IOException, ClassNotFoundException {
            return classResolver.resolveProxyClass(unmarshaller, interfaces);
        }

        protected Object resolveObject(final Object obj) throws IOException {
            return objectResolver.readResolve(obj);
        }
    }
}
