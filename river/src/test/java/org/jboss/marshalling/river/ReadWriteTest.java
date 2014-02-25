/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.marshalling.river;

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.ReflectiveCreator;
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
        configuration.setSerializedCreator(new SunReflectiveCreator());
        configuration.setExternalizerCreator(new ReflectiveCreator());
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
