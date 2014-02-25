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

package org.jboss.marshalling.cloner;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Jaikiran Pai
 */
public class ClassLoaderClassClonerTestCase {

    /**
     * Tests that cloning of primitive types via {@link ClassLoaderClassCloner#clone(Class)} doesn't run into
     * issues
     *
     * @throws Exception
     */
    @Test
    public void testPrimitiveCloning() throws Exception {
        final ClassLoaderClassCloner cloner = new ClassLoaderClassCloner(this.getClass().getClassLoader());

        Assert.assertEquals(cloner.clone(Double.TYPE), Double.TYPE, "Unexpected cloned type for " + Double.TYPE);
        Assert.assertEquals(cloner.clone(Boolean.TYPE), Boolean.TYPE, "Unexpected cloned type for " + Boolean.TYPE);
        Assert.assertEquals(cloner.clone(Character.TYPE), Character.TYPE, "Unexpected cloned type for " + Character.TYPE);
        Assert.assertEquals(cloner.clone(Void.TYPE), Void.TYPE, "Unexpected cloned type for " + Void.TYPE);
        Assert.assertEquals(cloner.clone(Short.TYPE), Short.TYPE, "Unexpected cloned type for " + Short.TYPE);
        Assert.assertEquals(cloner.clone(Long.TYPE), Long.TYPE, "Unexpected cloned type for " + Long.TYPE);
        Assert.assertEquals(cloner.clone(Float.TYPE), Float.TYPE, "Unexpected cloned type for " + Float.TYPE);
        Assert.assertEquals(cloner.clone(Integer.TYPE), Integer.TYPE, "Unexpected cloned type for " + Integer.TYPE);
        Assert.assertEquals(cloner.clone(Byte.TYPE), Byte.TYPE, "Unexpected cloned type for " + Byte.TYPE);

    }
}
