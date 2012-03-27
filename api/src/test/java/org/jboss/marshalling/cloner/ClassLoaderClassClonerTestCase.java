/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
