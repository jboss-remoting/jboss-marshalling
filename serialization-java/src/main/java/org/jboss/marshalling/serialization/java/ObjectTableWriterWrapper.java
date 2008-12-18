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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 19, 2008
 * </p>
 */
public class ObjectTableWriterWrapper implements Externalizable
{
   private static final long serialVersionUID = 3008264734827845637L;

   private ObjectTable.Writer writer;
   private Marshaller marshaller;
   private Object obj;

   public ObjectTableWriterWrapper(ObjectTable.Writer writer, Marshaller marshaller, Object obj) {
      if ((this.writer = writer) == null) {
         throw new RuntimeException("Externalizer must not be null");
      }
      this.marshaller = marshaller;
      this.obj = obj;
   }
   
   public ObjectTableWriterWrapper() {
   }
   
   public Object getWrappedObject() {
      return obj;
   }
   
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      try {
         ObjectTable objectTable = null;
         Unmarshaller unmarshaller = null;
         if (in instanceof JavaSerializationInputStream){
            JavaSerializationInputStream jsis = (JavaSerializationInputStream) in;
            objectTable = jsis.getObjectTable();
            unmarshaller = jsis.getUnmarshaller();
         }
         else {
            throw new RuntimeException("expecting JavaSerializationInputStream");
         }
         
         if (objectTable == null) {
            throw new IOException("ObjectTable must not be null");
         }
         
         obj = objectTable.readObject(unmarshaller);
         
      } catch (Exception e) {
         throw new IOException(e.getMessage());
      }
   }

   public void writeExternal(ObjectOutput out) throws IOException {
      writer.writeObject(marshaller, obj);
      writer = null;
      marshaller = null;
      obj = null;
   }
}

