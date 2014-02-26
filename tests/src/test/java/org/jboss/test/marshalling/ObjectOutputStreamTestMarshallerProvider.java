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

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.ObjectOutputStreamMarshaller;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Externalize;
import org.testng.SkipException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 *
 */
public final class ObjectOutputStreamTestMarshallerProvider implements TestMarshallerProvider {

    public Marshaller create(final MarshallingConfiguration config, final ByteOutput target) throws IOException {
        final MyObjectOutputStream ois = new MyObjectOutputStream(config, Marshalling.createOutputStream(target));
        return ois.marshaller;
    }

    private static final class MyObjectOutputStream extends ObjectOutputStream {
        private final ObjectResolver objectResolver;
        private final ClassResolver classResolver;
        private final Marshaller marshaller;

        private MyObjectOutputStream(final MarshallingConfiguration config, final OutputStream out) throws IOException {
            super(out);
            if (config.getClassTable() != null) {
                throw new SkipException("class tables not supported");
            }
            if (config.getObjectTable() != null) {
                throw new SkipException("object tables not supported");
            }
            if (config.getClassExternalizerFactory() != null) {
                throw new SkipException("externalizers not supported");
            }
            final ObjectResolver objectResolver = config.getObjectResolver();
            this.objectResolver = objectResolver == null ? Marshalling.nullObjectResolver() : objectResolver;
            final ClassResolver classResolver = config.getClassResolver();
            this.classResolver = classResolver == null ? new SimpleClassResolver(getClass().getClassLoader()) : classResolver;
            enableReplaceObject(true);
            //noinspection ThisEscapedInObjectConstruction
            marshaller = new ObjectOutputStreamMarshaller(this);
        }

        protected void annotateProxyClass(final Class<?> cl) throws IOException {
            classResolver.annotateProxyClass(marshaller, cl);
        }

        protected void annotateClass(final Class<?> cl) throws IOException {
            classResolver.annotateClass(marshaller, cl);
        }

        protected Object replaceObject(final Object obj) throws IOException {
            if (obj.getClass().getAnnotation(Externalize.class) != null) {
                throw new SkipException("@Externalize object serialization not supported");
            }
            return objectResolver.writeReplace(obj);
        }
    }
}