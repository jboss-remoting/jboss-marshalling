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
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Oct 3, 2008
 * </p>
 */
public class JavaSerializationExternalizer {
   
   public static void externalize(ObjectOutput out, Externalizer externalizer, Object obj) throws IOException {
      out.writeObject(externalizer);
      out.writeUTF(obj.getClass().getName());
      externalizer.writeExternal(obj, out);
   }

   public static Object internalize(ObjectInput in, Creator creator) throws IOException, ClassNotFoundException
   {
      try {
         Externalizer externalizer = (Externalizer) in.readObject();
         if (externalizer == null) {
            throw new IOException("Cannot read Externalizer");
         }
         
         String objClassName = in.readUTF();
         if (objClassName == null) {
            throw new IOException("Cannot read object class name");
         }
         
         Class<?> objClass = Class.forName(objClassName);
         Object obj = null;
         if (creator != null) {
            obj = externalizer.createExternal(objClass, in, creator);
         }
         else {
            obj = objClass.newInstance();
            externalizer.readExternal(obj, in);
         }
         
         return obj;
         
      } catch (Exception e) {
         throw new IOException(e.getMessage());
      }
   }
}

