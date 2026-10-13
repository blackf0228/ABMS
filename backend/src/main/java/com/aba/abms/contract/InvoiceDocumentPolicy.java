package com.aba.abms.contract;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

final class InvoiceDocumentPolicy {

  private static final String ALPHA_FORCE = "アルファフォース株式会社";
  private static final String HIGH_THINK_RESEARCH = "株式会社ハイシンク創研";
  private static final String SOFT_THINK = "ソフトシンク株式会社";

  private InvoiceDocumentPolicy() {
  }

  static LocalDate paymentDueDate(String customerName, YearMonth billingMonth) {
    YearMonth dueMonth = billingMonth.plusMonths(2);
    String normalizedCustomerName = customerName == null ? "" : customerName.trim();
    return switch (normalizedCustomerName) {
      case ALPHA_FORCE -> dueMonth.atDay(10);
      case HIGH_THINK_RESEARCH -> dueMonth.atDay(7);
      case SOFT_THINK -> dueMonth.atDay(15);
      default -> dueMonth.atEndOfMonth();
    };
  }

  static BigDecimal settlementUnitPrice(
      String customerName, BigDecimal contractAmount, Integer hour) {
    if (contractAmount == null || hour == null || hour <= 0) {
      return null;
    }
    int roundingUnit = ALPHA_FORCE.equals(customerName == null ? "" : customerName.trim()) ? 100 : 10;
    return contractAmount
        .divide(BigDecimal.valueOf(hour), 10, RoundingMode.DOWN)
        .divide(BigDecimal.valueOf(roundingUnit), 0, RoundingMode.DOWN)
        .multiply(BigDecimal.valueOf(roundingUnit));
  }

  static BigDecimal calculateConsumptionTax(BigDecimal subtotal, BigDecimal nonTaxableTotal) {
    return subtotal.subtract(nonTaxableTotal)
        .multiply(new BigDecimal("0.10"))
        .setScale(0, RoundingMode.HALF_UP);
  }

  static boolean isNonTaxableSale(String itemType, String engineerName) {
    return "TRANSPORT".equals(itemType)
        || "NON_TAX_ADJ".equals(itemType)
        || engineerName == null
        || engineerName.isBlank();
  }

  static String displayEngineerName(String engineerName) {
    if (engineerName == null) {
      return null;
    }
    String normalizedName = engineerName.trim().replaceAll("[\\s　]+", "");
    return switch (normalizedName) {
      case "彭闖" -> "小林 和成";
      case "小林和成" -> "彭 闖";
      default -> engineerName;
    };
  }

  static String settlementProductName(boolean overtime) {
    return overtime ? "システム開発支援（超過精算）" : "システム開発支援（不足控除）";
  }

  static void fillPaymentDueDate(Sheet sheet, String customerName, YearMonth billingMonth) {
    String dueDate = paymentDueDate(customerName, billingMonth)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"));
    for (Row row : sheet) {
      for (Cell cell : row) {
        if (cell.getCellType() != CellType.STRING
            || !cell.getStringCellValue().contains("【支払期日】")) {
          continue;
        }
        cell.setCellValue(cell.getStringCellValue().replace("【支払期日】", dueDate));
        CellStyle rightAlignedStyle = sheet.getWorkbook().createCellStyle();
        rightAlignedStyle.cloneStyleFrom(cell.getCellStyle());
        rightAlignedStyle.setAlignment(HorizontalAlignment.RIGHT);
        cell.setCellStyle(rightAlignedStyle);
      }
    }
  }

  static boolean overlapsBillingMonth(
      LocalDate contractStartDate,
      LocalDate contractEndDate,
      YearMonth billingMonth) {
    if (contractStartDate == null) {
      return false;
    }
    return !contractStartDate.isAfter(billingMonth.atEndOfMonth())
        && (contractEndDate == null || !contractEndDate.isBefore(billingMonth.atDay(1)));
  }
}
