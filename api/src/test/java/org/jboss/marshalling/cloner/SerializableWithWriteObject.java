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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

/**
 * @author <a href="mailto:bmaxwell@redhat.com">Brad Maxwell</a>
 */
public class SerializableWithWriteObject implements Serializable {

    private String[] data;

    public String[] getData() {
        return data;
    }

    private void writeObject(ObjectOutputStream objectoutputstream) throws IOException { 
    }

    public boolean equals(Object other) {
        return other instanceof SerializableWithWriteObject && equals((SerializableWithWriteObject)other);
    }

    public boolean equals(SerializableWithWriteObject other) {
        return Arrays.equals(data, other.getData()); 
    }
}

