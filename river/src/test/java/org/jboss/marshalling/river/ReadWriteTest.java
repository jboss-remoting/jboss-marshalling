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

package org.jboss.marshalling.river;

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import org.jboss.marshalling.MarshallingConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 *
 */
public abstract class ReadWriteTest {
    public void run() throws Throwable {
        final MarshallerFactory factory = new RiverMarshallerFactory();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setCreator(new SunReflectiveCreator());
        configure(configuration);
        final Marshaller marshaller = factory.createMarshaller(configuration);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
        marshaller.start(Marshalling.createByteOutput(baos));
        runWrite(marshaller);
        marshaller.finish();
        final byte[] bytes = baos.toByteArray();
        final Unmarshaller unmarshaller = factory.createUnmarshaller(configuration);
        unmarshaller.start(Marshalling.createByteInput(new ByteArrayInputStream(bytes)));
        runRead(unmarshaller);
        unmarshaller.finish();
    }

    public void configure(MarshallingConfiguration configuration) throws Throwable {}

    public void runWrite(Marshaller marshaller) throws Throwable {};

    public void runRead(Unmarshaller unmarshaller) throws Throwable {};
}
