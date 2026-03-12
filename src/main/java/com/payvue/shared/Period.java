package com.payvue.shared;

import java.time.Instant;

public final class Period {

  private final Instant from;
  private final Instant to;

  private Period(Instant from, Instant to) {
    this.from = from;
    this.to = to;
  }

  public static Period of(Instant from, Instant to) {
    return new Period(from, to);
  }

  public static Period unbounded() {
    return new Period(null, null);
  }

  public Instant from() { return from; }
  public Instant to() { return to; }

  public boolean isUnbounded() {
    return from == null && to == null;
  }
}
