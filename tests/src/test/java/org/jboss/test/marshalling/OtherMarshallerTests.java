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

package org.jboss.test.marshalling;

import java.io.Serializable;
import java.util.concurrent.PriorityBlockingQueue;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectClonerFactory;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.testng.annotations.Test;


/**
 * OtherMarshallerTests
 */
public final class OtherMarshallerTests extends TestBase {

    public OtherMarshallerTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        super(testMarshallerProvider, testUnmarshallerProvider, configuration);
    }    

    @Test
    public synchronized void testPriorityBlockingQueue() throws Throwable {
        final ObjectClonerFactory clonerFactory = ObjectCloners.getSerializingObjectClonerFactory();
        final ClonerConfiguration configuration = new ClonerConfiguration();
        final ObjectCloner cloner = clonerFactory.createCloner(configuration);
        PriorityBlockingQueueTestObject testObject = new PriorityBlockingQueueTestObject<Integer>();
        if(testObject != null) {
            testObject.add(new Integer(100));
            cloner.clone(testObject);
        }
    }

    public static class PriorityBlockingQueueTestObject<T> implements Serializable {
        private PriorityBlockingQueue<T> queue = new PriorityBlockingQueue<T>();
        public void add(T item) {
            this.queue.add(item);
        }
    }
}
