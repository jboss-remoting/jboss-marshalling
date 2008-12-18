
/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.marshalling.serialization.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;

import static org.jboss.marshalling.serialization.java.JavaSerializationConstants.*;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 19, 2008
 * </p>
 */
public class JavaSerializationInputStream extends ObjectInputStream
{
   private static boolean privateFieldsAndMethodsSet;
   
   private static Field binField;
   private static Field enableOverrideField;
   private static Field externalizableField;
   private static Field handlesField;
   private static Field serializableField;
   private static Field vlistField;
   private static Method clearMethod;
   private static Method getSuperDescMethod;
   private static Method verifySubclassMethod;
   
   private static Class<?> blockDataInputStreamClass;
   private static Constructor<?> blockDataInputStreamConstructor;
   private static Method setBlockDataModeMethod;
   
   private static Class<?> handleTableClass;
   private static Constructor<?> handleTableConstructor;
   
   private static Class<?> validationListClass;
   private static Constructor<?> validationListConstructor;
   
   private static Constructor<?> objectStreamClassConstructor;
   
   private static Field getAccessibleDeclaredField(final Class<?> clazz, final String name) {
      return AccessController.doPrivileged(new PrivilegedAction<Field>() {
           public Field run() {
              try {
                 final Field field = clazz.getDeclaredField(name);
                 field.setAccessible(true);
                 return field;
              } catch (NoSuchFieldException e) {
                 throw new NoSuchFieldError(e.getMessage());
              }
           }
      });
   }

   private static Method getAccessibleDeclaredMethod(final Class<?> clazz, final String name, final Class<?>... paramTypes) {
       return AccessController.doPrivileged(new PrivilegedAction<Method>() {
           public Method run() {
               try {
                   final Method method = clazz.getDeclaredMethod(name, paramTypes);
                   method.setAccessible(true);
                   return method;
               } catch (NoSuchMethodException e) {
                   throw new NoSuchMethodError(e.getMessage());
               }
           }
       });
   }

   private static Constructor getAccessibleDeclaredConstructor(final Class<?> clazz, final Class<?>... paramTypes) {
       return AccessController.doPrivileged(new PrivilegedAction<Constructor>() {
           public Constructor run() {
               try {
                   final Constructor constructor = clazz.getDeclaredConstructor(paramTypes);
                   constructor.setAccessible(true);
                   return constructor;
               } catch (NoSuchMethodException e) {
                   throw new NoSuchMethodError(e.getMessage());
               }
           }
       });
   }

   static
   {
      externalizableField = getAccessibleDeclaredField(ObjectStreamClass.class, "externalizable");
      serializableField = getAccessibleDeclaredField(ObjectStreamClass.class, "serializable");
      clearMethod = getAccessibleDeclaredMethod(ObjectInputStream.class, "clear");
      getSuperDescMethod = getAccessibleDeclaredMethod(ObjectStreamClass.class, "getSuperDesc");
      objectStreamClassConstructor = getAccessibleDeclaredConstructor(ObjectStreamClass.class, Class.class);
   }
   
   private JavaSerializationUnmarshaller unmarshaller;
   private StreamHeader streamHeader;
   private ClassResolver classResolver;
   private ClassTable classTable;
   private ObjectResolver objectResolver;
   private ObjectTable objectTable;
   private Creator creator;
   
   public JavaSerializationInputStream(JavaSerializationUnmarshaller unmarshaller,
                                       StreamHeader streamHeader,
                                       ClassResolver classResolver,
                                       ClassTable classTable,
                                       ObjectResolver objectResolver,
                                       ObjectTable objectTable,
                                       Creator creator) throws IOException {
      this.unmarshaller = unmarshaller;
      this.streamHeader = streamHeader;
      this.classResolver = classResolver;
      this.classTable = classTable;
      this.objectResolver = new ObjectResolverWrapper(objectResolver);
      this.objectTable = objectTable;
      this.creator = creator;
      
      try {
         // Simulate single arg ObjectInputStream constructor.
         setPrivateFieldsAndMethods();
         verifySubclassMethod.invoke(this, EMPTY_PARAMS);
         binField.set(this, blockDataInputStreamConstructor.newInstance(new Object[] {this, unmarshaller.getInputStream()}));
         handlesField.set(this, handleTableConstructor.newInstance(new Object[]{10}));
         vlistField.set(this, validationListConstructor.newInstance(EMPTY_PARAMS));
         enableOverride(false);
      } catch (Exception e) {
         e.printStackTrace();
         throw new IOException(e.getClass() + ": " + e.getMessage());
      }
   }
   
   public JavaSerializationInputStream(JavaSerializationUnmarshaller unmarshaller,
                                       ClassResolver classResolver,
                                       ClassTable classTable,
                                       ObjectResolver objectResolver,
                                       ObjectTable objectTable,
                                       Creator creator) throws IOException {
      super(unmarshaller.getInputStream());   

      this.unmarshaller = unmarshaller;
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
   }
   
   public void clear()
   {
      try {
         clearMethod.invoke(this, EMPTY_PARAMS);
      }
      catch (Throwable t) {
         t.printStackTrace();
      }
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
   
   protected void completeConstruction() throws IOException {
      try {
         readStreamHeader();
         setBlockDataModeMethod.invoke(binField.get(this), new Object[]{ Boolean.TRUE });
         AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
               enableResolveObject(true);
               return null;
            }
         });
      }
      catch (Exception e) {
         throw new IOException(e.getMessage());
      }
   }
   
   protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
      if (classTable != null && CLASS_TABLE_WRITER == readByte()) {
         Class<?> c = classTable.readClass(unmarshaller);
         if (c != null) {
            try {
               return (ObjectStreamClass) objectStreamClassConstructor.newInstance(new Object[]{c});
            }
            catch (Exception e) {
               throw new IOException(e.getClass() + ": " + e.getMessage());
            }
         }
         else {
            return super.readClassDescriptor();   
         }
      }
      else {
         return super.readClassDescriptor();
      }
   }

   protected void readStreamHeader() throws IOException {
      if (streamHeader != null) {
         streamHeader.readHeader(unmarshaller);
      }
      else {
         super.readStreamHeader();
      }
   }
   
   protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      if (classResolver != null) {
         return classResolver.resolveClass(unmarshaller, desc.getName(), desc.getSerialVersionUID());
      }
      else {
         return super.resolveClass(desc);
      }
   }
   
   protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
      if (classResolver != null) {
         return classResolver.resolveProxyClass(unmarshaller, interfaces);
      }
      else {
         return super.resolveProxyClass(interfaces);
      }
   }
   
   protected Object resolveObject(Object obj) throws IOException {
      return objectResolver.readResolve(obj);
   }
   
   private void enableOverride(boolean enable) throws IOException {
      try {
         enableOverrideField.set(this, enable);
      }
      catch (Exception e) {
         throw new IOException(e.getClass() + ": " + e.getMessage());
      }
   }
   
   private synchronized void setPrivateFieldsAndMethods() {
      if (privateFieldsAndMethodsSet) {
         return;
      }
      
      try
      {
         binField = ObjectInputStream.class.getDeclaredField("bin");
         binField.setAccessible(true);
         
         enableOverrideField = ObjectInputStream.class.getDeclaredField("enableOverride");
         enableOverrideField.setAccessible(true);

         handlesField = ObjectInputStream.class.getDeclaredField("handles");
         handlesField.setAccessible(true);
         
         vlistField = ObjectInputStream.class.getDeclaredField("vlist");
         vlistField.setAccessible(true);
         
         verifySubclassMethod = ObjectInputStream.class.getDeclaredMethod("verifySubclass", new Class[]{});
         verifySubclassMethod.setAccessible(true);
         
         Class<?>[] classes = ObjectInputStream.class.getDeclaredClasses();
         for (int i = 0; i < classes.length; i++) {
            if ("java.io.ObjectInputStream$BlockDataInputStream".equals(classes[i].getName())) {
               blockDataInputStreamClass = classes[i];
            }
            
            if ("java.io.ObjectInputStream$HandleTable".equals(classes[i].getName())) {
               handleTableClass = classes[i];
            }
            
            if ("java.io.ObjectInputStream$ValidationList".equals(classes[i].getName())) {
               validationListClass = classes[i];
            }
         }
         
         if (blockDataInputStreamClass == null) {
            throw new Exception("Unable to find BlockDataInputStream class");
         }
         if (handleTableClass == null) {
            throw new Exception("Unable to find HandleTable class");
         }
         if (validationListClass == null) {
            throw new Exception("Unable to find ValidationList class");
         }

         blockDataInputStreamConstructor = blockDataInputStreamClass.getDeclaredConstructor(new Class[]{ObjectInputStream.class, InputStream.class});
         blockDataInputStreamConstructor.setAccessible(true);
         
         setBlockDataModeMethod = blockDataInputStreamClass.getDeclaredMethod("setBlockDataMode", new Class[]{boolean.class});
         setBlockDataModeMethod.setAccessible(true);
        
         handleTableConstructor = handleTableClass.getDeclaredConstructor(new Class[]{int.class});
         handleTableConstructor.setAccessible(true);
         
         validationListConstructor = validationListClass.getDeclaredConstructor(new Class[]{});
         validationListConstructor.setAccessible(true);
         
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
