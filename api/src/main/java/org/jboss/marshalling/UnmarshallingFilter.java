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

@FunctionalInterface
public interface UnmarshallingFilter {

    FilterResponse checkInput(FilterInput input);

    enum FilterResponse {
        ACCEPT,
        REJECT,
        UNDECIDED;
    }

    interface FilterInput {

        Class<?> getUnmarshalledClass();

        long getArrayLength();

        long getDepth();

        long getReferences();

        long getStreamBytes();
    }

    final class Factory {

        private Factory() {
        }

        public static UnmarshallingFilter createFilter(String filterSpec) {
            return createFilter(filterSpec, false);
        }

        public static UnmarshallingFilter createFilter(String filterSpec, boolean checkJEPS290ProcessFilter) {
            UnmarshallingFilter result = new SimpleUnmarshallingFilter(filterSpec);
            if (checkJEPS290ProcessFilter) {
                UnmarshallingFilter processWide = createJEPS290DefaultFilter(false, true);
                if (processWide != null) {
                    result = new ChainedUnmarshallingFilter(processWide, result);
                }
            }
            return result;
        }

        public static UnmarshallingFilter createJEPS290DefaultFilter(boolean asWhitelist) {
            return createJEPS290DefaultFilter(asWhitelist, false);
        }

        private static UnmarshallingFilter createJEPS290DefaultFilter(boolean forWhitelist, boolean nullok) {
            UnmarshallingFilter result = JDKSpecific.getJEPS290ProcessWideFilter();
            if (result != null) {
                if (forWhitelist) {
                    // If the spec filter doesn't ACCEPT, then REJECT
                    result = new ChainedUnmarshallingFilter(result, REJECTING);
                }
            } else if (!nullok) {
                result = UNDECIDED;
            }
            return result;
        }

        public static boolean isJEPS290ProcessWideFilteringConfigured() {
            return JDKSpecific.getJEPS290ProcessWideFilter() != null;
        }
    }

    UnmarshallingFilter ACCEPTING = input -> FilterResponse.ACCEPT;
    UnmarshallingFilter UNDECIDED = input -> FilterResponse.UNDECIDED;
    UnmarshallingFilter REJECTING = input -> FilterResponse.REJECT;
}
