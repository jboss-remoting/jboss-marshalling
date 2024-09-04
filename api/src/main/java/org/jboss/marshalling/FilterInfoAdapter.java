/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

package org.jboss.marshalling;

import java.io.ObjectInputFilter;

/**
 * An adapter that allows to use an UnmarshallingObjectInputFilter.FilterInfo in place of an ObjectInputFilter.FilterInfo.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class FilterInfoAdapter implements ObjectInputFilter.FilterInfo {

    private final UnmarshallingObjectInputFilter.FilterInfo adaptee;

    FilterInfoAdapter(final UnmarshallingObjectInputFilter.FilterInfo adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public Class<?> serialClass() {
        return adaptee.getUnmarshalledClass();
    }

    @Override
    public long arrayLength() {
        return adaptee.getArrayLength();
    }

    @Override
    public long depth() {
        return adaptee.getDepth();
    }

    @Override
    public long references() {
        return adaptee.getReferences();
    }

    @Override
    public long streamBytes() {
        return adaptee.getStreamBytes();
    }

}
