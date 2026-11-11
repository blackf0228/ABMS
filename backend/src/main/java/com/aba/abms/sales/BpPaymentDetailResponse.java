package com.aba.abms.sales;
import java.util.List;
public record BpPaymentDetailResponse(String billingMonth, String billingSourceCompanyId,
    String billingSourceCompanyName, List<SalesDetailItem> items) {}
