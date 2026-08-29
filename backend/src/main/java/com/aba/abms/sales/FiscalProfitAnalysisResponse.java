package com.aba.abms.sales;

import java.math.BigDecimal;
import java.util.List;

public record FiscalProfitAnalysisResponse(
    String fiscalStartMonth,
    String fiscalEndMonth,
    List<String> months,
    List<FiscalProfitAnalysisRow> rows,
    BigDecimal companyProfitTotal,
    List<ProjectAffiliationProfitRow> projectAffiliationProfits) {
}
