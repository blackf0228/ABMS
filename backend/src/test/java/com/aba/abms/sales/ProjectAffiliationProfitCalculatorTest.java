package com.aba.abms.sales;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectAffiliationProfitCalculatorTest {
  @Test
  void allocatesActualProfitByOwnerAndProposer() {
    var detail = new ProfitContributionDetail("2025-10", "B001", "技術者", "2", 1,
        "0001", "0010", new BigDecimal("500000"), new BigDecimal("390000"),
        new BigDecimal("10000"), new BigDecimal("100000"));
    var result = ProjectAffiliationProfitCalculator.calculate(List.of(detail));
    assertThat(result).hasSize(5);
    var owner = result.stream().filter(row -> row.employeeNo().equals("0001")).findFirst().orElseThrow();
    var proposer = result.stream().filter(row -> row.employeeNo().equals("0010")).findFirst().orElseThrow();
    assertThat(owner.profitAmount()).isEqualByComparingTo("80000");
    assertThat(owner.ownerContribution()).isEqualByComparingTo("80000");
    assertThat(owner.percentage()).isEqualByComparingTo("80");
    assertThat(proposer.profitAmount()).isEqualByComparingTo("20000");
    assertThat(proposer.proposerContribution()).isEqualByComparingTo("20000");
    assertThat(proposer.percentage()).isEqualByComparingTo("20");
  }
}
