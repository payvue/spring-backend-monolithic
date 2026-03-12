package com.payvue.app.domain.summary;

import java.util.Collection;

public interface SummaryAggregationStrategy<S extends SummaryView> {
  S aggregate(Collection<S> summaries);
}
