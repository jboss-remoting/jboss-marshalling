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

import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 19, 2008
 * </p>
 */
public class ExternalizableWrapper implements Externalizable
{
   private static final long serialVersionUID = -8877220049146293957L;

   private Externalizer externalizer;
   private Object obj;
 
   public ExternalizableWrapper(Externalizer externalizer, Object obj) {
      if ((this.externalizer = externalizer) == null) {
         throw new RuntimeException("Externalizer must not be null");
      }
      this.obj = obj;
   }
   
   public ExternalizableWrapper() {
   }
   
   public Object getWrappedObject() {
      return obj;
   }
   
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      try {
         externalizer = (Externalizer) in.readObject();
         String objClassName = in.readUTF();
         if (objClassName == null) {
            throw new IOException("Cannot read object class name");
         }
         Class<?> objClass = Class.forName(objClassName);
         
         Creator creator;
         if (in instanceof JavaSerializationInputStream){
            creator = ((JavaSerializationInputStream) in).getCreator();
         }
         else {
            throw new RuntimeException("expecting JavaSerializationInputStream");
         }
         
         if (creator != null) {
            obj = externalizer.createExternal(objClass, in, creator);
         }
         else {
            obj = objClass.newInstance();
         }
         
         externalizer.readExternal(obj, in);
         externalizer = null;
         
      } catch (Exception e) {
         throw new IOException(e.getMessage());
      }
   }

   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeObject(externalizer);
      out.writeUTF(obj.getClass().getName());
      externalizer.writeExternal(obj, out);
   }
}

