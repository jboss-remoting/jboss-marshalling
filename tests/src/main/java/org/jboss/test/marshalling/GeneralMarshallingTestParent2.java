package org.jboss.test.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.AbstractClassResolver;
import org.jboss.marshalling.serialization.java.JavaSerializationMarshallerFactory;

/**
 * GeneralMarshallingTestParent is intended to be an implementation independent characterization
 * of the semantics expected of any marshaller/unmarshaller implementation.  Some tests vefity that
 * behaviors which are typically implemented in less than naive fashion, e.g., repeated transmission of
 * an object, are handled correctly.
 * 
 * There are three sections in this class:
 * 
 * 1. the tests in the first section use implementations of org.jboss.marshalling.MarshallerFactory
 *    with default implementations of the adjunct objects such as org.jboss.marshalling.StreamHeader
 *    
 * 2. the tests in the second section use custom implementations of these adjunct objects
 * 
 * 3. the third section contains the definition of classes used in the tests.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 20, 2008
 * </p>
 */
public abstract class GeneralMarshallingTestParent2 extends TestCase implements Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -8328320670610062142L;
   
   private TestComplexObject tco;
   
   public GeneralMarshallingTestParent2() {}

   public void setUp() {
      TestCreator.creators.clear();
      TestExternalizerFactory.externalizers.clear();
      TestComplexObject tco1 = new TestComplexObject(false, (byte) 3, '7', (short) 11, 13, 17L, 19.0f, 23.0d, "seventeen", null);
      TestComplexObject tco2 = new TestComplexObject(true,  (byte) 4, '8', (short) 12, 14, 18L, 20.0f, 24.0d, "eighteen", null);
      HashSet<TestComplexObject> set = new HashSet<TestComplexObject>();
      set.add(tco1);
      set.add(tco2);
      tco  = new TestComplexObject(true,  (byte) 5, '9', (short) 13, 15, 19L, 21.0f, 25.0d, "nineteen", set);
   }
   
   public boolean equals(Object o)
   {
      return o instanceof GeneralMarshallingTestParent2;
   }
   
   /***********************************************************************************************
    ***********************************************************************************************
    **                                                                                           **
    **     The first section contains tests of the form testBasicMarshalling<description>.       **
    **     These tests use instances of org.jboss.marshalling.MarshallerFactory in which no      **
    **     special adjunct objects are configured. I.e., the implementations of                  **
    **     org.jboss.marshalling.StreamHeader, org.jboss.marshaller.ExternalizerFactory,         **
    **     etc., are the default implementations provided by the marshaller/unmarshaller         **
    **     implementation.                                                                       **
    **                                                                                           **
    ***********************************************************************************************
    ***********************************************************************************************/
   
   /**
    * Verify that the null value is handled correctly.
    */
   public void testBasicMarshallingNull() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      doMarshallingTest(factory, new MarshallingConfiguration(), null);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that Integer Objects are handled correctly.
    */
   public void testBasicMarshallingInteger() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      Object o = new Integer(3);
      doMarshallingTest(factory, new MarshallingConfiguration(), o);
      System.out.println(getName() + " PASSES");
   }

   /**
    * Verify that org.jboss.marshalling.TestComplexObject, which has a variety of
    * primitive and reference fields, is handled correctly. 
    */
   public void testBasicMarshallingComplexObject() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      doMarshallingTest(factory, new MarshallingConfiguration(), tco);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that this object is handled correctly.
    */
   public void testBasicMarshallingThis() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      Object o = this;
      doMarshallingTest(factory, new MarshallingConfiguration(), o);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that objects implementing java.io.Externalizable are handled correctly.
    */
   public void testBasicMarshallingExternalizable() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      Object o = new TestExternalizable(3, "seven", new Double(11.0));
      doMarshallingTest(factory, new MarshallingConfiguration(), o);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that objects with transient fields are handled correctly.
    */
   public void testBasicMarshallingTransientFields() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      TestTransientFields o = new TestTransientFields(7, "eleven");
      Object o2 = doMarshallingTest(factory, new MarshallingConfiguration(), o);
      assertTrue(o.ok(o2));
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify thta objects with writeObject() and readObject() methods are handled correctly.
    */
   public void testBasicMarshallingWithWriteObject() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      TestSerializableWithWriteObject o = new TestSerializableWithWriteObject(3, 7, 11);
      Object o2 = doMarshallingTest(factory, new MarshallingConfiguration(), o);
      assertTrue(o.ok(o2));
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that objects with writeReplace() and objects with readResolve() methods are
    * handled correctly.
    */
   public void testBasicMarshallingWithWriteReplace() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      TestSerializableWithWriteReplace o = new TestSerializableWithWriteReplace(3, 7);
      doMarshallingTest(factory, new MarshallingConfiguration(), o);
      assertTrue(o.ok(1));
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verifies that marshallers and unmarshallers can be stopped and restarted.
    */
   public void testBasicMarshallingWithRepeatedUse() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      Marshaller marshaller = factory.createMarshaller(config);
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      ArrayList<TestComplexObject> l = new ArrayList<TestComplexObject>();
      l.add(tco);
      l.add(tco);
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObject(l);
      marshaller.flush();
      marshaller.finish();
      
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ByteInput byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      Object o2 = unmarshaller.readObject();
      unmarshaller.finish();
      assertEquals(l, o2);
      
      baos = new ByteArrayOutputStream();
      byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObject(l);
      marshaller.flush();
      marshaller.finish();
      
      bais = new ByteArrayInputStream(baos.toByteArray());
      byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      o2 = unmarshaller.readObject();
      unmarshaller.finish();
      assertEquals(l, o2);
      
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * @TODO: Rewrite the discussion of replacement semantics
    * 
    * JBossMarshalling semantics requires that, when an object's writeReplace() method returns
    * a replacement object distinct from the original object, the replacement objects's writeReplace()
    * method, if any, must called as well.  If a new replacement object is returned, than that
    * object's writeReplace() method must be called.  And so on.
    * 
    * This test verifies that objects which lead to multiple writeReplace() calls are handled correctly.
    */
   public void testBasicMarshallingWithMultipleWriteReplace() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      TestSerializableWithMultipleWriteReplace o = new TestSerializableWithMultipleWriteReplace(3, TestEnum.FIRST);
      doMarshallingTest(factory, new MarshallingConfiguration(), o);
      assertTrue(o.ok(3));
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that enums are handled correctly.
    */
   public void testBasicMarshallingWithEnum() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      TestSerializableWithEnum o = new TestSerializableWithEnum(TestEnum.FIRST);
      doMarshallingTest(factory, new MarshallingConfiguration(), o);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that arrays are handled correctly.
    */
   public void testBasicMarshallingWithArray() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      TestSerializableWithArray o = new TestSerializableWithArray(new Object[] {"three", new Integer(7), new Float(11.13)});
      doMarshallingTest(factory, new MarshallingConfiguration(), o);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Repeated transmission of an object is typically implemented by transmitting the
    * object once and then transmitting references to the first transmission.
    * 
    * This test verifies that repeated transmissions are handled correctly.
    *
    * Note that this test addresses implementation behavior.
    */
   public void testBasicMarshallingWithRepeatedObject() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      ArrayList<TestComplexObject> l = new ArrayList<TestComplexObject>();
      l.add(tco);
      l.add(tco);
      doMarshallingTest(factory, new MarshallingConfiguration(), l);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Repeated transmission of an object is typically implemented by transmitting the
    * object once and then transmitting references to the first transmission.  However,
    * typically, this behavior can be overridden by flushing internal caches after each
    * transmission of the object.
    * 
    * This test verifies that repeated transmissions, with cache flushing, are handled correctly.
    *
    * Note that this test addresses implementation behavior.
    */
   public void testBasicMarshallingWithRepeatedObjectAndReset() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      doMarshallingTestWithRepeatedObjectAndReset(factory, new MarshallingConfiguration(), tco);
      System.out.println(getName() + " PASSES");
   }

   /**
    * Verify that the org.jboss.marshalling.Marshaller.writeObjectUnshared() and
    * org.jboss.marshalling.Unmarshaller.readObjectUnshared() methods work correctly.
    */
   public void testBasicMarshallingWithUnsharedObject() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      ArrayList<TestComplexObject> l = new ArrayList<TestComplexObject>();
      l.add(tco);
      l.add(tco);
      doMarshallingTestUnshared(factory, new MarshallingConfiguration(), l);
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that classes which declare a serialPersistentFields fiield, as defined in 
    * the Java serialization specification, are handled correctly.
    */
   public void testBasicMarshallingPersistentFields() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory =  new JavaSerializationMarshallerFactory();
      TestPersistentFields o = new TestPersistentFields(7, "eleven");
      Object o2 = doMarshallingTest(factory, new MarshallingConfiguration(), o);
      assertTrue(o.ok(o2));
      System.out.println(getName() + " PASSES");
   }
   
   /***********************************************************************************************
    ***********************************************************************************************
    **                                                                                           **
    **     The second section contains tests of the form test<description> These tests use       **
    **     instances of org.jboss.marshalling.MarshallerFactory configured with customized       **
    **     implementations of adjunct objects, e.g., org.jboss.marshalling.StreamHeader,         **
    **     org.jboss.marshaller.ExternalizerFactory, etc.                                        **
    **                                                                                           **
    ***********************************************************************************************
    ***********************************************************************************************/
   
   /**
    * Verify that use of customized StreamHeader works correctly.
    */
   public void testStreamHeader() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestStreamHeader streamHeader = new TestStreamHeader();
      config.setStreamHeader(streamHeader);
      doMarshallingTest(factory, config, tco);
      assertTrue(streamHeader.ok());
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that use of customized ClassResolver works correctly with regular classes.
    */
   public void testClassResolver() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestClassResolver classResolver = new TestClassResolver();
      config.setClassResolver(classResolver);
      doMarshallingTest(factory, config, tco);
      assertTrue(classResolver.ok());
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that use of customized ClassResolver works correctly with proxy classes.
    */
   public void testProxyClassResolver() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestProxyClassResolver classResolver = new TestProxyClassResolver();
      config.setClassResolver(classResolver);
      Object p = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{i1.class, i2.class}, new TestTargetClass());
      doMarshallingTest(factory, config, p);
      assertTrue(classResolver.ok());
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that use of customized ObjectResolver works correctly.
    */
   public void testObjectResolver() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestObjectResolver objectResolver = new TestObjectResolver();
      config.setObjectResolver(objectResolver);
      HashSet<Object> h = new HashSet<Object>();
      h.add(new Integer(7));
      h.add(new Integer(11));
      h.add("testString");
      doMarshallingTest(factory, config, h);
      assertTrue(objectResolver.ok());
      System.out.println(getName() + " PASSES");
   }
   
   // @TODO update to comply with resolution of writeReplace() concerns.
   public void testObjectResolverWithInterleavedWriteReplace() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestObjectResolverForInterleavedWriteReplace objectResolver = new TestObjectResolverForInterleavedWriteReplace();
      config.setObjectResolver(objectResolver);
      TestSerializableWithInterleavedWriteReplace o = new TestSerializableWithInterleavedWriteReplace(3, TestEnum.FIRST);
      doMarshallingTest(factory, config, o);
      assertTrue(o.ok(4));
      assertTrue(objectResolver.ok());
      System.out.println(getName() + " PASSES");
   }
   
   /*
    * Verify that use of customized ExternalizerFactory works correctly in
    * presence of default Creator.
    */
   public void testExternalizerWithDefaultCreator() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory marshallerFactory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestExternalizerFactory externalizerFactory = new TestExternalizerFactory(false);
      config.setExternalizerFactory(externalizerFactory);
      HashSet<Object> h = new HashSet<Object>();
      h.add(new TestExternalizableInt(7));
      h.add(new TestExternalizableString("eleven"));
      h.add("testString");
      doMarshallingTest(marshallerFactory, config, h);
      Set<TestExternalizer> externalizers = TestExternalizerFactory.externalizers;
      assertEquals(4, externalizers.size());
      for (TestExternalizer externalizer : externalizers) {
         assertTrue(externalizer.ok(1));
      }
      System.out.println(getName() + " PASSES");
   }
   
   /*
    * Verify that use of customized ExternalizerFactory to write the same object twice
    * results in sending a handle on the second write.
    */
   public void testExternalizerWithRepeatedWrites() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory marshallerFactory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestExternalizerFactory externalizerFactory = new TestExternalizerFactory(false);
      config.setExternalizerFactory(externalizerFactory);
      Object o = new TestExternalizableInt(7);
      doMarshallingTestWithRepeatedWrites(marshallerFactory, config, o);
      System.out.println(getName() + " PASSES");
   }
   
   /*
    * Verify that use of customized ExternalizerFactory works correctly in
    * presence of customized Creator.
    */
   public void testExternalizerWithConfiguredCreator() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory marshallerFactory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestExternalizerFactory externalizerFactory = new TestExternalizerFactory(true);
      config.setExternalizerFactory(externalizerFactory);
      TestCreator creator = new TestCreator();
      config.setCreator(creator);
      HashSet<Object> h = new HashSet<Object>();
      h.add(new TestExternalizableInt(13));
      h.add(new TestExternalizableString("seventeen"));
      h.add("anotherTestString");
      doMarshallingTest(marshallerFactory, config, h);
      Set<TestExternalizer> externalizers = TestExternalizerFactory.externalizers;
      assertEquals(4, externalizers.size());
      for (TestExternalizer externalizer : externalizers) {
         assertTrue(externalizer.ok(1));
      }
      assertTrue(creator.ok(2));
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that use of customized ClassTable works correctly.
    */
   public void testClassTable() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory marshallerFactory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestClassTable classTable = new TestClassTable();
      config.setClassTable(classTable);
      HashSet<Object> h = new HashSet<Object>();
      h.add(new TestExternalizableInt(13));
      h.add(new TestExternalizableString("seventeen"));
      h.add("anotherTestString");
      doMarshallingTest(marshallerFactory, config, h);
      assertTrue(classTable.ok(2));
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Verify that use of customized ObjectTable works correctly.
    */
   public void testObjectTable() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory marshallerFactory = getMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestObjectTable objectTable = new TestObjectTable();
      config.setObjectTable(objectTable);
      HashSet<Object> h = new HashSet<Object>();
      h.add(objectTable.getTco1());
      h.add(objectTable.getTco2());
      h.add("anotherTestString");
      doMarshallingTest(marshallerFactory, config, h);
      assertTrue(objectTable.ok(2));
      System.out.println(getName() + " PASSES");
   }
   
   /**
    * Returns MarshallerFactory for use in tests.
    */
   protected abstract MarshallerFactory getMarshallerFactory();
   
   protected Object doMarshallingTest(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {
      Marshaller marshaller = factory.createMarshaller(config);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObject(o);
      marshaller.flush();
      
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ByteInput byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      Object o2 = unmarshaller.readObject();
      assertEquals(o, o2); 
      return o2;
   }
   
   protected void doMarshallingTestWithRepeatedObjectAndReset(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {
      Marshaller marshaller = factory.createMarshaller(config);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObject(o);
      marshaller.flush();
      marshaller.clearClassCache();
      marshaller.clearInstanceCache();
      marshaller.writeObject(o);
      marshaller.flush();
      
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ByteInput byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      Object o2 = unmarshaller.readObject();
      assertEquals(o, o2);
      o2 = unmarshaller.readObject();
      assertEquals(o, o2);
   }
   
   protected Object doMarshallingTestUnshared(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {
      Marshaller marshaller = factory.createMarshaller(config);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObjectUnshared(o);
      marshaller.writeObjectUnshared(o);
      marshaller.flush();
      
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ByteInput byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      Object o2 = unmarshaller.readObjectUnshared();
      Object o3 = unmarshaller.readObjectUnshared();
      assertEquals(o, o2);
      assertEquals(o, o3);
      return o2;
   }
   
   protected void doMarshallingTestWithRepeatedWrites(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {
      Marshaller marshaller = factory.createMarshaller(config);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObject(o);
      marshaller.writeObject(o);
      marshaller.flush();
      
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ByteInput byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      Object o2 = unmarshaller.readObject();
      assertEquals(o, o2);
      Object o3 = unmarshaller.readObject();
      assertSame(o2, o3);
   }
   
   /***********************************************************************************************
    ***********************************************************************************************
    **                                                                                           **
    **     The third section contains classes used by the tests in the first two sections.       **
    **                                                                                           **
    ***********************************************************************************************
    ***********************************************************************************************/
   
   static class TestExternalizableSuper implements Externalizable {
      private int i;
      private String s;
      
      public TestExternalizableSuper(int i, String s) {
         this.i = i;
         this.s = s;
      }
      public TestExternalizableSuper() {
      }
      public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
         i = in.readInt();
         s = in.readUTF();
      }
      public void writeExternal(ObjectOutput out) throws IOException {
         out.writeInt(i);
         out.writeUTF(s);
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestExternalizableSuper)) {
            return false;
         }
         TestExternalizableSuper other = (TestExternalizableSuper) o;
         return i == other.i && s.equals(other.s);
      }
   }
   
   static class TestExternalizable extends TestExternalizableSuper implements Externalizable {
      private Double d;
      
      public TestExternalizable(int i, String s, Double d) {
         super(i, s);
         this.d = d;
      }
      public TestExternalizable() {
      }
      public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
         super.readExternal(in);
         d = new Double(in.readDouble());
      }
      public void writeExternal(ObjectOutput out) throws IOException {
         super.writeExternal(out);
         out.writeDouble(d.doubleValue());
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestExternalizable)) {
            return false;
         }
         TestExternalizable other = (TestExternalizable) o;
         return super.equals(o) && d.equals(other.d);
      }
   }
   
   static class TestTransientFields implements Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
      private transient int i;
      private String s;
      public TestTransientFields(int i, String s) {
         this.i = i;
         this.s = s;
      }
      public TestTransientFields() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestTransientFields)) {
            return false;
         }
         TestTransientFields other = (TestTransientFields) o;
         return this.s.equals(other.s);
      }
      public boolean ok(Object o) {
         if (!equals(o)) {
            return false;
         }
         TestTransientFields other = (TestTransientFields) o;
         return other.i == 0;
      }
   }
   
   static class TestBaseClass implements Serializable {
      private static final long serialVersionUID = 1L;
      private int i;
      public TestBaseClass(int i) {this.i = i;}
      public TestBaseClass() {}
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestBaseClass)) {
            return false;
         }
         return i == ((TestBaseClass)o).i;
      }
   }
   
   static class TestSerializableWithWriteObject extends TestBaseClass implements Serializable {
      private static final long serialVersionUID = 1L;
      private transient Integer j;
      private transient Integer factor;
      
      public TestSerializableWithWriteObject(int i, int j, int factor) {
         super(i);
         this.j = j;
         this.factor = factor;
      }
      public TestSerializableWithWriteObject() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestSerializableWithWriteObject)) {
            return false;
         }
         return super.equals(o);
      }
      public boolean ok(Object o) {
         return equals(o) && ((TestSerializableWithWriteObject)o).j == j * factor;
      }
      private void writeObject(ObjectOutputStream stream) throws IOException {
         System.out.println(this + ".writeObject()");
         stream.defaultWriteObject();
         stream.writeObject(new Integer(j * factor));
      }
      private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
         System.out.println(this + ".readObject()");
         stream.defaultReadObject();
         j = (Integer) stream.readObject();
      }
   }
   
   static class TestSerializableWithWriteReplaceHolder implements Serializable {
      private static final long serialVersionUID = 1L;
      private int i;
      private int j;
      public TestSerializableWithWriteReplaceHolder(TestSerializableWithWriteReplace original) {
         this.i = original.i;
         this.j = original.j;
      }
      public TestSerializableWithWriteReplaceHolder() {            
      }
      public Object readResolve() throws ObjectStreamException {
         System.out.println(this + ".readResolve()");
         return new TestSerializableWithWriteReplace(this);
      }
   }
   
   static class TestSerializableWithWriteReplace implements Serializable {
      private static final long serialVersionUID = 1L;
      private static int replaceCounter;
      private static int resolveCounter;
      private Integer i;
      private Integer j;
      
      public TestSerializableWithWriteReplace(int i, int j) {
         this.i = i;
         this.j = j;
      }
      public TestSerializableWithWriteReplace(TestSerializableWithWriteReplaceHolder holder) {
         resolveCounter++;
         this.i = holder.i;
         this.j = holder.j;
      }
      public TestSerializableWithWriteReplace() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestSerializableWithWriteReplace)) {
            return false;
         }
         TestSerializableWithWriteReplace other = (TestSerializableWithWriteReplace) o;
         return i.equals(other.i) && j.equals(other.j);
      }
      public boolean ok(int count) {
         return replaceCounter == count && resolveCounter == count;
      }
      public Object writeReplace() throws ObjectStreamException {
         System.out.println(this + ".writeReplace()");
         replaceCounter++;
         return new TestSerializableWithWriteReplaceHolder(this);
      }
   }
   
   static class TestSerializableWithMultipleWriteReplace implements Serializable {
      private static final long serialVersionUID = 1L;
      private static int replaceCounter;
      private static int resolveCounter;
      private Integer i;
      private TestEnum te;
      
      public TestSerializableWithMultipleWriteReplace(int i, TestEnum te) {
         this.i = i;
         this.te = te;
      }
      public TestSerializableWithMultipleWriteReplace() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestSerializableWithMultipleWriteReplace)) {
            return false;
         }
         TestSerializableWithMultipleWriteReplace other = (TestSerializableWithMultipleWriteReplace) o;
         return i.equals(other.i) && te.equals(other.te);
      }
      public boolean ok(int count) {
         return 0 < replaceCounter && replaceCounter <= count && resolveCounter == 1;
      }
      public Object writeReplace() throws ObjectStreamException {
         System.out.println(this + ".writeReplace()");
         replaceCounter++;
         if (te.equals(TestEnum.FIRST)) {
            return new TestSerializableWithMultipleWriteReplace2(i, te.next());
         }
         if (te.equals(TestEnum.SECOND)) {
            return new TestSerializableWithMultipleWriteReplace3(i, te.next());
         }
         return this;
      }
      public Object readResolve() throws ObjectStreamException {
         System.out.println(this + ".readResolve()");
         resolveCounter++;
//         if (te.equals(TestEnum.FIRST)) {
//            return this;
//         }
//         if (te.equals(TestEnum.SECOND)) {
//            return new TestSerializableWithMultipleWriteReplace(i, te.previous());
//         }
//         return new TestSerializableWithMultipleWriteReplace2(i, te.previous());
         
         // Note that the serialization specification describes a single resolution step on the
         // input side (http://java.sun.com/javase/6/docs/platform/serialization/spec/input.html#5903, item 12).
         return new TestSerializableWithMultipleWriteReplace(i, TestEnum.FIRST);
      }
   }
   
   static class TestSerializableWithMultipleWriteReplace2 extends TestSerializableWithMultipleWriteReplace implements Serializable {
      public TestSerializableWithMultipleWriteReplace2(int i, TestEnum te) {super(i, te);}
      public TestSerializableWithMultipleWriteReplace2() {}
   }
   
   static class TestSerializableWithMultipleWriteReplace3 extends TestSerializableWithMultipleWriteReplace2 implements Serializable {
      public TestSerializableWithMultipleWriteReplace3(int i, TestEnum te) {super(i, te);}
      public TestSerializableWithMultipleWriteReplace3() {}
   }
   
   static class TestSerializableWithInterleavedWriteReplace implements Serializable {
      private static final long serialVersionUID = 1L;
      private static int replaceCounter;
      private static int resolveCounter;
      private Integer i;
      private TestEnum te;
      
      public TestSerializableWithInterleavedWriteReplace(int i, TestEnum te) {
         this.i = i;
         this.te = te;
      }
      public TestSerializableWithInterleavedWriteReplace() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestSerializableWithInterleavedWriteReplace)) {
            return false;
         }
         TestSerializableWithInterleavedWriteReplace other = (TestSerializableWithInterleavedWriteReplace) o;
         return i.equals(other.i) && te.equals(other.te);
      }
      public boolean ok(int count) {
         return 0 < replaceCounter && replaceCounter <= count && resolveCounter == 1;
      }
      public Object writeReplace() throws ObjectStreamException {
         System.out.println(this + ".writeReplace()");
         replaceCounter++;
         if (te.equals(TestEnum.FIRST)) {
            return new TestSerializableWithInterleavedWriteReplace2(i, te.next());
         }
         if (te.equals(TestEnum.SECOND)) {
            return new TestSerializableWithInterleavedWriteReplace3(i, te.next());
         }
         if (te.equals(TestEnum.THIRD)) {
            return new TestSerializableWithInterleavedWriteReplace4(i, te.next());
         }
         return this;
      }
      public Object readResolve() throws ObjectStreamException {
         System.out.println(this + ".readResolve()");
         resolveCounter++;
         if (te.equals(TestEnum.FIFTH)) {
            return new TestSerializableWithInterleavedWriteReplace2(i, TestEnum.SECOND);
         }
         if (te.equals(TestEnum.FOURTH)) {
            return new TestSerializableWithInterleavedWriteReplace2(i, TestEnum.SECOND);
         }
         if (te.equals(TestEnum.THIRD)) {
            return new TestSerializableWithInterleavedWriteReplace2(i, TestEnum.SECOND);
         }
         else {
            return this;
         }
      }
   }
   
   static class TestSerializableWithInterleavedWriteReplace2 extends TestSerializableWithInterleavedWriteReplace implements Serializable {
      public TestSerializableWithInterleavedWriteReplace2(int i, TestEnum te) {super(i, te);}
      public TestSerializableWithInterleavedWriteReplace2() {}
   }
   
   static class TestSerializableWithInterleavedWriteReplace3 extends TestSerializableWithInterleavedWriteReplace2 implements Serializable {
      public TestSerializableWithInterleavedWriteReplace3(int i, TestEnum te) {super(i, te);}
      public TestSerializableWithInterleavedWriteReplace3() {}
   }
   
   static class TestSerializableWithInterleavedWriteReplace4 extends TestSerializableWithInterleavedWriteReplace3 implements Serializable {
      public TestSerializableWithInterleavedWriteReplace4(int i, TestEnum te) {super(i, te);}
      public TestSerializableWithInterleavedWriteReplace4() {}
   }
   
   static class TestSerializableWithInterleavedWriteReplace5 extends TestSerializableWithInterleavedWriteReplace4 implements Serializable {
      public TestSerializableWithInterleavedWriteReplace5(int i, TestEnum te) {super(i, te);}
      public TestSerializableWithInterleavedWriteReplace5() {}
   }
   
   static class TestObjectResolverForInterleavedWriteReplace implements ObjectResolver {
      private int resolveCounter;
      private int replaceCounter;
      
      public Object readResolve(Object replacement) {
         if (! (replacement instanceof TestSerializableWithInterleavedWriteReplace)) {
            return replacement;
         }
         System.out.println(this + ".readResolve(): " + replacement);
         resolveCounter++;
         TestSerializableWithInterleavedWriteReplace tsw = (TestSerializableWithInterleavedWriteReplace) replacement;
         if (tsw.te.equals(TestEnum.SECOND)) {
            return new TestSerializableWithInterleavedWriteReplace(tsw.i, tsw.te.previous());
         }
         else {
            return replacement;
         }
      }

      public Object writeReplace(Object original) {
         if (! (original instanceof TestSerializableWithInterleavedWriteReplace)) {
            return original;
         }
         System.out.println(this + ".writeReplace(): " + original);
         replaceCounter++;
         TestSerializableWithInterleavedWriteReplace tsw = (TestSerializableWithInterleavedWriteReplace) original;
         if (tsw.te.equals(TestEnum.FIRST)) {
            return new TestSerializableWithInterleavedWriteReplace2(tsw.i, tsw.te.next());
         }
         if (tsw.te.equals(TestEnum.SECOND)) {
            return new TestSerializableWithInterleavedWriteReplace3(tsw.i, tsw.te.next());
         }
         if (tsw.te.equals(TestEnum.THIRD)) {
            return new TestSerializableWithInterleavedWriteReplace4(tsw.i, tsw.te.next());
         }
         if (tsw.te.equals(TestEnum.FOURTH)) {
            return new TestSerializableWithInterleavedWriteReplace5(tsw.i, tsw.te.next());
         }
         else {
            return original;
         }
      }
      
      public boolean ok() {
         return replaceCounter == 1 && resolveCounter == 1;
      }
   }
   
   public enum TestEnum {
      FIRST  { TestEnum next() { return SECOND; }; TestEnum previous() { return FIFTH; } },
      SECOND { TestEnum next() { return THIRD;  }; TestEnum previous() { return FIRST;  } },
      THIRD  { TestEnum next() { return FOURTH; }; TestEnum previous() { return SECOND; } },
      FOURTH { TestEnum next() { return FIFTH;  }; TestEnum previous() { return THIRD;  } },
      FIFTH  { TestEnum next() { return FIRST;  }; TestEnum previous() { return FOURTH; } };
      abstract TestEnum next();
      abstract TestEnum previous();
    }


   static class TestSerializableWithEnum implements Serializable {
      private static final long serialVersionUID = 1L;
      private TestEnum secret;

      public TestSerializableWithEnum(TestEnum secret) {
         this.secret = secret;
      }
      public TestSerializableWithEnum() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestSerializableWithEnum)) {
            return false;
         }
         return secret.equals(((TestSerializableWithEnum)o).secret);
      }
   }

   static class TestSerializableWithArray implements Serializable {
      private static final long serialVersionUID = 1L;
      private Object[] array;

      public TestSerializableWithArray(Object[] array) {
         this.array = array;
      }
      public TestSerializableWithArray() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestSerializableWithArray)) {
            return false;
         }
         TestSerializableWithArray other = (TestSerializableWithArray) o;
         if (array.length != other.array.length) {
            return false;
         }
         for (int i = 0; i < array.length; i++) {
            if (! array[i].equals(other.array[i])) {
               return false;
            }
         }
         return true;
      }
   }
   
   static class TestPersistentFields implements Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
      private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("s", String.class)};

      private int i;
      private String s;
      
      public TestPersistentFields(int i, String s) {
         this.i = i;
         this.s = s;
      }
      public TestPersistentFields() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestPersistentFields)) {
            return false;
         }
         TestPersistentFields other = (TestPersistentFields) o;
         return this.s.equals(other.s);
      }
      public boolean ok(Object o) {
         if (!equals(o)) {
            return false;
         }
         TestPersistentFields other = (TestPersistentFields) o;
         return other.i == 0;
      }
   }
   
   static class TestStreamHeader implements StreamHeader {
      private short B1 = (short)1234;
      private short B2 = (short)4321;
      private boolean readVisited;
      private boolean writeVisited;
      
      public void readHeader(Unmarshaller input) throws IOException {
         readVisited = true;
         System.out.println("readHeader() visited");
         short b1 = input.readShort();
         short b2 = input.readShort();
         System.out.println("b1: " + b1 + ", b2: " + b2);
         if (b1 != B1 || b2 != B2) {
               throw new StreamCorruptedException("invalid stream header");
         }
      }

      public void writeHeader(Marshaller output) throws IOException {
         writeVisited = true;
         System.out.println("writeHeader() visited");
         output.writeShort(B1);
         output.writeShort(B2);
      }
      
      public boolean ok() {
         return readVisited && writeVisited;
      }
   }
   
   static class TestClassResolver extends AbstractClassResolver {
      private String[] storedClassNames = new String[16];
      private int counter = -1;
      private boolean writing = true;
      private boolean annotateVisited;
      private boolean resolveVisited;

       protected ClassLoader getClassLoader() {
           return null;
       }

       public void annotateClass(Marshaller marshaller, Class<?> clazz) throws IOException {
         annotateVisited = true;
         System.out.println("annotateClass() visited");
         storedClassNames[++counter] = "|" + clazz.getName() + "|";
         System.out.println("storedClassName: " + storedClassNames[counter]);
         marshaller.writeUTF(storedClassNames[counter]);
      }

      public void annotateProxyClass(Marshaller marshaller, Class<?> proxyClass) throws IOException { 
      }

      public Class<?> resolveClass(Unmarshaller unmarshaller, String name, long serialVersionUID) throws IOException, ClassNotFoundException {
         resolveVisited = true;
         System.out.println("resolveClass() visited");
         if (writing) {
            writing = false;
            counter = - 1;
         }
         String className = unmarshaller.readUTF();
         System.out.println("className: " + className);
         if (!storedClassNames[++counter].equals(className))
            throw new IOException("Didn't read: " + storedClassNames[counter]);
         return Class.forName(name);
      }

      public Class<?> resolveProxyClass(Unmarshaller unmarshaller, String[] interfaces) throws IOException, ClassNotFoundException {
         return null;
      }
      
      public boolean ok() {
         return annotateVisited && resolveVisited;
      }
   }

   static class TestProxyClassResolver extends AbstractClassResolver {
      private String[] storedClassNames = new String[16];
      private int counter = -1;
      private boolean writing = true;
      private boolean annotateVisited;
      private boolean resolveVisited;

       protected ClassLoader getClassLoader() {
           return null;
       }

       public void annotateClass(Marshaller marshaller, Class<?> clazz) throws IOException {
         System.out.println("annotateClass() visited");
      }

      public void annotateProxyClass(Marshaller marshaller, Class<?> proxyClass) throws IOException {
         annotateVisited = true;
         System.out.println("annotateProxyClass() visited");
         storedClassNames[++counter] = "|" + proxyClass.getName() + "|";
         System.out.println("storedClassName: " + storedClassNames[counter]);
         marshaller.writeUTF(storedClassNames[counter]);
      }

      public Class<?> resolveClass(Unmarshaller unmarshaller, String name, long serialVersionUID) throws IOException, ClassNotFoundException {
         System.out.println("resolveClass() visited");
         return Class.forName(name);
      }

      public Class<?> resolveProxyClass(Unmarshaller unmarshaller, String[] interfaces) throws IOException, ClassNotFoundException {
         resolveVisited = true;
         System.out.println("resolveProxyClass() visited");
         if (writing) {
            writing = false;
            counter = - 1;
         }
         String className = unmarshaller.readUTF();
         System.out.println("className: " + className);
         if (!storedClassNames[++counter].equals(className))
            throw new IOException("Didn't read: " + storedClassNames[counter]);
         Class<?>[] classes = new Class[interfaces.length];
         for (int i = 0; i < classes.length; i++) {
            classes[i] = Class.forName(interfaces[i]);
         }
         return Proxy.getProxyClass(getClass().getClassLoader(), classes);
      }
      
      public boolean ok() {
         return annotateVisited && resolveVisited;
      }
   }
   
   interface i1 {
      int f11();
      String f12();
   }
   
   interface i2 {
      float f21();
      String f22();
   }
   
   static class TestTargetClass implements InvocationHandler, Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = 4036911409414205695L;

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         if ("f11".equals(method.getName())) {
            return 3;
         }
         if ("f12".equals(method.getName())) {
            return "f12.result";
         }
         if ("f21".equals(method.getName())) {
            return "7.0";
         }
         if ("f22".equals(method.getName())) {
            return "f22.result";
         }
         if ("equals".equals(method.getName())) {
            return Proxy.isProxyClass(args[0].getClass());
         }
         System.out.println("unrecognized method name: " + method.getName());
         throw new RuntimeException("unrecognized method: " + method.getName());
      }
   }
   
   static class TestObjectResolver implements ObjectResolver {
      private boolean resolveVisited;
      private boolean replaceVisited;
      
      public Object readResolve(Object replacement) {
         if (replacement instanceof IntegerHolder) {
            resolveVisited = true;
            System.out.println("readResolve() visited");
            return new Integer(((IntegerHolder)replacement).getInteger());
         }
         else {
            return replacement;
         }
      }

      public Object writeReplace(Object original) {
         if (original instanceof Integer) {
            replaceVisited = true;
            System.out.println("writeReplace() visited");
            return new IntegerHolder(((Integer) original).intValue());
         }
         else {
            return original;
         }
      }
      
      public boolean ok() {
         return resolveVisited && replaceVisited;
      }
   }
   
   static class IntegerHolder implements Serializable{
      /** The serialVersionUID */
      private static final long serialVersionUID = 6644641943325833282L;
      private int held;
      public IntegerHolder(int integer) { held = integer; }
      public IntegerHolder() {}
      public int getInteger() { return held; }
   }
   
   static class TestExternalizerFactory implements ExternalizerFactory {
      public static HashSet<TestExternalizer> externalizers = new HashSet<TestExternalizer>();
      private boolean withCreator;
      
      public TestExternalizerFactory(boolean withCreator) {
         this.withCreator = withCreator;
      }
      
      public Externalizer getExternalizer(Object obj) {
         if (TestExternalizableInt.class.isAssignableFrom(obj.getClass())) {
            TestExternalizer externalizer = new TestExternalizer(withCreator);
            return externalizer;
         }
         else if (TestExternalizableString.class.isAssignableFrom(obj.getClass())) {
            TestExternalizer externalizer = new TestExternalizer(withCreator);
            return externalizer;
         }
         else {
            return null;
         }
      }
   }
   
   public static class TestExternalizer implements Externalizer, Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = -8104713864804175542L;
      private int createCounter;
      private int readCounter;
      private int writeCounter;
//      private boolean withCreator;
      
      public TestExternalizer(boolean withCreator) {
         TestExternalizerFactory.externalizers.add(this);
//         this.withCreator = withCreator;
      }
   
      public TestExternalizer() {
         TestExternalizerFactory.externalizers.add(this);
      }
      
      public Object createExternal(Class<?> subjectType, ObjectInput input, Creator defaultCreator)
      throws IOException, ClassNotFoundException {
         if (!TestExternalizableInt.class.isAssignableFrom(subjectType) &&
             !TestExternalizableString.class.isAssignableFrom(subjectType)) {
            throw new IOException(this + " only works for " + TestExternalizableInt.class + 
                                  " or " + TestExternalizableString.class);
         }
         
         Object obj = null;
         try {
            if (defaultCreator != null) {
               obj = defaultCreator.create(subjectType);
            }
            else {
               obj = Class.forName(subjectType.getName());
            }
         }
         catch (Exception e) {
            throw new IOException(e + "\n" + e.getMessage());
         }
         
//         readExternal(obj, input);
         createCounter++;
         return obj;
      }

      public void readExternal(Object subject, ObjectInput input) throws IOException, ClassNotFoundException {
         TestExternalizerFactory.externalizers.add(this);
         if (TestExternalizableInt.class.isAssignableFrom(subject.getClass())) {
            System.out.println(this + " reading  " + subject.getClass());
            readCounter++;
            ((TestExternalizableInt) subject).setSecret(input.readInt());
         }
         else if (TestExternalizableString.class.isAssignableFrom(subject.getClass())) {
            System.out.println(this + " reading " + subject.getClass());
            readCounter++;;
            ((TestExternalizableString) subject).setSecret(input.readUTF());
         }
         else {
            throw new IOException(this + " only works for " + TestExternalizableInt.class + 
                                  " or " + TestExternalizableString.class);
         }
      }

      public void writeExternal(Object subject, ObjectOutput output) throws IOException {
         if (TestExternalizableInt.class.isAssignableFrom(subject.getClass())) {
            System.out.println(this + " writing " + subject.getClass());
            writeCounter++;
            output.writeInt(((TestExternalizableInt) subject).getSecret());
         }
         else if (TestExternalizableString.class.isAssignableFrom(subject.getClass())) {
            System.out.println(this + " writing " + subject.getClass());
            writeCounter++;
            output.writeUTF(((TestExternalizableString) subject).getSecret());
         }
         else {
            throw new IOException(this + " only works for " + TestExternalizableInt.class + 
                                  " or " + TestExternalizableString.class);
         } 
      }
      
      public boolean ok(int counter) {
         return (writeCounter > 0  && createCounter == 0 && readCounter == 0 && writeCounter == counter) ||
                (writeCounter == 0 && createCounter == counter && readCounter == counter);
      }
   }
   
   public static class TestExternalizableInt implements Serializable{
      /** The serialVersionUID */
      private static final long serialVersionUID = 805500397903006481L;
      private int secret;
      public TestExternalizableInt(int secret) { this.secret = secret; }
      public TestExternalizableInt() {}
      public int getSecret() { return secret; }
      public void setSecret(int secret) { this.secret = secret; }
      
      public boolean equals(Object o) {
         if (! (o instanceof TestExternalizableInt)) {
            return false;
         }
         return ((TestExternalizableInt)o).secret == this.secret;
      }
      
      public int hashCode() {
         return secret;
      }
   }
   
   public static class TestExternalizableString implements Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
      private String secret;
      public TestExternalizableString(String secret) { this.secret = secret; }
      public TestExternalizableString() {}
      public String getSecret() { return secret; }
      public void setSecret(String secret) { this.secret = secret; }
      
      public boolean equals(Object o) {
         if (! (o instanceof TestExternalizableString)) {
            return false;
         }
         return ((TestExternalizableString)o).secret.equals(this.secret);
      }
      
      public int hashCode() {
         return secret.hashCode();
      }
   }
   
   public static class TestCreator implements Creator, Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = 4129528288404375640L;
      public static Map<Integer, TestCreator> creators = new HashMap<Integer, TestCreator>();
      private static int creatorCounter;
      private int counter;
      
      public TestCreator() {
         creators.put(creatorCounter++, this);
      }
      
      public <T> T create(Class<T> clazz) throws InvalidClassException
      {
         if (!creators.values().contains(this)) {
            creators.put(creatorCounter++, this);
         }
         
         try
         {
            if (TestExternalizableInt.class.isAssignableFrom(clazz)) {
               System.out.println(this + " creating  " + clazz);
               counter++;
               Constructor<T> cons = clazz.getConstructor();
               return cons.newInstance();
            }
            else if (TestExternalizableString.class.isAssignableFrom(clazz)) {
               System.out.println(this + " creating  " + clazz);
               counter++;
               Constructor<T> cons = clazz.getConstructor();
               return cons.newInstance();
            }
            else {
               System.out.println(this + " creating  " + clazz);
               Constructor<T> cons = clazz.getConstructor();
               return cons.newInstance();
            }
         }
         catch (Exception e) {
            throw new InvalidClassException(e.getCause() + ": " + e.getMessage());
         }
      }
      
      public boolean ok(int counter) {
         return creators.size() == 1 && creators.get(0).counter == counter;
      }
   }
   
   public static class TestClassTable implements ClassTable {
      private static final int TestExternalizableIntID = 2;
      private static final int TestExternalizableStringID = 3;
      
      private int readCounter;
      private int writeCounter;
      
      public Writer getClassWriter(Class<?> clazz)
      {
         if (TestExternalizableInt.class.isAssignableFrom(clazz) || TestExternalizableString.class.isAssignableFrom(clazz)) {
            return new Writer() {
               public void writeClass(Marshaller marshaller, Class<?> clazz) throws IOException {
                  byte classID = 1;
                  if (TestExternalizableInt.class.isAssignableFrom(clazz)) {
                     System.out.println(this + " writing " + clazz.toString());
                     writeCounter++;
                     classID = TestExternalizableIntID;
                  }
                  else if (TestExternalizableString.class.isAssignableFrom(clazz)) {
                     System.out.println(this + " writing " + clazz.toString());
                     writeCounter++;
                     classID = TestExternalizableStringID;
                  } 
                  marshaller.writeByte(classID);
               }
            };
         }
         else {
            return null;
         }
      }

      public Class<?> readClass(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException
      {
         byte classID = unmarshaller.readByte();
         if (classID == TestExternalizableIntID) {
            System.out.println(this + " read class ID " + classID);
            readCounter++;
            return TestExternalizableInt.class;
         }
         else if (classID == TestExternalizableStringID) {
            System.out.println(this + " read class ID " + classID);
            readCounter++;
            return TestExternalizableString.class;
         }
         else {
            return null;
         } 
      }
      
      public boolean ok(int counter) {
         return readCounter == counter && writeCounter == counter;
      }
   }
   
   public class TestObjectTable implements ObjectTable {
      private int writeCounter;
      private int readCounter;
      private TestComplexObject tco1 = new TestComplexObject(false, (byte) 6, '9', (short) 14, 16, 20L, 22.0f, 26.0d, "twenty", null);
      private TestComplexObject tco2 = new TestComplexObject(true,  (byte) 7, '1', (short) 15, 17, 21L, 23.0f, 27.0d, "twenty-one", null);
      
      public TestComplexObject getTco1() { return tco1; }
      public TestComplexObject getTco2() { return tco2; }
      
      public Writer getObjectWriter(final Object object) {
         if (object.equals(tco1) || object.equals(tco2)) {
            writeCounter++;
            return new Writer() {
               public void writeObject(Marshaller marshaller, Object object) throws IOException {
                  if (object.equals(tco1)) {
                     System.out.println("writing 1 for " + tco1);
                     marshaller.writeInt(1);
                  }
                  else if (object.equals(tco2)) {
                     System.out.println("writing 2 for " + tco2);
                     marshaller.writeInt(2);
                  }
                  else {
                     throw new RuntimeException("expecting tco1 or tco2");
                  }
               }       
            };
         }
         else {
            return null;
         }
      }

      public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
         int i = unmarshaller.readInt();
         if (i == 1) {
            readCounter++;
            System.out.println("read 1: returning " + tco1);
            return tco1;
         }
         else if (i == 2) {
            readCounter++;
            System.out.println("read 2: returning " + tco2);
            return tco2;
         }
         else {
            throw new RuntimeException("expected 1 or 2");
         }
      }
      
      public boolean ok(int counter) {
         return writeCounter == counter && readCounter == counter;
      }
   }
}
