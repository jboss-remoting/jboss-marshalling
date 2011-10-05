/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling.cloner;

import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.jboss.marshalling.Pair;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test
public final class SerializingClonerTestCase {

    public void testImmutables() throws Throwable {
        final ObjectCloner objectCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(new ClonerConfiguration());
        final Object[] objects = {
                TimeUnit.NANOSECONDS,
                "Bananananana",
                Boolean.TRUE,
                Integer.valueOf(12),
                new String("Something else"),
                new Integer(1234),
                Enum.class,
                Object.class,
                new Object[0],
                RetentionPolicy.RUNTIME,
        };
        // should not clone an immutable JDK class
        for (Object orig : objects) {
            final Object clone = objectCloner.clone(orig);
            assertSame(clone, orig);
        }
    }

    public void testEquals() throws Throwable {
        final ObjectCloner objectCloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(new ClonerConfiguration());
        final Object[] objects = {
                Pair.create("First", "Second"),
                Arrays.asList("One", Integer.valueOf(2), Boolean.TRUE, "Shoe")
        };
        for (Object orig : objects) {
            final Object clone = objectCloner.clone(orig);
            assertEquals(clone, orig);
        }
    }
}
