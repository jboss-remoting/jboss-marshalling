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
import java.util.Set;


@SuppressWarnings({ "NonFinalFieldReferencedInHashCode", "NonFinalFieldReferenceInEquals" })
public class TestComplexObject implements Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;
   
   private boolean b;
   private byte b8;
   private char c;
   private short s;
   private int i;
   private long l;
   private float f;
   private double d;
   private String str;
   private Set<?> set;
   
   public TestComplexObject(boolean b, byte b8, char c, short s, int i, long l, float f, double d, String str, Set<?> set) {
      this.b = b;
      this.b8 = b8;
      this.c = c;
      this.s = s;
      this.i = i;
      this.l = l;
      this.f = f;
      this.d = d;
      this.str = str;
      this.set = set;
   }
   
   public TestComplexObject() {}
   
   public int hashCode() {
      return Double.toString((b ? 1 : 2) * b8 * c * s * i * l * f * d * str.hashCode() * ((set == null) ? 1 : set.hashCode())).hashCode();
   }
   
   public boolean equals(Object o) {
      if (o == null || !(o instanceof TestComplexObject)) {
         return false;
      }
      TestComplexObject c = (TestComplexObject) o;
      return c.b == b && c.b8 == b8 && c.c == this.c && c.s == s && c.i == i && c.l == l 
             && Float.compare(c.f, f) == 0 && Double.compare(c.d, d) == 0 && str.equals(c.str)
             && ((set == null) ? c.set == null : set.equals(c.set));
   }
}
