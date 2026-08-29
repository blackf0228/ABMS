package com.aba.abms.sales;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectAffiliationProfitCalculatorTest {

  @Test
  void aggregatesByAffiliationAndExcludesSpecifiedContractorNames() {
    List<ProjectAffiliationProfitCalculator.ContractProfitSource> contracts = List.of(
        contract("0012", "山田 太郎", "田中 一郎", "500000", "400000", "2025-10-01", "2025-12-31"),
        contract("0013", "佐藤 花子", "王 超", "900000", "100000", "2025-10-01", "2025-12-31"),
        contract("0001", "王 超", "鈴木 次郎", "600000", "500000", "2025-11-01", "2025-11-30"));

    List<ProjectAffiliationProfitRow> result = ProjectAffiliationProfitCalculator.calculate(
        YearMonth.of(2025, 10),
        YearMonth.of(2025, 12),
        contracts);

    assertThat(result).containsExactly(
        new ProjectAffiliationProfitRow("0012", "山田 太郎", new BigDecimal("300000"), new BigDecimal("75.00")),
        new ProjectAffiliationProfitRow("0001", "王 超", new BigDecimal("100000"), new BigDecimal("25.00")));
  }

  private static ProjectAffiliationProfitCalculator.ContractProfitSource contract(
      String employeeNo,
      String employeeName,
      String contractorName,
      String contractAmount,
      String engineerUnitPrice,
      String startDate,
      String endDate) {
    return new ProjectAffiliationProfitCalculator.ContractProfitSource(
        employeeNo,
        employeeName,
        contractorName,
        new BigDecimal(contractAmount),
        new BigDecimal(engineerUnitPrice),
        LocalDate.parse(startDate),
        LocalDate.parse(endDate));
  }
}
