package com.payvue.core.transaction.query;

import java.util.List;

public record TransactionHistoryPage(
    List<TransactionHistoryView> items,
    long total
) {}
