package com.aba.abms.sales;

import java.util.List;
import java.util.Map;

public record AllowanceAnalysisResponse(
    String fiscalStartMonth,
    String fiscalEndMonth,
    List<String> months,
    List<AllowanceAnalysisItem> rows,
    Map<String, Map<String, java.math.BigDecimal>> recruitmentAmounts
) {
}
