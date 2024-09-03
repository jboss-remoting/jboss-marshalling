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
 * An adapter that allows to use an UnmarshallingObjectInputFilter in place of an ObjectInputFilter.
 *
 * @author Brian Stansberry
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ObjectInputFilterAdapter implements ObjectInputFilter {

    private final UnmarshallingObjectInputFilter adaptee;

    ObjectInputFilterAdapter(UnmarshallingObjectInputFilter adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
        UnmarshallingObjectInputFilter.Status status = adaptee.checkInput(new UnmarshallingFilterInfoAdapter(filterInfo));

        switch (status) {
            case ALLOWED:
                return ObjectInputFilter.Status.ALLOWED;
            case REJECTED:
                return ObjectInputFilter.Status.REJECTED;
            case UNDECIDED:
                return ObjectInputFilter.Status.UNDECIDED;
        }
        throw new IllegalStateException("Unexpected filtering decision: " + status);
    }

}
