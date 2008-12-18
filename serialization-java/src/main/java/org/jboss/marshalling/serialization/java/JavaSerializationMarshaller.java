package org.jboss.marshalling.serialization.java;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import org.jboss.marshalling.AbstractMarshaller;
import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.MarshallingConfiguration;


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


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 19, 2008
 * </p>
 */
public class JavaSerializationMarshaller extends AbstractMarshaller 
{
//   private static Logger log = Logger.getLogger(JavaSerializationMarshaller.class);
   
   private ByteOutputStream bos;
   private JavaSerializationOutputStream jsos;
   
  
   protected JavaSerializationMarshaller(AbstractMarshallerFactory marshallerFactory, final MarshallingConfiguration configuration) throws IOException {
      super(marshallerFactory, configuration);
   }

   public void clearClassCache() throws IOException {
      jsos.clear();
   }

   public void clearInstanceCache() throws IOException {
      jsos.clear();
   }
   
   public ByteOutputStream getOutputStream() {
      return bos;
   }
   
   /** {@inheritDoc} */
   public void flush() throws IOException {
       jsos.flush();
   }

   /** {@inheritDoc} */
   public void finish() throws IOException {
       jsos.close();
   }
   
   public void start(final ByteOutput byteOutput) throws IOException {
      super.start(byteOutput);
      bos = new ByteOutputStream(byteOutput);

      try {
         AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            public Void run() throws IOException {
               if (streamHeader == null) {
                  jsos = new JavaSerializationOutputStream(JavaSerializationMarshaller.this,
                                                           classResolver,
                                                           classTable,
                                                           objectResolver,
                                                           objectTable,
                                                           externalizerFactory);
               }
               else {
                  jsos = new JavaSerializationOutputStream(JavaSerializationMarshaller.this,
                                                           streamHeader,
                                                           classResolver,
                                                           classTable,
                                                           objectResolver,
                                                           objectTable,
                                                           externalizerFactory);
                  jsos.completeConstruction();
               }
               return null;
            }
         });
      } catch (PrivilegedActionException e) {
         throw (IOException) e.getCause();
      }
   }
   
   @Override
   protected void doWriteObject(Object obj, boolean unshared) throws IOException {
      if (unshared) {
         jsos.writeUnshared(obj);
      }
      else {
         jsos.writeObject(obj);
      }
   }
   
   /******************************************************************************************/
   /** DataOutput methods.
   /******************************************************************************************/
   /** {@inheritDoc} */
   public void write(final int b) throws IOException {
       jsos.write(b);
   }

   /** {@inheritDoc} */
   public void write(final byte[] b) throws IOException {
       jsos.write(b);
   }

   /** {@inheritDoc} */
   public void write(final byte[] b, final int off, final int len) throws IOException {
       jsos.write(b, off, len);
   }

   /** {@inheritDoc} */
   public void writeBoolean(final boolean v) throws IOException {
       jsos.writeBoolean(v);
   }

   /** {@inheritDoc} */
   public void writeByte(final int v) throws IOException {
       jsos.writeByte(v);
   }

   /** {@inheritDoc} */
   public void writeShort(final int v) throws IOException {
       jsos.writeShort(v);
   }

   /** {@inheritDoc} */
   public void writeChar(final int v) throws IOException {
       jsos.writeChar(v);
   }

   /** {@inheritDoc} */
   public void writeInt(final int v) throws IOException {
       jsos.writeInt(v);
   }

   /** {@inheritDoc} */
   public void writeLong(final long v) throws IOException {
       jsos.writeLong(v);
   }

   /** {@inheritDoc} */
   public void writeFloat(final float v) throws IOException {
       jsos.writeFloat(v);
   }

   /** {@inheritDoc} */
   public void writeDouble(final double v) throws IOException {
       jsos.writeDouble(v);
   }

   /** {@inheritDoc} */
   public void writeBytes(final String s) throws IOException {
       jsos.writeBytes(s);
   }

   /** {@inheritDoc} */
   public void writeChars(final String s) throws IOException {
       jsos.writeChars(s);
   }

   /** {@inheritDoc} */
   public void writeUTF(final String s) throws IOException {
       jsos.writeUTF(s);
   }
   
   protected void doStart() throws IOException {
      // Don't want to call StreamHeader.writeHeader() yet.
   }
}

