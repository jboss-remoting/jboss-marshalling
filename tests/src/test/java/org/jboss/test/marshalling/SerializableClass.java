/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.test.marshalling;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author wangc
 *
 */
public class SerializableClass implements Serializable {

    private static final long serialVersionUID = -4653225564318177939L;

    private String str;
    private List<NonSerializableClass> list = new ArrayList<>();

    public SerializableClass(String str) {
        this.str = str;
        list = Arrays.asList(new NonSerializableClass(str));
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof SerializableClass)) {
            return false;
        }
        SerializableClass c = (SerializableClass) o;
        return str.equals(c.str) && ((list == null) ? c.list == null : list.equals(c.list));
    }
}
