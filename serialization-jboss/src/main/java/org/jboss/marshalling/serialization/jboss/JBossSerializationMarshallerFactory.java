/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.marshalling.serialization.jboss;

import java.io.IOException;

import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.ClassExternalizerFactory;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Jan 29, 2009
 * </p>
 */
public final class JBossSerializationMarshallerFactory extends AbstractMarshallerFactory {

   public JBossSerializationMarshallerFactory() {
   }

   public Marshaller createMarshaller(MarshallingConfiguration configuration) throws IOException {
      return new JBossSerializationMarshaller(this, configuration);
   }

   public Unmarshaller createUnmarshaller(MarshallingConfiguration configuration) throws IOException {
      return new JBossSerializationUnmarshaller(this, configuration);
   }

   protected ExternalizerFactory getDefaultExternalizerFactory() {
      return null;
   }

   protected ClassExternalizerFactory getDefaultClassExternalizerFactory() {
      return null;
   }

   protected StreamHeader getDefaultStreamHeader() {
      return null;
   }

   protected ClassTable getDefaultClassTable() {
      return null;
   }

   protected ClassResolver getDefaultClassResolver() {
      return null;
   }
   protected ObjectTable getDefaultObjectTable() {
      return null;
   }

   protected ObjectResolver getDefaultObjectResolver() {
      return null;
   }
}
