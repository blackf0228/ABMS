package com.aba.abms.contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class InvoiceDocumentController {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");
  private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy年MM月");
  private static final DateTimeFormatter ATTENDANCE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
  private static final DateTimeFormatter FILE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy\u5e74MM\u6708");
  private static final int DETAIL_START_ROW = 16;
  private static final int TOTAL_ROW = 18;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public InvoiceDocumentController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostMapping("/{contractNo}/invoice-document")
  public InvoiceDocumentResponse create(
      @PathVariable Integer contractNo,
      @RequestBody DocumentCreationRequest request) throws IOException {
    String customerName = jdbcTemplate.queryForObject(
        "select customer_name from contracts where contract_no = :contractNo",
        Map.of("contractNo", contractNo),
        String.class);
    if (customerName == null || customerName.isBlank()) {
      throw new IllegalStateException("取引先名を取得できません。");
    }

    YearMonth currentMonth = request.toYearMonth();
    List<InvoiceItem> items = findInvoiceItems(customerName, currentMonth);
    List<NonTaxableSalesItem> nonTaxableItems = findNonTaxableSalesItems(customerName, currentMonth);
    if (items.isEmpty() && nonTaxableItems.isEmpty()) {
      throw new IllegalStateException("請求書の対象となる契約がありません。");
    }
    return generateInvoice(customerName, items, nonTaxableItems, currentMonth);
  }

  @PostMapping("/invoice-documents/bulk")
  public InvoiceDocumentBulkResponse createBulk(@RequestBody DocumentCreationRequest request) throws IOException {
    YearMonth targetMonth = request.toYearMonth();
    List<String> customerNames = findBulkCustomerNames(targetMonth);
    List<String> fileNames = new ArrayList<>();
    for (String customerName : customerNames) {
      List<InvoiceItem> items = findInvoiceItems(customerName, targetMonth);
      List<NonTaxableSalesItem> nonTaxableItems = findNonTaxableSalesItems(customerName, targetMonth);
      if (!items.isEmpty() || !nonTaxableItems.isEmpty()) {
        fileNames.add(generateInvoice(customerName, items, nonTaxableItems, targetMonth).fileName());
      }
    }
    return new InvoiceDocumentBulkResponse(
        fileNames.size(),
        fileNames,
        resolveProjectRoot().resolve("output").resolve("請求書").toString());
  }

  private List<String> findBulkCustomerNames(YearMonth targetMonth) {
    return jdbcTemplate.queryForList("""
        select customer_name
        from (
          select distinct c.customer_name
          from contracts c
          join customer_masters customer
            on customer.company_name = c.customer_name
          where customer.company_id <> '0000'
            and c.contract_start_date <= :monthEnd
            and (c.contract_end_date is null or c.contract_end_date >= :monthStart)
          union
          select distinct customer.company_name as customer_name
          from sales_imports s
          join customer_masters customer
            on customer.company_id = s.billing_destination_company_id
          where s.billing_source_company_id = '0000'
            and (
              s.item_type in ('TRANSPORT', 'NON_TAX_ADJ')
              or trim(coalesce(s.engineer_name, '')) = ''
            )
            and s.billing_month between :monthStart and :monthEnd
        ) customers
        order by customer_name
        """,
        Map.of(
            "monthStart", targetMonth.atDay(1),
            "monthEnd", targetMonth.atEndOfMonth()),
        String.class);
  }

  private List<InvoiceItem> findInvoiceItems(String customerName, YearMonth currentMonth) {
    return jdbcTemplate.query(
        """
        select
          c.contract_no,
          coalesce(nullif(trim(concat_ws(' ', engineer.last_name, engineer.first_name)), ''), engineer.employee_name, c.engineer_name) as engineer_name,
          c.contract_amount,
          c.engineer_unit_price,
          c.settlement_lower_hour,
          c.settlement_upper_hour,
          a.working_hours
        from contracts c
        left join employee_masters_for_period(:monthStart, :monthEnd) engineer
          on engineer.employee_no = c.engineer_name
        left join attendance_hours a
          on a.contract_no = c.contract_no
         and a.engineer_name = c.engineer_name
         and a.working_month = :workingMonth
        where c.customer_name = :customerName
          and c.contract_start_date <= :monthEnd
          and (c.contract_end_date is null or c.contract_end_date >= :monthStart)
        order by c.engineer_name, c.contract_no
        """,
        Map.of(
            "customerName", customerName,
            "workingMonth", currentMonth.format(ATTENDANCE_MONTH_FORMAT),
            "monthStart", currentMonth.atDay(1),
            "monthEnd", currentMonth.atEndOfMonth()),
        new InvoiceItemMapper());
  }

  private List<NonTaxableSalesItem> findNonTaxableSalesItems(String customerName, YearMonth currentMonth) {
    return jdbcTemplate.query(
        """
        select
          coalesce(nullif(trim(concat_ws(' ', engineer.last_name, engineer.first_name)), ''), engineer.employee_name, s.engineer_name) as engineer_name,
          s.billing_amount,
          case
            when s.item_type in ('TRANSPORT', 'NON_TAX_ADJ') then s.item_type
            when trim(coalesce(s.engineer_name, '')) = '' then 'NON_TAX_BLANK'
            else s.item_type
          end as item_type
        from sales_imports s
        join customer_masters customer
          on customer.company_id = s.billing_destination_company_id
        left join employee_masters_for_period(:monthStart, :monthEnd) engineer
          on engineer.employee_no = s.engineer_name
        where s.billing_source_company_id = '0000'
          and (
            s.item_type in ('TRANSPORT', 'NON_TAX_ADJ')
            or trim(coalesce(s.engineer_name, '')) = ''
          )
          and customer.company_name = :customerName
          and s.billing_month between :monthStart and :monthEnd
        order by s.no
        """,
        Map.of(
            "customerName", customerName,
            "monthStart", currentMonth.atDay(1),
            "monthEnd", currentMonth.atEndOfMonth()),
        (rs, rowNum) -> new NonTaxableSalesItem(
            rs.getString("engineer_name"),
            rs.getBigDecimal("billing_amount"),
            rs.getString("item_type")));
  }

  private InvoiceDocumentResponse generateInvoice(
      String customerName,
      List<InvoiceItem> items,
      List<NonTaxableSalesItem> nonTaxableItems,
      YearMonth currentMonth) throws IOException {
    Path projectRoot = resolveProjectRoot();
    Path outputDirectory = projectRoot.resolve("output").resolve("請求書");
    Files.createDirectories(outputDirectory);
    String fileName = sanitizeFileName(
        "請求書_" + compact(customerName) + "_" + currentMonth.format(FILE_MONTH_FORMAT) + ".xlsx");
    Path outputPath = outputDirectory.resolve(fileName);
    Path templatePath = projectRoot.getParent()
        .resolve("要件")
        .resolve("請求書_取引先名_請求年月.xlsx");

    List<InvoiceDetail> details = buildInvoiceDetails(customerName, items, nonTaxableItems, currentMonth);
    BigDecimal subtotal = details.stream()
        .map(InvoiceDetail::amount)
        .filter(value -> value != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal nonTaxableSubtotal = details.stream()
        .filter(item -> !item.taxable())
        .map(InvoiceDetail::amount)
        .filter(value -> value != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal tax = InvoiceDocumentPolicy.calculateConsumptionTax(subtotal, nonTaxableSubtotal);
    BigDecimal total = subtotal.add(tax);

    try (InputStream input = Files.newInputStream(templatePath);
        XSSFWorkbook workbook = new XSSFWorkbook(input);
        OutputStream output = Files.newOutputStream(outputPath)) {
      Sheet sheet = workbook.getSheetAt(0);
      sheet.shiftRows(TOTAL_ROW, sheet.getLastRowNum(), details.size(), true, false);
      fillHeader(sheet, customerName, currentMonth, total);
      fillDetailRows(sheet, details, currentMonth);
      fillSpacerRows(sheet, details.size());
      fillTotals(sheet, details.size(), subtotal, tax, total);
      workbook.write(output);
    }
    return new InvoiceDocumentResponse(fileName, outputPath.toString(), details.size());
  }

  private static void fillHeader(Sheet sheet, String customerName, YearMonth currentMonth, BigDecimal total) {
    replaceText(sheet, "【発行年月日】", currentMonth.atEndOfMonth().plusDays(1).format(DATE_FORMAT));
    replaceText(sheet, "【取引先名】", customerName);
    InvoiceDocumentPolicy.fillPaymentDueDate(sheet, customerName, currentMonth);
    replaceNumber(sheet, "【合計結果】", total);
  }

  private static void fillDetailRows(Sheet sheet, List<InvoiceDetail> details, YearMonth currentMonth) {
    Row templateRow = sheet.getRow(DETAIL_START_ROW);
    for (int index = 0; index < details.size(); index++) {
      Row row = sheet.getRow(DETAIL_START_ROW + index);
      if (row == null) {
        row = sheet.createRow(DETAIL_START_ROW + index);
      }
      if (index > 0) {
        copyRowStyle(templateRow, row);
      }
      InvoiceDetail item = details.get(index);
      setCellValue(row, 0, index + 1);
      if (item.adjustment()) {
        setAdjustmentProductName(row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), item.productName());
      } else {
        setCellValue(row, 1, item.productName());
      }
      setCellValue(row, 2, currentMonth.format(MONTH_FORMAT));
      setCellValue(row, 3, item.quantity());
      setNumericCurrencyCell(row, 4, item.unitPrice());
      setNumericCurrencyCell(row, 5, item.amount());
      setCellValue(row, 6, item.remarks());
    }
  }

  private static List<InvoiceDetail> buildInvoiceDetails(
      String customerName,
      List<InvoiceItem> items,
      List<NonTaxableSalesItem> nonTaxableItems,
      YearMonth currentMonth) {
    int workingDays = businessDays(currentMonth);
    List<InvoiceDetail> details = new java.util.ArrayList<>();
    for (InvoiceItem item : items) {
      String engineerName = InvoiceDocumentPolicy.displayEngineerName(item.engineerName());
      details.add(new InvoiceDetail(
          "\u30b7\u30b9\u30c6\u30e0\u958b\u767a\u652f\u63f4",
          BigDecimal.ONE,
          item.contractAmount(),
          item.contractAmount(),
          engineerName,
          false,
          true));

      Integer lowerHour = resolvedHour(item.settlementLowerHour(), 8 * workingDays - 4);
      Integer upperHour = resolvedHour(item.settlementUpperHour(), 8 * workingDays + 20);
      BigDecimal underUnitPrice = InvoiceDocumentPolicy.settlementUnitPrice(
          customerName, item.contractAmount(), lowerHour);
      BigDecimal overUnitPrice = item.settlementUpperHour() != null
          && item.settlementUpperHour() == 999
              ? InvoiceDocumentPolicy.settlementUnitPrice(customerName, item.contractAmount(), 160)
              : InvoiceDocumentPolicy.settlementUnitPrice(
                  customerName, item.contractAmount(), upperHour);
      BigDecimal workingHours = item.workingHours();

      if (workingHours != null && workingHours.compareTo(BigDecimal.ZERO) != 0
          && upperHour != null && overUnitPrice != null
          && workingHours.compareTo(BigDecimal.valueOf(upperHour)) > 0) {
        BigDecimal quantity = workingHours.subtract(BigDecimal.valueOf(upperHour));
        details.add(new InvoiceDetail(
            InvoiceDocumentPolicy.settlementProductName(true),
            quantity,
            overUnitPrice,
            overUnitPrice.multiply(quantity),
            engineerName,
            true,
            true));
      }
      if (workingHours != null && workingHours.compareTo(BigDecimal.ZERO) != 0
          && lowerHour != null && underUnitPrice != null
          && workingHours.compareTo(BigDecimal.valueOf(lowerHour)) < 0) {
        BigDecimal quantity = BigDecimal.valueOf(lowerHour).subtract(workingHours);
        details.add(new InvoiceDetail(
            InvoiceDocumentPolicy.settlementProductName(false),
            quantity,
            underUnitPrice,
            underUnitPrice.multiply(quantity).negate(),
            engineerName,
            true,
            true));
      }
    }
    for (NonTaxableSalesItem item : nonTaxableItems) {
      String productName = switch (item.itemType()) {
        case "TRANSPORT" -> "交通費";
        case "NON_TAX_BLANK" -> "売上（技術者未設定・非課税）";
        default -> "売上調整（非課税）";
      };
      details.add(new InvoiceDetail(
          productName,
          BigDecimal.ONE,
          item.amount(),
          item.amount(),
          item.engineerName(),
          false,
          !InvoiceDocumentPolicy.isNonTaxableSale(item.itemType(), item.engineerName())));
    }
    return details;
  }

  private static Integer resolvedHour(Integer configuredHour, int calculatedHour) {
    if (configuredHour == null || configuredHour == 0) {
      return null;
    }
    return configuredHour == 999 ? calculatedHour : configuredHour;
  }

  private static int businessDays(YearMonth month) {
    Set<LocalDate> holidays = japaneseHolidays(month.getYear());
    int count = 0;
    for (int day = 1; day <= month.lengthOfMonth(); day++) {
      LocalDate date = month.atDay(day);
      if (date.getDayOfWeek() != DayOfWeek.SATURDAY
          && date.getDayOfWeek() != DayOfWeek.SUNDAY
          && !holidays.contains(date)) {
        count++;
      }
    }
    return count;
  }

  private static Set<LocalDate> japaneseHolidays(int year) {
    Set<LocalDate> holidays = new HashSet<>();
    addHoliday(holidays, year, 1, 1);
    addHoliday(holidays, year, 2, 11);
    addHoliday(holidays, year, 2, 23);
    addHoliday(holidays, year, 4, 29);
    addHoliday(holidays, year, 5, 3);
    addHoliday(holidays, year, 5, 4);
    addHoliday(holidays, year, 5, 5);
    addHoliday(holidays, year, 8, 11);
    addHoliday(holidays, year, 11, 3);
    addHoliday(holidays, year, 11, 23);
    if (year >= 2000) {
      holidays.add(nthMonday(year, 1, 2));
      holidays.add(nthMonday(year, 10, 2));
    }
    if (year >= 2003) {
      holidays.add(nthMonday(year, 7, 3));
      holidays.add(nthMonday(year, 9, 3));
    }
    if (year >= 2016) {
      holidays.add(LocalDate.of(year, 8, 11));
    }
    holidays.add(LocalDate.of(year, 3, vernalEquinoxDay(year)));
    holidays.add(LocalDate.of(year, 9, autumnalEquinoxDay(year)));
    Set<LocalDate> substituteHolidays = new HashSet<>();
    for (LocalDate holiday : holidays) {
      if (holiday.getDayOfWeek() == DayOfWeek.SUNDAY) {
        LocalDate substitute = holiday.plusDays(1);
        while (holidays.contains(substitute) || substituteHolidays.contains(substitute)) {
          substitute = substitute.plusDays(1);
        }
        substituteHolidays.add(substitute);
      }
    }
    holidays.addAll(substituteHolidays);
    for (LocalDate date = LocalDate.of(year, 1, 2);
        date.isBefore(LocalDate.of(year + 1, 1, 1));
        date = date.plusDays(1)) {
      if (!holidays.contains(date)
          && holidays.contains(date.minusDays(1))
          && holidays.contains(date.plusDays(1))) {
        holidays.add(date);
      }
    }
    return holidays;
  }

  private static void addHoliday(Set<LocalDate> holidays, int year, int month, int day) {
    holidays.add(LocalDate.of(year, month, day));
  }

  private static LocalDate nthMonday(int year, int month, int occurrence) {
    LocalDate first = LocalDate.of(year, month, 1);
    int offset = (DayOfWeek.MONDAY.getValue() - first.getDayOfWeek().getValue() + 7) % 7;
    return first.plusDays(offset + (long) (occurrence - 1) * 7);
  }

  private static int vernalEquinoxDay(int year) {
    return (int) (20.8431 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0));
  }

  private static int autumnalEquinoxDay(int year) {
    return (int) (23.2488 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0));
  }

  private static void setRowFontColor(Row row, IndexedColors color) {
    for (Cell cell : row) {
      var workbook = row.getSheet().getWorkbook();
      var style = workbook.createCellStyle();
      style.cloneStyleFrom(cell.getCellStyle());
      Font source = workbook.getFontAt(cell.getCellStyle().getFontIndexAsInt());
      Font font = workbook.createFont();
      font.setFontName(source.getFontName());
      font.setFontHeight(source.getFontHeight());
      font.setBold(source.getBold());
      font.setItalic(source.getItalic());
      font.setColor(color.getIndex());
      style.setFont(font);
      cell.setCellStyle(style);
    }
  }

  private static void fillDetailRowsLegacy(Sheet sheet, List<InvoiceItem> items, YearMonth currentMonth) {
    Row templateRow = sheet.getRow(DETAIL_START_ROW);
    for (int index = 0; index < items.size(); index++) {
      Row row = sheet.getRow(DETAIL_START_ROW + index);
      if (row == null) {
        row = sheet.createRow(DETAIL_START_ROW + index);
      }
      if (index > 0) {
        copyRowStyle(templateRow, row);
      }
      InvoiceItem item = items.get(index);
      setCellValue(row, 0, index + 1);
      setCellValue(row, 1, "システム開発支援");
      setCellValue(row, 2, currentMonth.format(MONTH_FORMAT));
      setCellValue(row, 3, 1);
      setCellValue(row, 4, item.contractAmount());
      setCellValue(row, 5, item.contractAmount());
      setCellValue(row, 6, item.engineerName());
    }
  }

  private static void fillTotals(Sheet sheet, int detailCount, BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
    int subtotalRow = TOTAL_ROW + detailCount;
    setNumericCurrencyCell(sheet.getRow(subtotalRow), 5, subtotal);
    setNumericCurrencyCell(sheet.getRow(subtotalRow + 1), 5, tax);
    setNumericCurrencyCell(sheet.getRow(subtotalRow + 2), 5, total);
    setNumericCurrencyCell(sheet.getRow(subtotalRow + 6), 1, subtotal);
    setNumericCurrencyCell(sheet.getRow(subtotalRow + 6), 2, tax);
  }

  private static void fillSpacerRows(Sheet sheet, int detailCount) {
    Row templateRow = sheet.getRow(DETAIL_START_ROW);
    for (int offset = 0; offset < 2; offset++) {
      int rowIndex = DETAIL_START_ROW + detailCount + offset;
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        row = sheet.createRow(rowIndex);
      }
      copyRowStyle(templateRow, row);
      clearRow(row);
    }
  }

  private static void clearRow(Row row) {
    for (int column = 0; column <= 6; column++) {
      row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setBlank();
    }
  }

  private static void replaceText(Sheet sheet, String placeholder, String replacement) {
    for (Row row : sheet) {
      for (Cell cell : row) {
        if (cell.getCellType() == CellType.STRING) {
          cell.setCellValue(cell.getStringCellValue().replace(placeholder, replacement));
        }
      }
    }
  }

  private static void replaceNumber(Sheet sheet, String placeholder, BigDecimal replacement) {
    for (Row row : sheet) {
      for (Cell cell : row) {
        if (cell.getCellType() == CellType.STRING
            && cell.getStringCellValue().contains(placeholder)) {
          setNumericCurrencyCell(cell, replacement);
        }
      }
    }
  }

  private static void copyRowStyle(Row source, Row target) {
    target.setHeight(source.getHeight());
    for (int column = 0; column <= 6; column++) {
      Cell sourceCell = source.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      Cell targetCell = target.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
      if (sourceCell != null) {
        targetCell.setCellStyle(sourceCell.getCellStyle());
      }
    }
  }

  private static void setCellValue(Row row, int column, Object value) {
    Cell cell = row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else {
      cell.setCellValue(value == null ? "" : value.toString());
    }
  }

  private static void setNumericCurrencyCell(Row row, int column, BigDecimal value) {
    setNumericCurrencyCell(
        row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), value);
  }

  private static void setNumericCurrencyCell(Cell cell, BigDecimal value) {
    if (value == null) {
      cell.setBlank();
      return;
    }
    cell.setCellValue(value.doubleValue());
    var style = cell.getSheet().getWorkbook().createCellStyle();
    style.cloneStyleFrom(cell.getCellStyle());
    style.setDataFormat(cell.getSheet().getWorkbook().createDataFormat().getFormat("￥#,##0"));
    String yen = String.valueOf((char) 0x00A5);
    String currencyFormat = "\"" + yen + "\"#,##0;\"" + yen + "\"-#,##0";
    style.setDataFormat(cell.getSheet().getWorkbook().createDataFormat().getFormat(currencyFormat));
    cell.setCellStyle(style);
  }

  private static void setAdjustmentProductName(Cell cell, String productName) {
    String marker = productName.contains("\u8d85\u904e\u7cbe\u7b97")
        ? "\u8d85\u904e\u7cbe\u7b97"
        : "\u4e0d\u8db3\u63a7\u9664";
    int markerStart = productName.indexOf(marker);
    XSSFRichTextString richText = new XSSFRichTextString(productName);
    XSSFFont redFont = ((XSSFWorkbook) cell.getSheet().getWorkbook()).createFont();
    redFont.setColor(IndexedColors.RED.getIndex());
    richText.applyFont(markerStart, markerStart + marker.length(), redFont);
    cell.setCellValue(richText);
  }

  private static Path resolveProjectRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    if ("backend".equalsIgnoreCase(current.getFileName().toString())) {
      return current.getParent();
    }
    return current;
  }

  private static String compact(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "");
  }

  private static String sanitizeFileName(String value) {
    return value.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private record InvoiceItem(
      Integer contractNo,
      String engineerName,
      BigDecimal contractAmount,
      BigDecimal engineerUnitPrice,
      Integer settlementLowerHour,
      Integer settlementUpperHour,
      BigDecimal workingHours) {
  }

  private record InvoiceDetail(
      String productName,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal amount,
      String remarks,
      boolean adjustment,
      boolean taxable) {
  }

  private record NonTaxableSalesItem(String engineerName, BigDecimal amount, String itemType) {
  }

  private static class InvoiceItemMapper implements RowMapper<InvoiceItem> {
    @Override
    public InvoiceItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new InvoiceItem(
          rs.getInt("contract_no"),
          rs.getString("engineer_name"),
          rs.getBigDecimal("contract_amount"),
          rs.getBigDecimal("engineer_unit_price"),
          (Integer) rs.getObject("settlement_lower_hour"),
          (Integer) rs.getObject("settlement_upper_hour"),
          rs.getBigDecimal("working_hours"));
    }
  }
}
