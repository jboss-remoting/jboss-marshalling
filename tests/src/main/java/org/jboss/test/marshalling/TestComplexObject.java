/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
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

import java.io.Serializable;
import java.util.Set;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Oct 8, 2008
 * </p>
 */
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
