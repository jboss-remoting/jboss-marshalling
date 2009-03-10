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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.marshalling.AbstractMarshaller;
import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.serialization.java.ByteOutputStream;
import org.jboss.serial.util.StringUtilBuffer;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Jan 29, 2009
 * </p>
 */
public class JBossSerializationMarshaller extends AbstractMarshaller 
{
//   private static Logger log = Logger.getLogger(JavaSerializationMarshaller.class);
   
   private ByteOutputStream bos;
   private JBossSerializationOutputStream jbsos;
   private boolean nativeImmutableHandling = true;
   
  
   protected JBossSerializationMarshaller(AbstractMarshallerFactory marshallerFactory, final MarshallingConfiguration configuration) throws IOException {
      super(marshallerFactory, configuration);
   }

   public void clearClassCache() throws IOException {
      jbsos.clear();
   }

   public void clearInstanceCache() throws IOException {
      jbsos.clear();
   }
   
   public ByteOutputStream getOutputStream() {
      return bos;
   }
   
   /** {@inheritDoc} */
   public void flush() throws IOException {
       jbsos.flush();
   }

   /** {@inheritDoc} */
   public void finish() throws IOException {
       jbsos.close();
   }
   
   public void start(final ByteOutput byteOutput) throws IOException {
      super.start(byteOutput);
      bos = new ByteOutputStream(byteOutput);

      try {
         AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            public Void run() throws IOException {
               jbsos = new JBossSerializationOutputStream(false,
                                                          new StringUtilBuffer(10024, 10024),
                                                          JBossSerializationMarshaller.this,
                                                          streamHeader,
                                                          classResolver,
                                                          classTable,
                                                          objectResolver,
                                                          objectTable,
                                                          classExternalizerFactory,
                                                          nativeImmutableHandling);
               jbsos.completeConstruction();
               return null;
            }
         });
      } catch (PrivilegedActionException e) {
         throw (IOException) e.getCause();
      }
   }

   public boolean isNativeImmutableHandling()
   {
      return nativeImmutableHandling;
   }

   public void setNativeImmutableHandling(boolean nativeImmutableHandling)
   {
      this.nativeImmutableHandling = nativeImmutableHandling;
   }

   @Override
   protected void doWriteObject(Object obj, boolean unshared) throws IOException {
      if (unshared) {
         jbsos.writeUnshared(obj);
      }
      else {
         jbsos.writeObject(obj);
      }
   }
   
   /******************************************************************************************/
   /** DataOutput methods.
   /******************************************************************************************/
   /** {@inheritDoc} */
   public void write(final int b) throws IOException {
       jbsos.write(b);
   }

   /** {@inheritDoc} */
   public void write(final byte[] b) throws IOException {
       jbsos.write(b);
   }

   /** {@inheritDoc} */
   public void write(final byte[] b, final int off, final int len) throws IOException {
       jbsos.write(b, off, len);
   }

   /** {@inheritDoc} */
   public void writeBoolean(final boolean v) throws IOException {
       jbsos.writeBoolean(v);
   }

   /** {@inheritDoc} */
   public void writeByte(final int v) throws IOException {
       jbsos.writeByte(v);
   }

   /** {@inheritDoc} */
   public void writeShort(final int v) throws IOException {
       jbsos.writeShort(v);
   }

   /** {@inheritDoc} */
   public void writeChar(final int v) throws IOException {
       jbsos.writeChar(v);
   }

   /** {@inheritDoc} */
   public void writeInt(final int v) throws IOException {
       jbsos.writeInt(v);
   }

   /** {@inheritDoc} */
   public void writeLong(final long v) throws IOException {
       jbsos.writeLong(v);
   }

   /** {@inheritDoc} */
   public void writeFloat(final float v) throws IOException {
       jbsos.writeFloat(v);
   }

   /** {@inheritDoc} */
   public void writeDouble(final double v) throws IOException {
       jbsos.writeDouble(v);
   }

   /** {@inheritDoc} */
   public void writeBytes(final String s) throws IOException {
       jbsos.writeBytes(s);
   }

   /** {@inheritDoc} */
   public void writeChars(final String s) throws IOException {
       jbsos.writeChars(s);
   }

   /** {@inheritDoc} */
   public void writeUTF(final String s) throws IOException {
       jbsos.writeUTF(s);
   }
   
   protected void doStart() throws IOException {
      // Don't want to call StreamHeader.writeHeader() yet.
   }
}

