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
 * An adapter that allows to use an ObjectInputFilter.FilterInfo in place of an UnmarshallingObjectInputFilter.FilterInfo.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class UnmarshallingFilterInfoAdapter implements UnmarshallingObjectInputFilter.FilterInfo {

    private final ObjectInputFilter.FilterInfo adaptee;

    UnmarshallingFilterInfoAdapter(final ObjectInputFilter.FilterInfo adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public Class<?> getUnmarshalledClass() {
        return adaptee.serialClass();
    }

    @Override
    public long getArrayLength() {
        return adaptee.arrayLength();
    }

    @Override
    public long getDepth() {
        return adaptee.depth();
    }

    @Override
    public long getReferences() {
        return adaptee.references();
    }

    @Override
    public long getStreamBytes() {
        return adaptee.streamBytes();
    }

}
