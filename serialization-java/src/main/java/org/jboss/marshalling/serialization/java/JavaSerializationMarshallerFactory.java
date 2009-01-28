package org.jboss.marshalling.serialization.java;

import java.io.IOException;

import org.jboss.marshalling.AbstractMarshallerFactory;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ExternalizerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.ClassExternalizerFactory;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Sep 19, 2008
 * </p>
 */
public final class JavaSerializationMarshallerFactory extends AbstractMarshallerFactory {

   public JavaSerializationMarshallerFactory() {
   }

   public Marshaller createMarshaller(MarshallingConfiguration configuration) throws IOException {
      return new JavaSerializationMarshaller(this, configuration);
   }

   public Unmarshaller createUnmarshaller(MarshallingConfiguration configuration) throws IOException {
      return new JavaSerializationUnmarshaller(this, configuration);
   }
   
   protected ExternalizerFactory getDefaultExternalizerFactory() {
      return null;
   }

   protected ClassExternalizerFactory getDefaultClassExternalizerFactory() {
      return null;
   }

  protected StreamHeader getDefaultStreamHeader() {
      return null;
  }

  protected ObjectResolver getDefaultObjectResolver() {
      return null;
  }

  protected ClassResolver getDefaultClassResolver() {
      return null;
  }

  protected ClassTable getDefaultClassTable() {
      return null;
  }

  protected ObjectTable getDefaultObjectTable() {
      return null;
  }
}
