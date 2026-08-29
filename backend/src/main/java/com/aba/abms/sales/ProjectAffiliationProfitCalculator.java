package com.aba.abms.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProjectAffiliationProfitCalculator {

  private static final Set<String> EXCLUDED_NAMES = Set.of(
      "王超", "小林和成", "毛杰", "李鴻民", "孫偉東");

  private ProjectAffiliationProfitCalculator() {
  }

  static List<ProjectAffiliationProfitRow> calculate(
      YearMonth fiscalStart,
      YearMonth fiscalEnd,
      List<ContractProfitSource> contracts) {
    Map<String, ProfitAccumulator> grouped = new HashMap<>();
    for (ContractProfitSource contract : contracts) {
      if (contract == null || EXCLUDED_NAMES.contains(normalizeName(contract.contractorName()))) {
        continue;
      }
      YearMonth activeStart = later(fiscalStart, YearMonth.from(contract.contractStartDate()));
      YearMonth contractEnd = contract.contractEndDate() == null
          ? fiscalEnd
          : YearMonth.from(contract.contractEndDate());
      YearMonth activeEnd = earlier(fiscalEnd, contractEnd);
      if (activeStart.isAfter(activeEnd)) {
        continue;
      }
      long activeMonths = ChronoUnit.MONTHS.between(activeStart, activeEnd) + 1;
      BigDecimal monthlyProfit = amount(contract.contractAmount()).subtract(amount(contract.engineerUnitPrice()));
      BigDecimal contractProfit = monthlyProfit.multiply(BigDecimal.valueOf(activeMonths));
      String key = contract.employeeNo() + "\u0000" + contract.employeeName();
      grouped.computeIfAbsent(
          key,
          ignored -> new ProfitAccumulator(contract.employeeNo(), contract.employeeName()))
          .add(contractProfit);
    }

    BigDecimal totalProfit = grouped.values().stream()
        .map(ProfitAccumulator::profitAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<ProjectAffiliationProfitRow> rows = new ArrayList<>();
    for (ProfitAccumulator accumulator : grouped.values()) {
      BigDecimal percentage = totalProfit.compareTo(BigDecimal.ZERO) == 0
          ? BigDecimal.ZERO.setScale(2)
          : accumulator.profitAmount()
              .multiply(BigDecimal.valueOf(100))
              .divide(totalProfit, 2, RoundingMode.HALF_UP);
      rows.add(new ProjectAffiliationProfitRow(
          accumulator.employeeNo(),
          accumulator.employeeName(),
          accumulator.profitAmount(),
          percentage));
    }
    rows.sort((left, right) -> right.profitAmount().compareTo(left.profitAmount()));
    return rows;
  }

  private static String normalizeName(String value) {
    return value == null ? "" : value.replace(" ", "").replace("　", "").trim();
  }

  private static BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static YearMonth later(YearMonth left, YearMonth right) {
    return left.isAfter(right) ? left : right;
  }

  private static YearMonth earlier(YearMonth left, YearMonth right) {
    return left.isBefore(right) ? left : right;
  }

  record ContractProfitSource(
      String employeeNo,
      String employeeName,
      String contractorName,
      BigDecimal contractAmount,
      BigDecimal engineerUnitPrice,
      LocalDate contractStartDate,
      LocalDate contractEndDate) {
  }

  private static final class ProfitAccumulator {
    private final String employeeNo;
    private final String employeeName;
    private BigDecimal profitAmount = BigDecimal.ZERO;

    private ProfitAccumulator(String employeeNo, String employeeName) {
      this.employeeNo = employeeNo;
      this.employeeName = employeeName;
    }

    private void add(BigDecimal amount) {
      profitAmount = profitAmount.add(amount);
    }

    private String employeeNo() {
      return employeeNo;
    }

    private String employeeName() {
      return employeeName;
    }

    private BigDecimal profitAmount() {
      return profitAmount;
    }
  }
}
