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
