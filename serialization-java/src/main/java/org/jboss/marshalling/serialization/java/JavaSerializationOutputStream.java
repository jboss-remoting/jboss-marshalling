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
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.ClassExternalizerFactory;

import static org.jboss.marshalling.serialization.java.JavaSerializationConstants.*;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 19, 2008
 * </p>
 */
public class JavaSerializationOutputStream extends ObjectOutputStream
{
   private static boolean privateFieldsAndMethodsSetForStreamHeader;
   private static boolean privateFieldsAndMethodsSetForWriteOverride;
   
   private static Field boutField;
   private static Field depthField;
   private static Field enableOverrideField;
   private static Field handlesField;
   private static Field subsField;
   private static Method clearMethod;
   private static Method verifySubclassMethod;
   private static Method writeFatalExceptionMethod;
   private static Method writeObject0Method;
   
   private static Class<?> blockDataOutputStreamClass;
   private static Constructor<?> blockDataOutputStreamConstructor;
   private static Method setBlockDataModeMethod;
   
   private static Class<?> handleTableClass;
   private static Constructor<?> handleTableConstructor;
   
   private static Class<?> replaceTableClass;
   private static Constructor<?> replaceTableConstructor;
   
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

   private Map<Object, ExternalizableWrapper> externalizedCache = new HashMap<Object, ExternalizableWrapper>();
   private Map<Object, ObjectTableWriterWrapper> objectTableWritableCache = new HashMap<Object, ObjectTableWriterWrapper>();
   private JavaSerializationMarshaller marshaller;
   private StreamHeader streamHeader;
   private ClassResolver classResolver;
   private ObjectResolver objectResolver;
   private ClassExternalizerFactory classExternalizerFactory;
   private ExternalizerFactory externalizerFactory;
   private ClassTable classTable;
   private ObjectTable objectTable;
   
   public JavaSerializationOutputStream(JavaSerializationMarshaller marshaller,
                                       StreamHeader streamHeader,
                                       ClassResolver classResolver,
                                       ClassTable classTable,
                                       ObjectResolver objectResolver,
                                       ObjectTable objectTable,
                                       ExternalizerFactory externalizerFactory,
                                       ClassExternalizerFactory classExternalizerFactory) throws IOException
   {
      super();   
      
      this.marshaller = marshaller;
      this.streamHeader = streamHeader;
      this.classResolver = classResolver;
      this.classTable = classTable;
      this.objectResolver = objectResolver;
      this.objectTable = objectTable;
      this.externalizerFactory = externalizerFactory;
      this.classExternalizerFactory = classExternalizerFactory;
      
      try {
         // Simulate single arg ObjectOutputStream constructor.
         setPrivateFieldsAndMethodsForStreamHeader();
         verifySubclassMethod.invoke(this, EMPTY_PARAMS);
         boutField.set(this, blockDataOutputStreamConstructor.newInstance(new Object[] {marshaller.getOutputStream()}));
         handlesField.set(this, handleTableConstructor.newInstance(new Object[]{10, (float) 3.00}));
         subsField.set(this, replaceTableConstructor.newInstance(new Object[]{10, (float) 3.00}));
         
         if (objectTable == null && externalizerFactory == null && classExternalizerFactory == null) {
            setPrivateFieldsAndMethodsForWriteOverride();
            enableOverride(false);
         }
      } catch (Exception e) {
         throw new IOException(e.getClass() + ": " + e.getMessage());
      }
   }

   public JavaSerializationOutputStream(JavaSerializationMarshaller marshaller,
                                        ClassResolver classResolver,
                                        ClassTable classTable,
                                        ObjectResolver objectResolver,
                                        ObjectTable objectTable,
                                        ExternalizerFactory externalizerFactory,
                                        ClassExternalizerFactory classExternalizerFactory) throws IOException
   {
      super(marshaller.getOutputStream());   

      this.marshaller = marshaller;
      this.classResolver = classResolver;
      this.classTable = classTable;
      this.objectResolver = objectResolver;
      this.objectTable = objectTable;
      this.externalizerFactory = externalizerFactory;
      this.classExternalizerFactory = classExternalizerFactory;

      if (objectTable != null || externalizerFactory != null || classExternalizerFactory != null) {
         try {
            setPrivateFieldsAndMethodsForWriteOverride();
            enableOverride(true);
         } catch (Exception e) {
            throw new IOException(e.getClass() + ": " + e.getMessage());
         }
      }

      if (objectResolver != null) {
         enableReplaceObject(true);
      }
   }
   
   public void clear() throws IOException
   {
//      try {
//         clearMethod.invoke(this, EMPTY_PARAMS);
//      }
//      catch (Throwable t) {
//         t.printStackTrace();
//      }
      reset();
      objectTableWritableCache.clear();
      externalizedCache.clear();
   }
   
   public void writeStreamHeader() throws IOException {
      if (streamHeader != null) {
         streamHeader.writeHeader(marshaller);
      }
      else {
         super.writeStreamHeader();
      }
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
   
   protected void completeConstruction() throws IOException {
      try {
         writeStreamHeader();
         setBlockDataModeMethod.invoke(boutField.get(this), Boolean.TRUE);
         if (objectResolver != null) {
            enableReplaceObject(true);
         }
      }
      catch (Exception e) {
          final IOException ioe = new IOException(e.getMessage());
          ioe.initCause(e);
          throw ioe;
      }
   }
   
   protected Object replaceObject(Object obj) throws IOException {
      if (objectResolver != null) { // sanity test
          Object replacement = objectResolver.writeReplace(obj);
//          while (!replacement.equals(obj))
//             obj = replacement;
//             replacement = objectResolver.writeReplace(obj);
          return replacement;
      }
      else {
         throw new RuntimeException("objectResolver should not be null");
      }
   }
   
   protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
      if (classTable != null) {
         ClassTable.Writer writer = classTable.getClassWriter(desc.forClass());
         if (writer == null) {
            writeByte(NO_CLASS_TABLE_WRITER);
            super.writeClassDescriptor(desc);
         }
         else {
            writeByte(CLASS_TABLE_WRITER);
            writer.writeClass(marshaller, desc.forClass());
         }
      }
      else {
         super.writeClassDescriptor(desc);
      }
   }
   
   protected void writeObjectOverride(Object obj) throws IOException {
      if (objectTable != null) {
         ObjectTable.Writer writer = objectTable.getObjectWriter(obj);
         if (writer != null) {
            ObjectTableWriterWrapper wrapper = objectTableWritableCache.get(obj);
            if (wrapper == null) {
               wrapper = new ObjectTableWriterWrapper(writer, marshaller, obj);
               objectTableWritableCache.put(obj, wrapper);
            }
            obj = wrapper;
         }
      }

      Externalizer externalizer = null;
      if (classExternalizerFactory != null) {
         externalizer = classExternalizerFactory.getExternalizer(obj.getClass());
      }
      if (externalizer == null && externalizerFactory != null) {
         externalizer = externalizerFactory.getExternalizer(obj);
      }

      if (externalizer != null) {
         ExternalizableWrapper wrapper = externalizedCache.get(obj);
         if (wrapper == null) {
            wrapper = new ExternalizableWrapper(externalizer, obj);
            externalizedCache.put(obj, wrapper);
         }
         obj = wrapper;
      }

      try {
         writeObject0Method.invoke(this, new Object[]{obj, false});
      }
      catch (InvocationTargetException ex) {
         try {
            if ((Integer) depthField.get(this) == 0) {
               writeFatalExceptionMethod.invoke(this, new Object[]{ex});
            }
         }
         catch (Exception e) {
            throw new IOException(e.getCause() + ": " + e.getMessage());
         }
      }
      catch (Exception e) {
         throw new IOException(e.getCause() + ": " + e.getMessage());
      }
   }

   private void enableOverride(boolean enable) throws IOException {
      try {
         enableOverrideField.set(this, Boolean.valueOf(enable));
      }
      catch (Exception e) {
          final IOException ioe = new IOException(e.getMessage());
          ioe.initCause(e);
          throw ioe;
      }
   }
   
   private synchronized static void setPrivateFieldsAndMethodsForStreamHeader() {
      if (privateFieldsAndMethodsSetForStreamHeader) {
         return;
      }
      
      try
      {  
         boutField = getAccessibleDeclaredField(ObjectOutputStream.class, "bout");
         handlesField = getAccessibleDeclaredField(ObjectOutputStream.class, "handles");
         subsField = getAccessibleDeclaredField(ObjectOutputStream.class, "subs");
         verifySubclassMethod = getAccessibleDeclaredMethod(ObjectOutputStream.class, "verifySubclass");
         
         Class<?>[] classes = ObjectOutputStream.class.getDeclaredClasses();
         for (Class<?> clazz : classes) {

            if ("java.io.ObjectOutputStream$BlockDataOutputStream".equals(clazz.getName())) {
               blockDataOutputStreamClass = clazz;
            }

            if ("java.io.ObjectOutputStream$HandleTable".equals(clazz.getName())) {
               handleTableClass = clazz;
            }

            if ("java.io.ObjectOutputStream$ReplaceTable".equals(clazz.getName())) {
               replaceTableClass = clazz;
            }
         }
         
         if (blockDataOutputStreamClass == null) {
            throw new Exception("Unable to find BlockDataOutputStream class");
         }
         if (handleTableClass == null) {
            throw new Exception("Unable to find HandleTable class");
         }
         if (replaceTableClass == null) {
            throw new Exception("Unable to find ReplaceTable class");
         }

         blockDataOutputStreamConstructor = getAccessibleDeclaredConstructor(blockDataOutputStreamClass, OutputStream.class);
         setBlockDataModeMethod = getAccessibleDeclaredMethod(blockDataOutputStreamClass, "setBlockDataMode", boolean.class);
         handleTableConstructor = getAccessibleDeclaredConstructor(handleTableClass, int.class, float.class);
         replaceTableConstructor = getAccessibleDeclaredConstructor(replaceTableClass, int.class, float.class);
         privateFieldsAndMethodsSetForStreamHeader = true;

      } catch (Exception e) {
         throw new RuntimeException("unable to obtain private field or method from superclass", e);
      }
   }
   
   private synchronized static void setPrivateFieldsAndMethodsForWriteOverride() {
      if (privateFieldsAndMethodsSetForWriteOverride) {
         return;
      }
      
      try
      {  
         depthField = getAccessibleDeclaredField(ObjectOutputStream.class, "depth");
         enableOverrideField = getAccessibleDeclaredField(ObjectOutputStream.class, "enableOverride");
         clearMethod = getAccessibleDeclaredMethod(ObjectOutputStream.class, "clear");
         writeFatalExceptionMethod = getAccessibleDeclaredMethod(ObjectOutputStream.class, "writeFatalException", IOException.class);
         writeObject0Method = getAccessibleDeclaredMethod(ObjectOutputStream.class, "writeObject0", Object.class, boolean.class);
         privateFieldsAndMethodsSetForWriteOverride = true;
         
      } catch (Exception e) {
         throw new RuntimeException("unable to obtain private field or method from superclass", e);
      }
   }
}
