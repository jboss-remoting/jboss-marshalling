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
            UnmarshallingFilter result = MarshallingConfiguration.getJEPS290ProcessWideFilter();
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
            return MarshallingConfiguration.getJEPS290ProcessWideFilter() != null;
        }
    }

    UnmarshallingFilter ACCEPTING = input -> FilterResponse.ACCEPT;
    UnmarshallingFilter UNDECIDED = input -> FilterResponse.UNDECIDED;
    UnmarshallingFilter REJECTING = input -> FilterResponse.REJECT;
}
