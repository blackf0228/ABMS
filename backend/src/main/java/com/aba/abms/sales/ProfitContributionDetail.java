package com.aba.abms.sales;
import java.math.BigDecimal;
public record ProfitContributionDetail(String month, String engineerNo, String engineerName,
    String employeeType, Integer contractNo, String projectAffiliation, String engineerProposer,
    BigDecimal salesAmount, BigDecimal paymentAmount, BigDecimal allowanceAmount, BigDecimal profitAmount,
    String payrollEmployeeNo, String relatedCompanyName) {
  public ProfitContributionDetail(String month, String engineerNo, String engineerName,
      String employeeType, Integer contractNo, String projectAffiliation, String engineerProposer,
      BigDecimal salesAmount, BigDecimal paymentAmount, BigDecimal allowanceAmount, BigDecimal profitAmount) {
    this(month, engineerNo, engineerName, employeeType, contractNo, projectAffiliation, engineerProposer,
        salesAmount, paymentAmount, allowanceAmount, profitAmount, engineerNo, null);
  }
}
