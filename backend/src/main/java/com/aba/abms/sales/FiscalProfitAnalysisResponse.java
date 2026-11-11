package com.aba.abms.sales;

import java.math.BigDecimal;
import java.util.List;

public record FiscalProfitAnalysisResponse(
    String fiscalStartMonth,
    String fiscalEndMonth,
    List<String> months,
    List<ProjectAffiliationProfitRow> projectAffiliationProfits,
    List<ProfitContributionDetail> details,
    List<String> warnings,
    BigDecimal unallocatedProfit) {
}
