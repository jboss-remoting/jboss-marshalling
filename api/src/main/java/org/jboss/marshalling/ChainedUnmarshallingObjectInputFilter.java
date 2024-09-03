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

/**
 * {@link UnmarshallingObjectInputFilter} implementation that is chaining multiple {@link UnmarshallingObjectInputFilter}s.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ChainedUnmarshallingObjectInputFilter implements UnmarshallingObjectInputFilter {

    private final UnmarshallingObjectInputFilter[] chain;

    ChainedUnmarshallingObjectInputFilter(final UnmarshallingObjectInputFilter... chain) {
        this.chain = chain;
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
        for (UnmarshallingObjectInputFilter filter : chain) {
            Status status = filter.checkInput(filterInfo);
            if (status != Status.UNDECIDED) {
                return status;
            }
        }
        return Status.UNDECIDED;
    }

}
