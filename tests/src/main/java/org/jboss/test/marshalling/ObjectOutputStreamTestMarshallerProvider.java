/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
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

package org.jboss.test.marshalling;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.ObjectOutputStreamMarshaller;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.ByteOutput;
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
            return objectResolver.writeReplace(obj);
        }
    }
}