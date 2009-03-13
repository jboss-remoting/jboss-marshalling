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

import static org.jboss.marshalling.serialization.jboss.Protocol.ID_EXTERNALIZER_CLASS;
import static org.jboss.marshalling.serialization.jboss.Protocol.ID_NEW_OBJECT;
import static org.jboss.marshalling.serialization.jboss.Protocol.ID_NO_CLASS_DESC;
import static org.jboss.marshalling.serialization.jboss.Protocol.ID_ORDINARY_CLASS;
import static org.jboss.marshalling.serialization.jboss.Protocol.ID_PREDEFINED_CLASS;
import static org.jboss.marshalling.serialization.jboss.Protocol.ID_PREDEFINED_OBJECT;
import static org.jboss.marshalling.serialization.jboss.Protocol.ID_PROXY_CLASS;
import static org.jboss.marshalling.serialization.jboss.Protocol.ID_PROXY_OBJECT;

import java.io.IOException;
import java.io.InvalidClassException;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.marshalling.ClassExternalizerFactory;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Externalize;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.serial.classmetamodel.ClassMetaData;
import org.jboss.serial.classmetamodel.DefaultClassDescriptorStrategy;
import org.jboss.serial.io.JBossObjectOutputStreamSharedTree;
import org.jboss.serial.objectmetamodel.DefaultObjectDescriptorStrategy;
import org.jboss.serial.objectmetamodel.ObjectsCache;
import org.jboss.serial.objectmetamodel.ObjectsCache.JBossSeralizationOutputInterface;
import org.jboss.serial.util.StringUtilBuffer;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Jan 28, 2009
 * </p>
 */
public class JBossSerializationOutputStream extends JBossObjectOutputStreamSharedTree
{   
   private static final boolean DONT_WRITE_CLASS_NAME = false;
   private static final boolean WRITE_CLASS_NAME = true;
   
   private JBossSerializationMarshaller marshaller;
   private StreamHeader streamHeader;
   private ClassResolver classResolver;
   private ObjectResolver objectResolver;
   private ClassExternalizerFactory classExternalizerFactory;
   private ClassTable classTable;
   private ObjectTable objectTable;

   private boolean nativeImmutableHandling = true;
   
   private boolean readyForStreamHeader;
   
   public JBossSerializationOutputStream(boolean checkSerializableClass,
                                         StringUtilBuffer buffer,
                                         JBossSerializationMarshaller marshaller,
                                         StreamHeader streamHeader,
                                         ClassResolver classResolver,
                                         ClassTable classTable,
                                         ObjectResolver objectResolver,
                                         ObjectTable objectTable,
                                         ClassExternalizerFactory classExternalizerFactory,
                                         boolean nativeImmutableHandling,
                                         boolean jbossSerializationCompatible) throws IOException {
      super(marshaller.getOutputStream(), checkSerializableClass, buffer);
      this.marshaller = marshaller;
      this.streamHeader = streamHeader;
      this.classResolver = classResolver;
      this.classTable = classTable;
      this.objectResolver = objectResolver;
      this.objectTable = objectTable;
      this.classExternalizerFactory = classExternalizerFactory;
      this.nativeImmutableHandling = nativeImmutableHandling;
      
      if (jbossSerializationCompatible) {
         setClassDescriptorStrategy(new DefaultClassDescriptorStrategy());
         setObjectDescriptorStrategy(new DefaultObjectDescriptorStrategy());
         setStandardReplacement(false);
      } else {
         setClassDescriptorStrategy(new JBMClassDescriptorStrategy(marshaller, this));
         setObjectDescriptorStrategy(new JBMObjectDescriptorStrategy(marshaller, this));
         setStandardReplacement(true);
      }
      
      if (objectResolver != null) {
         AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
               enableReplaceObject(true);
               return null;
            }
         });
      }
      
      readyForStreamHeader = true;
   }
   
   protected void annotateClass(Class<?> cl) throws IOException {
      if (classResolver != null) {
         classResolver.annotateClass(marshaller, cl);
      }
   }
   
   protected void annotateProxyClass(Class<?> cl) throws IOException {
      if (classResolver != null) {
         classResolver.annotateProxyClass(marshaller, cl);
      }
   }

   public void clear() throws IOException {
      reset();
   }

   protected void completeConstruction() throws IOException {
      writeStreamHeader();
   }
   
   public boolean isNativeImmutableHandling() {
      return nativeImmutableHandling;
   }

   protected Object replaceObject(Object obj) throws IOException {
      if (objectResolver != null) {
          return objectResolver.writeReplace(obj);
      }
      else {
         return super.replaceObject(obj);
      }
   }
   
   public void setNativeImmutableHandling(boolean nativeImmutableHandling) {
      this.nativeImmutableHandling = nativeImmutableHandling;
   }
   
   public void writeStreamHeader() throws IOException {
      if (readyForStreamHeader) {
         if (streamHeader != null) {
            streamHeader.writeHeader(marshaller);
         }
         else {
            super.writeStreamHeader();
         }
      }
   }

   public void writeUnshared(Object obj) throws IOException {
      throw new UnsupportedOperationException();
   }
   
   static class JBMClassDescriptorStrategy extends DefaultClassDescriptorStrategy {
      private JBossSerializationMarshaller marshaller;
      private JBossSerializationOutputStream output;
      private ClassTable classTable;
      private ClassResolver classResolver;
      private ObjectTable objectTable;
      
      public JBMClassDescriptorStrategy(JBossSerializationMarshaller marshaller, JBossSerializationOutputStream output) {
         this.marshaller = marshaller;
         this.output = output;
         this.classTable = output.classTable;
         this.classResolver = output.classResolver;
         this.objectTable = output.objectTable;
      }

      public void writeClassDescription(Object obj, ClassMetaData metaData, ObjectsCache cache, int description) throws IOException {
         if (objectTable != null && objectTable.getObjectWriter(obj) != null) {
            output.write(ID_NO_CLASS_DESC);
            return;
         }
         Class<? extends Object> clazz = obj.getClass();
         ClassTable.Writer writer = null;
         if (classTable != null && (writer = classTable.getClassWriter(clazz)) != null) {
            output.write(ID_PREDEFINED_CLASS);
            writer.writeClass(marshaller, clazz);
         } else if (classResolver != null) {
            if (metaData.isProxy()) {
               output.write(ID_PROXY_CLASS);
               final String[] names = classResolver.getProxyInterfaces(clazz);
               output.writeInt(names.length);
               for (String name : names) {
                   output.writeUTF(name);
               }
               classResolver.annotateProxyClass(marshaller, clazz);
            } else {
               output.write(ID_ORDINARY_CLASS);
               String className = metaData.getClassName();
               String replacementClassName = classResolver.getClassName(clazz);
               if (className.equals(replacementClassName)) {
                  output.writeUTF(className);
                  classResolver.annotateClass(marshaller, clazz);
                  super.writeClassDescription(null, metaData, cache, description, DONT_WRITE_CLASS_NAME);
               } else {
                  output.writeUTF(replacementClassName);
                  classResolver.annotateClass(marshaller, clazz);
                  try
                  {
                     ClassMetaData replacementMetaData = new ClassMetaData(Class.forName(replacementClassName));
                     super.writeClassDescription(null, replacementMetaData, cache, description, DONT_WRITE_CLASS_NAME);
                  }
                  catch (ClassNotFoundException e) {
                     throw new IOException(e.getMessage());
                  }
               }
            }
         } else {
            output.write(ID_ORDINARY_CLASS);
            super.writeClassDescription(clazz, metaData, cache, description, WRITE_CLASS_NAME);
         }
      }
   }
   
   static class JBMObjectDescriptorStrategy extends DefaultObjectDescriptorStrategy {
      private JBossSerializationMarshaller marshaller;
      private ClassResolver classResolver;
      private ObjectTable objectTable; 
      private ClassExternalizerFactory classExternalizerFactory; 
      private boolean nativeImmutableHandling;
      
      public JBMObjectDescriptorStrategy(JBossSerializationMarshaller marshaller, JBossSerializationOutputStream output) {
         this.marshaller = marshaller;
         this.classResolver = output.classResolver;
         this.objectTable = output.objectTable;
         this.classExternalizerFactory = output.classExternalizerFactory;
         this.nativeImmutableHandling = output.nativeImmutableHandling;
      }
      
      public boolean doneReplacing(ObjectsCache cache, Object newObject, Object oldObject, ClassMetaData oldMetaData) throws IOException {
         return true;
      }
      
      public void writeObject(JBossSeralizationOutputInterface output, ObjectsCache cache, ClassMetaData metadata, Object obj) throws IOException {
         if (classResolver != null && Proxy.isProxyClass(obj.getClass())) {
            output.write(ID_PROXY_OBJECT);
            output.writeObject(Proxy.getInvocationHandler(obj));
            return;
         }
         Externalizer externalizer = null;
         Externalize annotation = null;
         if (classExternalizerFactory != null) {
            externalizer = classExternalizerFactory.getExternalizer(obj.getClass());
         }
         if (externalizer == null && obj != null) {
            annotation = obj.getClass().getAnnotation(Externalize.class);
         }
         if (externalizer == null && annotation != null) {
            final Class<? extends Externalizer> clazz = annotation.value();
            try {
               externalizer = clazz.newInstance();
            } catch (InstantiationException e) {
               final InvalidClassException ice = new InvalidClassException(obj.getClass().getName(), "Error instantiating externalizer \"" + clazz.getName() + "\"");
               ice.initCause(e);
               throw ice;
            } catch (IllegalAccessException e) {
               final InvalidClassException ice = new InvalidClassException(obj.getClass().getName(), "Illegal access instantiating externalizer \"" + clazz.getName() + "\"");
               ice.initCause(e);
               throw ice;
            }
         }
         if (externalizer != null) {
            output.write(ID_EXTERNALIZER_CLASS);
            marshaller.writeObject(externalizer);
            externalizer.writeExternal(obj, output);
            return;
         }
         output.write(ID_NEW_OBJECT);
         super.writeObject(output, cache, metadata, obj);
      }
      
      public boolean writeObjectSpecialCase(JBossSeralizationOutputInterface output, ObjectsCache cache, Object obj) throws IOException {
         if (objectTable != null) {
            ObjectTable.Writer writer = objectTable.getObjectWriter(obj);
            if (writer != null) {
               output.write(ID_PREDEFINED_OBJECT);
               writer.writeObject(marshaller, obj);
               return true;
            }
         }
         if (nativeImmutableHandling) {
            return super.writeObjectSpecialCase(output, cache, obj);
         }
         return false;
      }
   }
}
