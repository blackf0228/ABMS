package com.aba.abms.sales;
import java.math.BigDecimal;
public record BpPaymentSummaryItem(int no, String billingMonth, String billingSourceCompanyId,
    String billingSourceCompanyName, BigDecimal billingAmount) {}
