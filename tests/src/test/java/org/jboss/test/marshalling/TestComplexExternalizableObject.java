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
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.Set;

@SuppressWarnings({ "NonFinalFieldReferencedInHashCode", "NonFinalFieldReferenceInEquals" })
public class TestComplexExternalizableObject implements Externalizable
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

   public TestComplexExternalizableObject(boolean b, byte b8, char c, short s, int i, long l, float f, double d, String str, Set<?> set) {
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

   public TestComplexExternalizableObject() {}

   public int hashCode() {
      return Double.toString((b ? 1 : 2) * b8 * c * s * i * l * f * d * str.hashCode() * ((set == null) ? 1 : set.hashCode())).hashCode();
   }

   public boolean equals(Object o) {
      if (o == null || !(o instanceof TestComplexExternalizableObject)) {
         return false;
      }
      TestComplexExternalizableObject c = (TestComplexExternalizableObject) o;
      return c.b == b && c.b8 == b8 && c.c == this.c && c.s == s && c.i == i && c.l == l
             && Float.compare(c.f, f) == 0 && Double.compare(c.d, d) == 0 && str.equals(c.str)
             && ((set == null) ? c.set == null : set.equals(c.set));
   }

    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeBoolean(b);
        out.writeByte(b8);
        out.writeChar(c);
        out.writeShort(s);
        out.writeInt(i);
        out.writeLong(l);
        out.writeFloat(f);
        out.writeDouble(d);
        out.writeUTF(str);
        out.writeObject(set);
    }

    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        b = in.readBoolean();
        b8 = in.readByte();
        c = in.readChar();
        s = in.readShort();
        i = in.readInt();
        l = in.readLong();
        f = in.readFloat();
        d = in.readDouble();
        str = in.readUTF();
        set = (Set<?>) in.readObject();
    }
}