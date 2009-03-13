/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
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

package org.jboss.test.marshalling;

import java.io.IOException;
import java.util.Arrays;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.StreamHeader;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.serialization.jboss.JBossSerializationMarshallerFactory;
import org.jboss.marshalling.serialization.jboss.JBossSerializationUnmarshaller;
import org.jboss.serial.objectmetamodel.DataContainerConstants;
import org.testng.SkipException;

/**
 *
 */
public final class JBossCompatibilityMarshallerFactoryTestUnmarshallerProvider implements TestUnmarshallerProvider {
    private final MarshallerFactory marshallerFactory;
    private final int version;

    public JBossCompatibilityMarshallerFactoryTestUnmarshallerProvider(final MarshallerFactory factory) {
        marshallerFactory = factory;
        version = -1;
    }

    public JBossCompatibilityMarshallerFactoryTestUnmarshallerProvider(final MarshallerFactory factory, final int version) {
        marshallerFactory = factory;
        this.version = version;
    }

    public Unmarshaller create(final MarshallingConfiguration config, final ByteInput source) throws IOException {
       if (config.getClassExternalizerFactory() != null ||
             config.getClassResolver() != null ||
             config.getClassTable() != null ||
             config.getObjectResolver() != null ||
             config.getObjectTable() != null ||
             config.getStreamHeader() != null) {
            throw new SkipException("Don't need extra features for JBossSerialization compatibility tests");           
         }

         config.setStreamHeader(new StreamHeader() {
            public void readHeader(ByteInput input) throws IOException {
               byte signature[] = new byte[DataContainerConstants.openSign.length];
               input.read(signature);
               if (!Arrays.equals(signature,DataContainerConstants.openSign)) {
                   throw new IOException("Mismatch version of JBossSerialization signature");
               }
            }

            public void writeHeader(ByteOutput output) throws IOException {
               if (output!=null) {
                   output.write(DataContainerConstants.openSign);
               }
            }
         });
         
        if (version != -1) {
            config.setVersion(version);
        }
        final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(config);
        if (marshallerFactory instanceof JBossSerializationMarshallerFactory) {
           ((JBossSerializationUnmarshaller) unmarshaller).setJbossSerializationCompatible(true);
        }
        unmarshaller.start(source);
        return unmarshaller;
    }
}
