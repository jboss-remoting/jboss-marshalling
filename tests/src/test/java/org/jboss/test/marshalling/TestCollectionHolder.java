/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.EnumSet;
import java.util.EnumMap;
import java.lang.annotation.RetentionPolicy;
import java.io.Serializable;

@SuppressWarnings({ "unchecked" })
public final class TestCollectionHolder implements Serializable {
    private ArrayList arrayList = new ArrayList();
    private LinkedList linkedList = new LinkedList();

    private HashSet hashSet = new HashSet();
    private LinkedHashSet linkedHashSet = new LinkedHashSet();
    private TreeSet treeSet = new TreeSet();

    private HashMap hashMap = new HashMap();
    private LinkedHashMap linkedHashMap = new LinkedHashMap();
    private IdentityHashMap identityHashMap = new IdentityHashMap();
    private Hashtable hashtable = new Hashtable();
    private TreeMap treeMap = new TreeMap();

    private EnumSet enumSet = EnumSet.noneOf(RetentionPolicy.class);
    private EnumMap enumMap = new EnumMap(RetentionPolicy.class);
    private static final long serialVersionUID = 7939173274625487478L;
}
