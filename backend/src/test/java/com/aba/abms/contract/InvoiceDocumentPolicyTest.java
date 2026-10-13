package com.aba.abms.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class InvoiceDocumentPolicyTest {

  @Test
  void usesCustomerSpecificPaymentDueDates() {
    YearMonth billingMonth = YearMonth.of(2026, 8);

    assertThat(InvoiceDocumentPolicy.paymentDueDate("アルファフォース株式会社", billingMonth))
        .isEqualTo(LocalDate.of(2026, 10, 10));
    assertThat(InvoiceDocumentPolicy.paymentDueDate("株式会社ハイシンク創研", billingMonth))
        .isEqualTo(LocalDate.of(2026, 10, 7));
    assertThat(InvoiceDocumentPolicy.paymentDueDate("ソフトシンク株式会社", billingMonth))
        .isEqualTo(LocalDate.of(2026, 10, 15));
    assertThat(InvoiceDocumentPolicy.paymentDueDate("その他株式会社", billingMonth))
        .isEqualTo(LocalDate.of(2026, 10, 31));
  }

  @Test
  void includesContractsThatOverlapTheBillingMonthRegardlessOfCurrentStatus() {
    YearMonth billingMonth = YearMonth.of(2026, 8);

    assertThat(InvoiceDocumentPolicy.overlapsBillingMonth(
        LocalDate.of(2025, 10, 1), LocalDate.of(2026, 8, 15), billingMonth)).isTrue();
    assertThat(InvoiceDocumentPolicy.overlapsBillingMonth(
        LocalDate.of(2025, 10, 1), LocalDate.of(2026, 7, 31), billingMonth)).isFalse();
    assertThat(InvoiceDocumentPolicy.overlapsBillingMonth(
        LocalDate.of(2026, 9, 1), null, billingMonth)).isFalse();
  }

  @Test
  void roundsSettlementUnitPriceDownToHundredsOnlyForAlphaForce() {
    BigDecimal contractAmount = new BigDecimal("730000");

    assertThat(InvoiceDocumentPolicy.settlementUnitPrice(
        "アルファフォース株式会社", contractAmount, 140))
        .isEqualByComparingTo("5200");
    assertThat(InvoiceDocumentPolicy.settlementUnitPrice(
        "その他株式会社", contractAmount, 140))
        .isEqualByComparingTo("5210");
  }

  @Test
  void excludesNonTaxableNegativeAdjustmentsFromConsumptionTax() {
    BigDecimal subtotal = new BigDecimal("9475");
    BigDecimal nonTaxableTotal = new BigDecimal("-525");

    assertThat(InvoiceDocumentPolicy.calculateConsumptionTax(subtotal, nonTaxableTotal))
        .isEqualByComparingTo("1000");
    assertThat(InvoiceDocumentPolicy.calculateConsumptionTax(
        new BigDecimal("-825"), new BigDecimal("-825")))
        .isEqualByComparingTo("0");
  }

  @Test
  void excludesSalesWithBlankEngineerNamesFromConsumptionTax() {
    assertThat(InvoiceDocumentPolicy.isNonTaxableSale("NORMAL", "  ")).isTrue();
    assertThat(InvoiceDocumentPolicy.isNonTaxableSale("NON_TAX_ADJ", "免税")).isTrue();
    assertThat(InvoiceDocumentPolicy.isNonTaxableSale("TRANSPORT", "王 超")).isTrue();
    assertThat(InvoiceDocumentPolicy.isNonTaxableSale("NORMAL", "王 超")).isFalse();
  }

  @Test
  void displaysPengChuangAsXiaolinKazunariInInvoiceRemarks() {
    assertThat(InvoiceDocumentPolicy.displayEngineerName("彭 闖"))
        .isEqualTo("小林 和成");
    assertThat(InvoiceDocumentPolicy.displayEngineerName("彭　闖"))
        .isEqualTo("小林 和成");
    assertThat(InvoiceDocumentPolicy.displayEngineerName("小林　和成"))
        .isEqualTo("彭 闖");
    assertThat(InvoiceDocumentPolicy.displayEngineerName("小林 和成"))
        .isEqualTo("彭 闖");
    assertThat(InvoiceDocumentPolicy.displayEngineerName("王 超"))
        .isEqualTo("王 超");
  }

  @Test
  void usesRequestedProductNamesForOverAndUnderSettlementRows() {
    assertThat(InvoiceDocumentPolicy.settlementProductName(true))
        .isEqualTo("システム開発支援（超過精算）");
    assertThat(InvoiceDocumentPolicy.settlementProductName(false))
        .isEqualTo("システム開発支援（不足控除）");
  }

  @Test
  void replacesPaymentDueDateAndRightAlignsOnlyThatCell() throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("請求書");
      Cell dueDateCell = sheet.createRow(0).createCell(0);
      dueDateCell.setCellValue("お支払期日：【支払期日】");
      var sourceStyle = workbook.createCellStyle();
      sourceStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      dueDateCell.setCellStyle(sourceStyle);

      InvoiceDocumentPolicy.fillPaymentDueDate(
          sheet, "その他株式会社", YearMonth.of(2026, 8));

      assertThat(dueDateCell.getStringCellValue())
          .isEqualTo("お支払期日：2026年10月31日");
      assertThat(dueDateCell.getCellStyle().getAlignment())
          .isEqualTo(HorizontalAlignment.RIGHT);
      assertThat(dueDateCell.getCellStyle().getVerticalAlignment())
          .isEqualTo(VerticalAlignment.CENTER);
    }
  }
}
