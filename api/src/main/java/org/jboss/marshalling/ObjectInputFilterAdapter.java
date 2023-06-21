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

final class ObjectInputFilterAdapter implements ObjectInputFilter {

    private final UnmarshallingFilter unmarshallingFilter;

    ObjectInputFilterAdapter(UnmarshallingFilter unmarshallingFilter) {
        this.unmarshallingFilter = unmarshallingFilter;
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
        UnmarshallingFilter.FilterResponse response = unmarshallingFilter.checkInput(new UnmarshallingFilter.FilterInput() {
            @Override
            public Class<?> getUnmarshalledClass() {
                return filterInfo.serialClass();
            }

            @Override
            public long getArrayLength() {
                return filterInfo.arrayLength();
            }

            @Override
            public long getDepth() {
                return filterInfo.depth();
            }

            @Override
            public long getReferences() {
                return filterInfo.references();
            }

            @Override
            public long getStreamBytes() {
                return filterInfo.streamBytes();
            }
        });
        return toObjectInputFilterStatus(response);
    }

    private Status toObjectInputFilterStatus(UnmarshallingFilter.FilterResponse response) {
        Status status = null;
        switch (response) {
            case ACCEPT:
                status = Status.ALLOWED;
                break;
            case REJECT:
                status = Status.REJECTED;
                break;
            case UNDECIDED:
                status = Status.UNDECIDED;
                break;
        }
        return status;
    }

}
