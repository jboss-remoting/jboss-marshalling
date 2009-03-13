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
package org.jboss.marshalling.serialization.jboss;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.AbstractUnmarshaller;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.serialization.java.ByteInputStream;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 19, 2008
 * </p>
 */
public class JBossSerializationUnmarshaller extends AbstractUnmarshaller 
{
//   private static Logger log = Logger.getLogger(JavaSerializationMarshaller.class);
   
   private ByteInputStream bis;
   private JBossSerializationInputStream jbsis;
   private boolean jbossSerializationCompatible = false;
      
   public boolean isJbossSerializationCompatible() {
      return jbossSerializationCompatible;
   }

   public void setJbossSerializationCompatible(boolean jbossSerializationCompatible) {
      this.jbossSerializationCompatible = jbossSerializationCompatible;
   }

   protected JBossSerializationUnmarshaller(AbstractMarshallerFactory marshallerFactory, MarshallingConfiguration configuration) throws IOException {
      super(marshallerFactory, configuration);
   }

   public InputStream getInputStream() {
      return bis;
   }

   public void clearClassCache() throws IOException {
      jbsis.clear();
   }

   public void clearInstanceCache() throws IOException {
      jbsis.clear();   
   }

   public void close() throws IOException {
      jbsis.close();
      super.finish();
   }
   
   /** {@inheritDoc} */
   public void finish() throws IOException {
       jbsis.close();
   }
   
   /** {@inheritDoc} */
   public void start(final ByteInput byteInput) throws IOException {
      super.start(byteInput);
      bis = new ByteInputStream(byteInput);
      try {
         AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            public Void run() throws IOException {
               jbsis = new JBossSerializationInputStream(JBossSerializationUnmarshaller.this, streamHeader, classResolver, classTable, objectResolver, objectTable, creator, jbossSerializationCompatible);
               jbsis.completeConstruction();
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
       return jbsis.read();
   }

   /** {@inheritDoc} */
   public int read(final byte[] b) throws IOException {
       return read(b, 0, b.length);
   }

   /** {@inheritDoc} */
   public int read(final byte[] b, final int off, final int len) throws IOException {
       return jbsis.read(b, off, len);
   }

   /** {@inheritDoc} */
   public long skip(final long n) throws IOException {
       return jbsis.skip(n);
   }

   /** {@inheritDoc} */
   public int available() throws IOException {
       return jbsis.available();
   }

   /** {@inheritDoc} */
   public void readFully(final byte[] b) throws IOException {
       jbsis.readFully(b);
   }

   /** {@inheritDoc} */
   public void readFully(final byte[] b, final int off, final int len) throws IOException {
       jbsis.readFully(b, off, len);
   }

   /** {@inheritDoc} */
   public int skipBytes(final int n) throws IOException {
       return jbsis.skipBytes(n);
   }

   /** {@inheritDoc} */
   public boolean readBoolean() throws IOException {
       return jbsis.readBoolean();
   }

   /** {@inheritDoc} */
   public byte readByte() throws IOException {
       return jbsis.readByte();
   }

   /** {@inheritDoc} */
   public int readUnsignedByte() throws IOException {
       return jbsis.readUnsignedByte();
   }

   /** {@inheritDoc} */
   public short readShort() throws IOException {
       return jbsis.readShort();
   }

   /** {@inheritDoc} */
   public int readUnsignedShort() throws IOException {
       return jbsis.readUnsignedShort();
   }

   /** {@inheritDoc} */
   public char readChar() throws IOException {
       return jbsis.readChar();
   }

   /** {@inheritDoc} */
   public int readInt() throws IOException {
       return jbsis.readInt();
   }

   /** {@inheritDoc} */
   public long readLong() throws IOException {
       return jbsis.readLong();
   }

   /** {@inheritDoc} */
   public float readFloat() throws IOException {
       return jbsis.readFloat();
   }

   /** {@inheritDoc} */
   public double readDouble() throws IOException {
       return jbsis.readDouble();
   }

   /** {@inheritDoc} */
   @Deprecated
   public String readLine() throws IOException {
       return jbsis.readLine();
   }

   /** {@inheritDoc} */
   public String readUTF() throws IOException {
       return jbsis.readUTF();
   }
   
   @Override
   protected Object doReadObject(boolean unshared) throws ClassNotFoundException, IOException {
      if (unshared) {
         return jbsis.readUnshared();
      }
      else {
         return jbsis.readObject();
      }
   }
   
   @Override
   protected void doStart() throws IOException {
      // Don't want to call StreamHeader.readHeader() yet.
   }
}
