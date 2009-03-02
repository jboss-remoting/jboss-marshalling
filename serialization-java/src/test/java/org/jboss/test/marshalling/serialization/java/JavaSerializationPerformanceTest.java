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
package org.jboss.test.marshalling.serialization.java;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.serialization.java.JavaSerializationMarshallerFactory;
import org.jboss.serial.io.JBossObjectInputStream;
import org.jboss.serial.io.JBossObjectOutputStream;

/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Oct 5, 2008
 * </p>
 */
public class JavaSerializationPerformanceTest
{
   private TestComplexObject tco1 = new TestComplexObject(false, 3, (float) 7.0, 11.0, "seventeen", null);
   private TestComplexObject tco2 = new TestComplexObject(true, 23, (float) 29.0, 31.0, "thirty-seven", null);
   private ArrayList<Object> list = new ArrayList<Object>();
   private HashSet<Object> set = new HashSet<Object>();
   
   public static void main(String[] args) {
      JavaSerializationPerformanceTest test = new JavaSerializationPerformanceTest();
      test.init();
      try
      {
         int count = 50000;
         test.warmup(count);
         
         test.testMarshallingPureSerialization(count);
         test.testMarshallingWithExternalizer(count);
         test.testMarshallingWithStreamHeader(count);
         test.testJavaPureSerialization(count);
         
         test.testMarshallingPureSerializationWithRecreation(count);
         test.testMarshallingWithExternalizerAndRecreation(count);
         test.testMarshallingWithStreamHeaderAndRecreation(count);
         test.testJavaPureSerializationWithRecreation(count);
//         test.testJBossPureSerialization(count);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   
   public void warmup(int count) throws Exception {
      System.out.println("warming up");
      int halfCount = (int) (count / 2.0);
      MarshallingConfiguration config = new MarshallingConfiguration();
      
      PipedOutputStream pos = new PipedOutputStream();
      PipedInputStream pis = new PipedInputStream(pos);
      BufferedOutputStream bos = new BufferedOutputStream(pos);
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.flush();
      BufferedInputStream bis = new BufferedInputStream(pis);
      ObjectInputStream ois = new ObjectInputStream(bis);
      doStreamTest(oos, ois, set, halfCount);
      
      testMarshalling(config, halfCount);
   }
   
   public void testJavaPureSerialization(int count) throws Exception {
      System.out.print("testJavaPureSerialization(): ");
      PipedOutputStream pos = new PipedOutputStream();
      PipedInputStream pis = new PipedInputStream(pos);
      BufferedOutputStream bos = new BufferedOutputStream(pos);
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.flush();
      BufferedInputStream bis = new BufferedInputStream(pis);
      ObjectInputStream ois = new ObjectInputStream(bis);
      int time = doStreamTest(oos, ois, set, count);
      System.out.println(time);
   }
   
   public void testMarshallingPureSerialization(int count) throws Exception {
      System.out.print("testMarshallingPureSerialization(): ");
      MarshallingConfiguration config = new MarshallingConfiguration();
      int time = testMarshalling(config, count);
      System.out.println(time);
   }
   
   public void testMarshallingWithExternalizer(int count) throws Exception {
      System.out.print("testMarshallingWithExternalizer(): ");
      MarshallingConfiguration config = new MarshallingConfiguration();
      ExternalizerFactory factory = new ExternalizerFactory() {
         public Externalizer getExternalizer(Object instance) {
            return null;
         }
      };
      config.setExternalizerFactory(factory);
      int time = testMarshalling(config, count);
      System.out.println(time);
   }
   
   public void testMarshallingWithStreamHeader(int count) throws Exception {
      System.out.print("testMarshallingWithStreamHeader(): ");
      MarshallingConfiguration config = new MarshallingConfiguration();
      StreamHeader streamHeader = new StreamHeader() {
         public void readHeader(ByteInput input) throws IOException { }
         public void writeHeader(ByteOutput output) throws IOException { }
      };
      config.setStreamHeader(streamHeader);
      int time = testMarshalling(config, count);
      System.out.println(time);
   }
   
   public int testMarshalling(MarshallingConfiguration config, int count) throws Exception {
      JavaSerializationMarshallerFactory factory = new JavaSerializationMarshallerFactory();
      Marshaller marshaller = factory.createMarshaller(config);
      PipedOutputStream pos = new PipedOutputStream();
      PipedInputStream pis = new PipedInputStream(pos);
      BufferedOutputStream bos = new BufferedOutputStream(pos);
      ByteOutput byteOutput = Marshalling.createByteOutput(bos);
      marshaller.start(byteOutput);
      marshaller.flush();
      
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      BufferedInputStream bis = new BufferedInputStream(pis);
      ByteInput byteInput = Marshalling.createByteInput(bis);
      unmarshaller.start(byteInput);
      
      return doMarshallerTest(marshaller, unmarshaller, set, count);
   }
   
   public void testJBossPureSerialization(int count) throws Exception {
      PipedOutputStream pos = new PipedOutputStream();
      PipedInputStream pis = new PipedInputStream(pos);
      BufferedOutputStream bos = new BufferedOutputStream(pos);
      ObjectOutputStream oos = new JBossObjectOutputStream(bos);
      oos.flush();
      BufferedInputStream bis = new BufferedInputStream(pis);
      ObjectInputStream ois = new JBossObjectInputStream(bis);
      int time = doStreamTest(oos, ois, set, count);
      System.out.println("testJBossPureSerialization(): " + time);
   }

   public void testJavaPureSerializationWithRecreation(int count) throws Exception {
      System.out.print("testJavaPureSerializationWithRecreation(): ");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         PipedOutputStream pos = new PipedOutputStream();
         PipedInputStream pis = new PipedInputStream(pos);
         ObjectOutputStream oos = new ObjectOutputStream(pos);
         oos.flush();
         ObjectInputStream ois = new ObjectInputStream(pis);
         oos.writeObject(set);
         oos.flush();
         oos.reset();
         if (! (ois.readObject() instanceof HashSet)) {
            throw new IOException("didn't get HashSet");
         }
      }
      System.out.println(System.currentTimeMillis() - start);
   }
   
   public void testMarshallingPureSerializationWithRecreation(int count) throws Exception {
      System.out.print("testMarshallingPureSerializationWithRecreation(): ");
      MarshallingConfiguration config = new MarshallingConfiguration();
      int time = doMarshallerTestWithRecreation(config, count);
      System.out.println(time);
   }
   
   public void testMarshallingWithExternalizerAndRecreation(int count) throws Exception {
      System.out.print("testMarshallingWithExternalizerAndRecreation(): ");
      MarshallingConfiguration config = new MarshallingConfiguration();
      ExternalizerFactory factory = new ExternalizerFactory() {
         public Externalizer getExternalizer(Object instance) {
            return null;
         }
      };
      config.setExternalizerFactory(factory);
      int time = doMarshallerTestWithRecreation(config, count);
      System.out.println(time);
   }
   
   public void testMarshallingWithStreamHeaderAndRecreation(int count) throws Exception {
      System.out.print("testMarshallingWithStreamHeaderAndRecreation(): ");
      MarshallingConfiguration config = new MarshallingConfiguration();
      StreamHeader streamHeader = new StreamHeader() {
         public void readHeader(ByteInput input) throws IOException { }
         public void writeHeader(ByteOutput output) throws IOException { }
      };
      config.setStreamHeader(streamHeader);
      int time = doMarshallerTestWithRecreation(config, count);
      System.out.println(time);
   }
   
   private void init() {
      list.add(tco1);
      list.add(tco2);
      list.add("testString");
      set.add(list);
      set.add(new Integer(17));
   }
   
   private int doStreamTest(ObjectOutputStream out, ObjectInputStream in, Object obj, int count) throws IOException, ClassNotFoundException
   {
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
//         if ((i+1) % 1000 == 0) System.out.println(i + 1);
         out.writeObject(obj);
         out.flush();
         out.reset();
         if (! (in.readObject() instanceof HashSet)) {
            throw new IOException("didn't get HashSet");
         }
      }
      return (int)(System.currentTimeMillis() - start);
   }
   
   private int doMarshallerTest(Marshaller out, Unmarshaller in, Object obj, int count) throws IOException, ClassNotFoundException
   {
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
//         if ((i+1) % 1000 == 0) System.out.println(i + 1);
         out.writeObject(obj);
         out.flush();
         out.clearClassCache();
//         out.clearInstanceCache();
         if (! (in.readObject() instanceof HashSet)) {
            throw new IOException("didn't get HashSet");
         }
      }
      return (int)(System.currentTimeMillis() - start);
   }
   
   protected int doMarshallerTestWithRecreation(MarshallingConfiguration config, int count) throws Exception {
      JavaSerializationMarshallerFactory factory = new JavaSerializationMarshallerFactory();
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         Marshaller marshaller = factory.createMarshaller(config);
         PipedOutputStream pos = new PipedOutputStream();
         PipedInputStream pis = new PipedInputStream(pos);
         ByteOutput byteOutput = Marshalling.createByteOutput(pos);
         marshaller.start(byteOutput);
         marshaller.flush();
         Unmarshaller unmarshaller = factory.createUnmarshaller(config);
         ByteInput byteInput = Marshalling.createByteInput(pis);
         unmarshaller.start(byteInput);
         marshaller.writeObject(set);
         marshaller.flush();
         marshaller.clearClassCache();
         if (! (unmarshaller.readObject() instanceof HashSet)) {
            throw new IOException("didn't get HashSet");
         }
      }
      return (int) (System.currentTimeMillis() - start);
   }
   
//   class AcceptThread extends Thread {
//      public void run() {
//         try {
//            ServerSocket ss = new ServerSocket(2345);
//            while (running) {
//               ss.accept();
//            }
//         }
//         catch (IOException e) {
//            e.printStackTrace();
//         }
//      }
//   }
   
   static class TestComplexObject implements Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = 6622787935922260900L;
      
      private boolean b;
      private int i;
      private float f;
      private double d;
      private String s;
      private Set<?> set;
      
      public TestComplexObject(boolean b, int i, float f, double d, String s, Set<?> set) {
         this.b = b;
         this.i = i;
         this.f = f;
         this.d = d;
         this.s = s;
         this.set = set;
      }
      
      public int hashCode() {
         return Double.toString((b ? 1 : 2) * i * f * d * s.hashCode() * ((set == null) ? 1 : set.hashCode())).hashCode();
      }
      
      public boolean equals(Object o) {
         if (!(o instanceof TestComplexObject)) {
            return false;
         }
         TestComplexObject c = (TestComplexObject) o;
         return c.b == b && c.i == i && Float.compare(f, c.f) == 0 && s.equals(c.s)
                && ((set == null) ? c.set == null : set.equals(c.set));
      }
   }
}

