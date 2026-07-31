package com.aba.abms.sales;

import java.util.List;

public record SalesDetailResponse(
    String billingMonth,
    String billingDestinationCompanyId,
    String billingDestinationCompanyName,
    List<SalesDetailItem> items
) {
}
