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

final class UnmarshallingFilterAdapter implements UnmarshallingFilter {

    private final ObjectInputFilter delegate;

    UnmarshallingFilterAdapter(ObjectInputFilter delegate) {
        this.delegate = delegate;
    }

    @Override
    public FilterResponse checkInput(final FilterInput input) {
        ObjectInputFilter.Status status = delegate.checkInput(new ObjectInputFilter.FilterInfo() {
            @Override
            public Class<?> serialClass() {
                return input.getUnmarshalledClass();
            }

            @Override
            public long arrayLength() {
                return input.getArrayLength();
            }

            @Override
            public long depth() {
                return input.getDepth();
            }

            @Override
            public long references() {
                return input.getReferences();
            }

            @Override
            public long streamBytes() {
                return input.getStreamBytes();
            }
        });

        switch (status) {
            case ALLOWED:
                return FilterResponse.ACCEPT;
            case REJECTED:
                return FilterResponse.REJECT;
            case UNDECIDED:
                return FilterResponse.UNDECIDED;
        }
        throw new IllegalStateException("Unexpected filtering decision: " + status);
    }

}
