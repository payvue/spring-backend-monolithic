package com.payvue.app.domain.summary;

import java.util.List;

public interface SummaryView {
  Totals totals();
  Segment personal();
  Segment shared();
  Trend trend();
  String currency();
  List<? extends Category> categories();

  interface Totals {
    long cash();
    long value();
    long assetValue();
  }

  interface Segment {
    long cash();
    long value();
    long assetValue();
  }

  interface Trend {
    double monthlyChangeRate();
    double changeRate();
    long monthlyChange();
  }

  interface Category {
    String category();
    long amount();
    int ratio();
  }
}
