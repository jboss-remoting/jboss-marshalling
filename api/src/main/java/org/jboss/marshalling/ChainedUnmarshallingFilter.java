package org.jboss.marshalling;

public final class ChainedUnmarshallingFilter implements UnmarshallingFilter {

    private final UnmarshallingFilter[] chain;

    public ChainedUnmarshallingFilter(UnmarshallingFilter... chain) {
        this.chain = chain;
    }

    @Override
    public FilterResponse checkInput(FilterInput input) {
        for (UnmarshallingFilter filter : chain) {
            FilterResponse response = filter.checkInput(input);
            if (response != FilterResponse.UNDECIDED) {
                return response;
            }
        }
        return FilterResponse.UNDECIDED;
    }
}
