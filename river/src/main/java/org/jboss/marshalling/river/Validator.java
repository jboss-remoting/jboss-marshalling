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

package org.jboss.marshalling.river;

import java.io.ObjectInputValidation;

public final class Validator implements Comparable<Validator> {
    private final int priority;
    private final int seq;
    private final ObjectInputValidation validation;

    public Validator(final int priority, final int seq, final ObjectInputValidation validation) {
        this.priority = priority;
        this.seq = seq;
        this.validation = validation;
    }

    public int compareTo(final Validator o) {
        return priority == o.priority ? seq - o.seq : o.priority - priority;
    }

    public ObjectInputValidation getValidation() {
        return validation;
    }
}
