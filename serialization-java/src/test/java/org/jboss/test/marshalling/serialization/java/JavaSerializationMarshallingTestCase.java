package org.jboss.test.marshalling.serialization.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.AbstractClassResolver;
import org.jboss.marshalling.serialization.java.JavaSerializationMarshallerFactory;
import org.jboss.testsupport.LoggingHelper;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 20, 2008
 * </p>
 */
public class JavaSerializationMarshallingTestCase extends TestCase implements Serializable
{
   static {
      LoggingHelper.init();
   }

   /** The serialVersionUID */
   private static final long serialVersionUID = -8328320670610062142L;
   
   private TestComplexObject tco;

   public void setUp() {
      TestCreator.creators.clear();
      TestExternalizerFactory.externalizers.clear();
      TestComplexObject tco1 = new TestComplexObject(false, 3, (float) 7.0, 11.0, "seventeen", null);
      TestComplexObject tco2 = new TestComplexObject(true, 23, (float) 29.0, 31.0, "thirty-seven", null);
      HashSet<TestComplexObject> set = new HashSet<TestComplexObject>();
      set.add(tco1);
      set.add(tco2);
      tco  = new TestComplexObject(true, 41, (float) 43.0, 47.0, "fifty-three", set);
   }
   
   public boolean equals(Object o)
   {
      return o instanceof JavaSerializationMarshallingTestCase;
   }
   
   public void testBasicMarshallingInteger() throws Throwable
   {
      System.out.println("\nentering " + getName());
      JavaSerializationMarshallerFactory factory = new JavaSerializationMarshallerFactory();
      Object o = new Integer(3);
      doAllTests(factory, new MarshallingConfiguration(), o);
      System.out.println(getName() + " PASSES");
   }

   public void testBasicMarshallingComplexObject() throws Throwable
   {
      System.out.println("\nentering " + getName());
      JavaSerializationMarshallerFactory factory = new JavaSerializationMarshallerFactory();
      doAllTests(factory, new MarshallingConfiguration(), tco);
      System.out.println(getName() + " PASSES");
   }
   
   public void testBasicMarshallingThis() throws Throwable
   {
      System.out.println("\nentering " + getName());
      JavaSerializationMarshallerFactory factory = new JavaSerializationMarshallerFactory();
      Object o = this;
      doAllTests(factory, new MarshallingConfiguration(), o);
      System.out.println(getName() + " PASSES");
   }
   
   public void testBasicMarshallingPersistentFields() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory factory =  new JavaSerializationMarshallerFactory();
      TestPersistentFields o = new TestPersistentFields(7, "eleven");
      MarshallingConfiguration config = new MarshallingConfiguration();
      assertTrue(o.ok(doTestMarshallerToObjectInputStream(factory, config, o)));
      assertTrue(o.ok(doTestObjectOutputStreamToUnmarshaller(factory, config, o)));
      assertTrue(o.ok(doTestMarshallerToUnmarshaller(factory, config, o)));
      System.out.println(getName() + " PASSES");
   }
   
   public void testBasicMarshallingNonSerializableParent() throws Throwable
   {
      System.out.println("\nentering " + getName());
      JavaSerializationMarshallerFactory factory = new JavaSerializationMarshallerFactory();
      TestSerializableWithNonSerializableSuper o = new TestSerializableWithNonSerializableSuper(13, "seventeen");
      MarshallingConfiguration config = new MarshallingConfiguration();
      assertTrue(o.ok(doTestMarshallerToObjectInputStream(factory, config, o)));
      assertTrue(o.ok(doTestObjectOutputStreamToUnmarshaller(factory, config, o)));
      assertTrue(o.ok(doTestMarshallerToUnmarshaller(factory, config, o)));
      System.out.println(getName() + " PASSES");
   }
   
   /*
    * Verify that repeated use of ObjectTable works correctly.
    * In particular, the use of the ObjectTableWriterWrapper cache works correctly.
    */
   public void testObjectTableWithRepeatedWrites() throws Throwable
   {
      System.out.println("\nentering " + getName());
      MarshallerFactory marshallerFactory = new JavaSerializationMarshallerFactory();
      MarshallingConfiguration config = new MarshallingConfiguration();
      TestObjectTable objectTable = new TestObjectTable();
      config.setObjectTable(objectTable);
      Object o = objectTable.getTco1();
      doMarshallingTestWithRepeatedWrites(marshallerFactory, config, o);
      objectTable.ok(2, 1);
      System.out.println(getName() + " PASSES");
   }
   
   protected void doAllTests(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {
      doTestMarshallerToObjectInputStream(factory, config, o);
      doTestObjectOutputStreamToUnmarshaller(factory, config, o);
      doTestMarshallerToUnmarshaller(factory, config, o);
   }
   
   protected Object doTestMarshallerToObjectInputStream(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {
      Marshaller marshaller = factory.createMarshaller(config);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObject(o);
      
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object o2 = ois.readObject();
      assertEquals(o, o2);
      return o2;
   }
   
   protected Object doTestObjectOutputStreamToUnmarshaller(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {  
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(o);
      
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ByteInput byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      Object o2 = unmarshaller.readObject();
      assertEquals(o, o2); 
      return o2;
   }
   
   protected Object doTestMarshallerToUnmarshaller(MarshallerFactory factory, MarshallingConfiguration config, Object o) throws Throwable
   {
      Marshaller marshaller = factory.createMarshaller(config);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      marshaller.writeObject(o);
      
      Unmarshaller unmarshaller = factory.createUnmarshaller(config);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ByteInput byteInput = Marshalling.createByteInput(bais);
      unmarshaller.start(byteInput);
      Object o2 = unmarshaller.readObject();
      assertEquals(o, o2);
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
   
   class TestComplexObject implements Serializable {
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
      private boolean withCreator;
      
      public TestExternalizer(boolean withCreator) {
         TestExternalizerFactory.externalizers.add(this);
         this.withCreator = withCreator;
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
         
         readExternal(obj, input);
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
                (writeCounter == 0 && readCounter == counter
                                   && (withCreator && createCounter == counter) || (!withCreator && createCounter == 0));
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
               throw new IOException(this + " only works for " + TestExternalizableInt.class + 
                     " or " + TestExternalizableString.class);
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

      private TestComplexObject tco1 = new TestComplexObject(false, 3, (float) 7.0, 11.0, "seventeen", null);
      private TestComplexObject tco2 = new TestComplexObject(true, 23, (float) 29.0, 31.0, "thirty-seven", null);
      
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
      
      public boolean ok(int writeCounter, int readCounter) {
         return this.writeCounter == writeCounter && this.readCounter == readCounter;
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
   
   static class TestNonSerializableSuper {
      private int i;
      
      public TestNonSerializableSuper(int i) {
         this.i = i;
      }
      public TestNonSerializableSuper() {
      }
      public boolean ok(Object o) {
         if (o == null || !(o instanceof TestNonSerializableSuper)) {
            return false;
         }
         return ((TestNonSerializableSuper) o).i == 0;
      }
   }
   
   static class TestSerializableWithNonSerializableSuper extends TestNonSerializableSuper implements Serializable {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
      private String s;
      
      public TestSerializableWithNonSerializableSuper(int i, String s) {
         super(i);
         this.s = s;
      }
      public TestSerializableWithNonSerializableSuper() {
      }
      public boolean equals(Object o) {
         if (o == null || !(o instanceof TestSerializableWithNonSerializableSuper)) {
            return false;
         }
         return s.equals(((TestSerializableWithNonSerializableSuper)o).s);
      }
      public boolean ok(Object o) {
         return super.ok(o) && equals(o);
      }
   }
}

