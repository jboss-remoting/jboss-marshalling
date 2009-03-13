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
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.serialization.java.ObjectResolverWrapper;
import org.jboss.serial.classmetamodel.ClassMetaData;
import org.jboss.serial.classmetamodel.ClassMetaDataSlot;
import org.jboss.serial.classmetamodel.DefaultClassDescriptorStrategy;
import org.jboss.serial.classmetamodel.StreamingClass;
import org.jboss.serial.io.JBossObjectInputStreamSharedTree;
import org.jboss.serial.objectmetamodel.DefaultObjectDescriptorStrategy;
import org.jboss.serial.objectmetamodel.ObjectsCache;
import org.jboss.serial.objectmetamodel.ObjectsCache.JBossSeralizationInputInterface;
import org.jboss.serial.util.HashStringUtil;
import org.jboss.serial.util.StringUtilBuffer;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Jan 29, 2009
 * </p>
 */
public class JBossSerializationInputStream extends JBossObjectInputStreamSharedTree {
   
   private JBossSerializationUnmarshaller unmarshaller;
   private StreamHeader streamHeader;
   private ClassResolver classResolver;
   private ClassTable classTable;
   private ObjectResolver objectResolver;
   private ObjectTable objectTable;
   private Creator creator;
   private ThreadLocal<String> resolvedClassName = new ThreadLocal<String>();
   private ThreadLocal<Class<?>> resolvedClass = new ThreadLocal<Class<?>>();
   private boolean readyForStreamHeader;
   
   public JBossSerializationInputStream(JBossSerializationUnmarshaller unmarshaller,
                                        StreamHeader streamHeader,
                                        ClassResolver classResolver,
                                        ClassTable classTable,
                                        ObjectResolver objectResolver,
                                        ObjectTable objectTable,
                                        Creator creator, 
                                        boolean jbossSerializationCompatible) throws IOException {
      super(unmarshaller.getInputStream(), new StringUtilBuffer(10024, 10024));
      this.unmarshaller = unmarshaller;
      this.streamHeader = streamHeader;
      this.classResolver = classResolver;
      this.classTable = classTable;
      this.objectResolver = new ObjectResolverWrapper(objectResolver);
      this.objectTable = objectTable;
      this.creator = creator;
      
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
         public Void run() {
            enableResolveObject(true);
            return null;
         }
      });
      
      if (jbossSerializationCompatible) {
         setClassDescriptorStrategy(new DefaultClassDescriptorStrategy());
         setObjectDescriptorStrategy(new DefaultObjectDescriptorStrategy());
         setStandardReplacement(false);
      } else {
         setClassDescriptorStrategy(new JBMClassDescriptorStrategy(unmarshaller, this, classTable, classResolver));
         setObjectDescriptorStrategy(new JBMObjectDescriptorStrategy(unmarshaller, this));
         setStandardReplacement(true);
      }
      
      readyForStreamHeader = true;
   }
   
   public void clear() {
   }
   
   public Creator getCreator() {
      return creator;
   }
   
   public ObjectTable getObjectTable() {
      return objectTable;
   }
   
   public Unmarshaller getUnmarshaller() {
      return unmarshaller;
   }
   
   public Object readUnshared() throws IOException, ClassNotFoundException {
      throw new UnsupportedOperationException();
   }
   
   protected void completeConstruction() throws IOException {
      readStreamHeader();
   }
   
   protected void readStreamHeader() throws IOException {
      if (readyForStreamHeader) {
         if (streamHeader != null) {
            streamHeader.readHeader(unmarshaller);
         } else {
            super.readStreamHeader();
         }
      }
   }
   
   protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      String className = resolvedClassName.get();
      if (className != null && className.equals(desc.getName())) {
         return resolvedClass.get();
      } else {
         return super.resolveClass(desc);
      }
   }
   
   protected Object resolveObject(Object obj) throws IOException {
      return objectResolver.readResolve(obj);
   }
   
   static class JBMClassDescriptorStrategy extends DefaultClassDescriptorStrategy {
      private JBossSerializationUnmarshaller unmarshaller;
      private JBossSerializationInputStream jbsis;
      private ClassTable classTable;
      private ClassResolver classResolver;
      
      public JBMClassDescriptorStrategy(JBossSerializationUnmarshaller unmarshaller, JBossSerializationInputStream jbsis, ClassTable classTable, ClassResolver classResolver) {
         this.unmarshaller = unmarshaller;
         this.jbsis = jbsis;
         this.classTable = jbsis.classTable;
         this.classResolver = jbsis.classResolver;
      }
      
      public StreamingClass readClassDescription(ObjectsCache cache, JBossSeralizationInputInterface input, org.jboss.serial.classmetamodel.ClassResolver passedClassResolver, String ignored) throws IOException {
         byte tag = input.readByte();
         switch (tag) {
            case ID_NO_CLASS_DESC: {
               return null;
            }
            case ID_PREDEFINED_CLASS: {
               try {
                  return new DerivedStreamingClass(classTable.readClass(unmarshaller));
               } catch (ClassNotFoundException e) {
                  throw new IOException(e.getMessage());
               }
            }
            case ID_ORDINARY_CLASS: {
               if (classResolver != null) {
                  String className = input.readUTF();
                  try {
                     Class resolvedClass = classResolver.resolveClass(unmarshaller, className, -1);
                     jbsis.resolvedClassName.set(className);
                     jbsis.resolvedClass.set(resolvedClass);
                     if (className.equals(resolvedClass.getName())) {
                        StreamingClass streamingClass = super.readClassDescription(cache, input, passedClassResolver, className);
                        jbsis.resolvedClassName.set(null);
                        return streamingClass;
                     } else {
                        StreamingClass streamingClass = super.readClassDescription(cache, input, passedClassResolver, className);
                        ClassMetaData metaData = streamingClass.getMetadata();
                        metaData.setClassName(resolvedClass.getName());
                        metaData.setClazz(resolvedClass);
                        metaData.setConstructor(resolvedClass.getConstructor());
                        Long shaHash = HashStringUtil.hashName(resolvedClass.getName());
                        metaData.setShaHash(shaHash);
                        ClassMetaDataSlot slot = metaData.getSlots()[0];
                        slot.setSlotClass(resolvedClass);
                        slot.setShaHash(shaHash);
                        jbsis.resolvedClassName.set(null);
                        return streamingClass;
                     }
                  } catch (ClassNotFoundException e) {
                     throw new IOException(e.getMessage());
                  } catch (NoSuchMethodException e) {
                     throw new IOException(e.getMessage());
                  }
               } else {
                  return super.readClassDescription(cache, input, passedClassResolver, null);
               }
            }
            case ID_PROXY_CLASS: {
               if (classResolver != null) {
                  String[] interfaces = new String[input.readInt()];
                  for (int i = 0; i < interfaces.length; i ++) {
                      interfaces[i] = input.readUTF();
                  }
                  try {
                     Class<?> clazz = classResolver.resolveProxyClass(unmarshaller, interfaces);
                     jbsis.resolvedClassName.set("PROXY");
                     jbsis.resolvedClass.set(clazz);
                     StreamingClass streamingClass = new DerivedStreamingClass(clazz);
                     jbsis.resolvedClassName.set(null);
                     return streamingClass;
                  } catch (ClassNotFoundException e) {
                     StringBuffer sb = new StringBuffer(interfaces[0]);
                     for (int i = 1; i < interfaces.length; i++) {
                        sb.append('|').append(interfaces[i]);
                     }
                     throw new IOException("unable to create proxy: " + sb.toString());
                  }
               } else {
                  return super.readClassDescription(cache, input, passedClassResolver, null);
               }
            }
            default: {
               throw new IOException("unrecognized class type: " + tag);
            }          
         }
      }
   }
   
   static class JBMObjectDescriptorStrategy extends DefaultObjectDescriptorStrategy {
      private Unmarshaller unmarshaller;
      private ClassResolver classResolver;
      private ObjectTable objectTable;
      private Creator creator;
      
      public JBMObjectDescriptorStrategy(Unmarshaller unmarshaller, JBossSerializationInputStream jbsis) {
         this.unmarshaller = unmarshaller;
         this.classResolver = jbsis.classResolver;
         this.objectTable = jbsis.objectTable;
         this.creator = jbsis.creator;
      }
      
      public Object readObjectSpecialCase(JBossSeralizationInputInterface input, ObjectsCache cache, byte byteIdentify) throws IOException {
         switch (byteIdentify) {
            case ID_PREDEFINED_OBJECT: {
               try {
                  return objectTable.readObject(unmarshaller);
               } catch (ClassNotFoundException e) {
                  throw new IOException("class not found: " + e.getMessage());
               }
            }
            default: {
               return super.readObjectSpecialCase(input, cache, byteIdentify);
            }
         }
      }
      
      public Object readObject(JBossSeralizationInputInterface input, ObjectsCache cache, StreamingClass streamingClass, int reference) throws IOException {
         byte tag = input.readByte();
         switch (tag) {
            case ID_EXTERNALIZER_CLASS: {
               try {
                  Externalizer externalizer = (Externalizer) unmarshaller.readObject();
                  Object result = externalizer.createExternal(streamingClass.getMetadata().getClazz(), input, creator);
                  externalizer.readExternal(result, input);
                  cache.putObjectInCacheRead(reference, result);
                  return result;
               } catch (ClassNotFoundException e) {
                  throw new IOException("class not found: " + e.getMessage());
               }
            }
            case ID_PROXY_OBJECT: {
               if (classResolver != null) {
                  try{
                     InvocationHandler handler = (InvocationHandler) input.readObject();
                     Class<?> clazz = streamingClass.getClass();
                     Object obj = Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), handler);
                     cache.putObjectInCacheRead(reference, obj);
                     return obj;
                  } catch (ClassNotFoundException e){
                     throw new IOException("class not found: " + e.getMessage());
                  }
               }
            }
            case ID_NEW_OBJECT: {
               return super.readObject(input, cache, streamingClass, reference);
            }
            default: {
               throw new IOException("unrecognized object type: " + tag);
            }
         }
      }
   }
}
