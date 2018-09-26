/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
 * Filter that implementations of {@link Unmarshaller} can apply after resolving the class to decide if deserialization has to stop.
 */
@FunctionalInterface
public interface InputFilter {

  Status checkInput(FilterInfo info);

  static InputFilter create(String pattern) {

  }

  enum Status {
    UNDECIDED,
    ALLOWED,
    REJECTED
  }

  interface FilterInfo {

    Class<?> serialClass();

    long arrayLength();

    long depth();

    long references();

    long streamBytes();

  }

}
