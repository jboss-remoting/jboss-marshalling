package org.jboss.marshalling.serialization.java;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.AbstractUnmarshaller;
import org.jboss.marshalling.ByteInput;
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
public class JavaSerializationUnmarshaller extends AbstractUnmarshaller 
{
//   private static Logger log = Logger.getLogger(JavaSerializationMarshaller.class);
   
   private ByteInputStream bis;
   private JavaSerializationInputStream jsis;
      
   protected JavaSerializationUnmarshaller(AbstractMarshallerFactory marshallerFactory, MarshallingConfiguration configuration) throws IOException {
      super(marshallerFactory, configuration);
   }

   public InputStream getInputStream() {
      return bis;
   }

   public void clearClassCache() throws IOException {
         jsis.clear();
   }

   public void clearInstanceCache() throws IOException {
      jsis.clear();   
   }

   public void close() throws IOException {
      jsis.close();
      super.finish();
   }
   
   /** {@inheritDoc} */
   public void finish() throws IOException {
       jsis.close();
   }
   
   /** {@inheritDoc} */
   public void start(final ByteInput byteInput) throws IOException {
      super.start(byteInput);
      bis = new ByteInputStream(byteInput);
       try {
           AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
               public Void run() throws IOException {
                   if (streamHeader == null) {
                      jsis = new JavaSerializationInputStream(JavaSerializationUnmarshaller.this, classResolver, classTable, objectResolver, objectTable, creator);
                   }
                   else {
                      jsis = new JavaSerializationInputStream(JavaSerializationUnmarshaller.this, streamHeader, classResolver, classTable, objectResolver, objectTable, creator);
                      jsis.completeConstruction();
                   }
                   return null;
               }
           });
       } catch (PrivilegedActionException e) {
           throw (IOException) e.getCause();
       }
   }
   
   /*************************************************************************************
     ByteInput and DataInput methods
    *************************************************************************************/
   /** {@inheritDoc} */
   public int read() throws IOException {
       return jsis.read();
   }

   /** {@inheritDoc} */
   public int read(final byte[] b) throws IOException {
       return read(b, 0, b.length);
   }

   /** {@inheritDoc} */
   public int read(final byte[] b, final int off, final int len) throws IOException {
       return jsis.read(b, off, len);
   }

   /** {@inheritDoc} */
   public long skip(final long n) throws IOException {
       return jsis.skip(n);
   }

   /** {@inheritDoc} */
   public int available() throws IOException {
       return jsis.available();
   }

   /** {@inheritDoc} */
   public void readFully(final byte[] b) throws IOException {
       jsis.readFully(b);
   }

   /** {@inheritDoc} */
   public void readFully(final byte[] b, final int off, final int len) throws IOException {
       jsis.readFully(b, off, len);
   }

   /** {@inheritDoc} */
   public int skipBytes(final int n) throws IOException {
       return jsis.skipBytes(n);
   }

   /** {@inheritDoc} */
   public boolean readBoolean() throws IOException {
       return jsis.readBoolean();
   }

   /** {@inheritDoc} */
   public byte readByte() throws IOException {
       return jsis.readByte();
   }

   /** {@inheritDoc} */
   public int readUnsignedByte() throws IOException {
       return jsis.readUnsignedByte();
   }

   /** {@inheritDoc} */
   public short readShort() throws IOException {
       return jsis.readShort();
   }

   /** {@inheritDoc} */
   public int readUnsignedShort() throws IOException {
       return jsis.readUnsignedShort();
   }

   /** {@inheritDoc} */
   public char readChar() throws IOException {
       return jsis.readChar();
   }

   /** {@inheritDoc} */
   public int readInt() throws IOException {
       return jsis.readInt();
   }

   /** {@inheritDoc} */
   public long readLong() throws IOException {
       return jsis.readLong();
   }

   /** {@inheritDoc} */
   public float readFloat() throws IOException {
       return jsis.readFloat();
   }

   /** {@inheritDoc} */
   public double readDouble() throws IOException {
       return jsis.readDouble();
   }

   /** {@inheritDoc} */
   @Deprecated
   public String readLine() throws IOException {
       return jsis.readLine();
   }

   /** {@inheritDoc} */
   public String readUTF() throws IOException {
       return jsis.readUTF();
   }
   
   @Override
   protected Object doReadObject(boolean unshared) throws ClassNotFoundException, IOException {
      if (unshared) {
         return jsis.readUnshared();
      }
      else {
         return jsis.readObject();
      }
   }
   
   @Override
   protected void doStart() throws IOException {
      // Don't want to call StreamHeader.readHeader() yet.
   }
}
