package com.aba.abms.contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.text.NumberFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
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
public class OrderDocumentController {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");
  private static final DateTimeFormatter ORDER_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
  private static final DateTimeFormatter FILE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy\u5e74MM\u6708");

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public OrderDocumentController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostMapping("/{contractNo}/order-document")
  public OrderDocumentResponse create(
      @PathVariable Integer contractNo,
      @RequestBody DocumentCreationRequest request) throws IOException {
    return createOrderDocument(contractNo, request.toYearMonth());
  }

  @PostMapping("/order-documents/bulk")
  public OrderDocumentBulkResponse createBulk(@RequestBody DocumentCreationRequest request) throws IOException {
    YearMonth currentMonth = request.toYearMonth();
    YearMonth targetMonth = currentMonth;
    List<Integer> contractNos = jdbcTemplate.query(
        """
        select c.contract_no
        from contracts c
        join customer_masters engineer_company
          on trim(engineer_company.company_name) = trim(c.engineer_company_name)
        where trim(engineer_company.company_id) <> '0000'
          and trim(engineer_company.company_id) <> ''
          and c.operation_status = '契約中'
          and c.contract_start_date <= :targetMonthEnd
          and (c.contract_end_date is null or c.contract_end_date >= :targetMonthStart)
        order by c.engineer_name, c.contract_no
        """,
        Map.of(
            "targetMonthStart", targetMonth.atDay(1),
            "targetMonthEnd", targetMonth.atEndOfMonth()),
        (rs, rowNum) -> rs.getInt("contract_no"));

    List<String> fileNames = new ArrayList<>();
    String outputDirectory = resolveProjectRoot().resolve("output").resolve("注文書").toString();
    for (Integer contractNo : contractNos) {
      fileNames.add(createOrderDocument(contractNo, currentMonth).fileName());
    }
    return new OrderDocumentBulkResponse(fileNames.size(), fileNames, outputDirectory);
  }

  private OrderDocumentResponse createOrderDocument(Integer contractNo, YearMonth currentMonth) throws IOException {
    OrderDocumentData data = jdbcTemplate.queryForObject(
        """
        select
          coalesce(nullif(trim(concat_ws(' ', engineer.last_name, engineer.first_name)), ''), engineer.employee_name, c.engineer_name) as engineer_name,
          c.contract_amount,
          c.engineer_unit_price,
          c.engineer_company_name,
          c.settlement_lower_hour,
          c.settlement_upper_hour,
          c.customer_name,
          customer.company_id as customer_id,
          engineer_company.company_id as engineer_company_id
        from contracts c
        left join employee_masters_for_period(:monthStart, :monthEnd) engineer
          on engineer.employee_no = c.engineer_name
        left join customer_masters customer
          on trim(customer.company_name) = trim(c.customer_name)
        left join customer_masters engineer_company
          on trim(engineer_company.company_name) = trim(c.engineer_company_name)
        where c.contract_no = :contractNo
        """,
        Map.of("contractNo", contractNo, "monthStart", currentMonth.atDay(1), "monthEnd", currentMonth.atEndOfMonth()),
        new OrderDocumentDataMapper());

    if (data.engineerCompanyId() == null || data.engineerCompanyId().isBlank()) {
      throw new IllegalStateException("技術者所属会社番号を取得できません。取引先一覧に会社名を登録してください。");
    }

    Path outputDirectory = resolveProjectRoot().resolve("output").resolve("注文書");
    outputDirectory = orderOutputDirectory();
    Files.createDirectories(outputDirectory);

    String fileName = sanitizeFileName(
        "注文書_" + compact(data.engineerCompanyName()) + "_"
            + currentMonth.format(FILE_MONTH_FORMAT) + "_"
            + compact(data.engineerName()) + "様.xlsx");
    Path outputPath = outputDirectory.resolve(fileName);

    SettlementValues settlement = settlementValues(data, currentMonth);
    Map<String, String> replacements = replacements(data, currentMonth, settlement);
    Path templatePath = resolveProjectRoot().getParent()
        .resolve("要件")
        .resolve("注文書_技術者所属会社名_注文年月_技術者名様.xlsx");
    templatePath = orderTemplatePath();
    if (!Files.exists(templatePath)) {
      throw new IllegalStateException("注文書テンプレートが見つかりません: " + templatePath);
    }
    try (InputStream input = Files.newInputStream(templatePath);
        XSSFWorkbook workbook = new XSSFWorkbook(input);
        OutputStream output = Files.newOutputStream(outputPath)) {
      replacePlaceholders(workbook, replacements, settlement.workingDaysNotice());
      workbook.write(output);
    }

    return new OrderDocumentResponse(fileName, outputPath.toString());
  }

  private static Map<String, String> replacements(
      OrderDocumentData data,
      YearMonth currentMonth,
      SettlementValues settlement) {
    YearMonth nextMonth = currentMonth;
    Map<String, String> values = new HashMap<>();
    values.put("【発行年月日】", OrderDocumentPolicy.issueDate(currentMonth).format(DATE_FORMAT));
    values.put("【技術者所属会社名】", value(data.engineerCompanyName()));
    values.put("【注文番号】", "CMC" + nextMonth.format(ORDER_MONTH_FORMAT) + "-T" + data.engineerCompanyId() + "001");
    values.put("【作業開始日】", nextMonth.atDay(1).format(DATE_FORMAT));
    values.put("【作業終了日】", nextMonth.atEndOfMonth().format(DATE_FORMAT));
    values.put("【技術者氏名】", value(data.engineerName()));
    values.put("【契約単価】", amount(data.engineerUnitPrice()));
    values.put("【納期日】", currentMonth.plusMonths(3).atEndOfMonth().format(DATE_FORMAT));
    values.put("【精算下限時間】", settlement.lowerHour());
    values.put("【精算上限時間】", settlement.upperHour());
    values.put("【超過単価】", settlement.overUnitPrice());
    values.put("【不足単価】", settlement.underUnitPrice());
    String deliveryDate = currentMonth.plusMonths(3).atEndOfMonth().format(DATE_FORMAT);
    String targetDeliveryDate = currentMonth.plusMonths(2).atEndOfMonth().format(DATE_FORMAT);
    values.replaceAll((key, value) -> value.equals(deliveryDate) ? targetDeliveryDate : value);
    return values;
  }

  private static void replacePlaceholders(
      XSSFWorkbook workbook,
      Map<String, String> replacements,
      String workingDaysNotice) {
    for (Sheet sheet : workbook) {
      for (Row row : sheet) {
        for (Cell cell : row) {
          if (cell.getCellType() != CellType.STRING) {
            continue;
          }
          String value = cell.getStringCellValue();
          boolean orderNumberCell = value.contains("【注文番号】");
          boolean settlementConditionCell = value.contains("【精算下限時間】");
          for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            value = value.replace(replacement.getKey(), replacement.getValue());
          }
          if (settlementConditionCell && "固定".equals(workingDaysNotice)) {
            value = "固定";
          } else if (settlementConditionCell && !workingDaysNotice.isBlank()) {
            value = workingDaysNotice + value;
          }
          cell.setCellValue(value);
          if (orderNumberCell) {
            setFontName(workbook, cell, "Yu Gothic");
          }
        }
      }
    }
  }

  private static void setFontName(XSSFWorkbook workbook, Cell cell, String fontName) {
    XSSFCellStyle sourceStyle = (XSSFCellStyle) cell.getCellStyle();
    XSSFCellStyle copiedStyle = workbook.createCellStyle();
    copiedStyle.cloneStyleFrom(sourceStyle);

    XSSFFont sourceFont = workbook.getFontAt(sourceStyle.getFontIndexAsInt());
    XSSFFont copiedFont = workbook.createFont();
    copiedFont.setFontName(fontName);
    copiedFont.setFontHeight(sourceFont.getFontHeight());
    copiedFont.setBold(sourceFont.getBold());
    copiedFont.setItalic(sourceFont.getItalic());
    copiedFont.setStrikeout(sourceFont.getStrikeout());
    copiedFont.setUnderline(sourceFont.getUnderline());
    if (sourceFont.getXSSFColor() != null) {
      copiedFont.setColor(sourceFont.getXSSFColor());
    }
    copiedStyle.setFont(copiedFont);
    cell.setCellStyle(copiedStyle);
  }

  private static SettlementValues settlementValues(OrderDocumentData data, YearMonth currentMonth) {
    int workingDays = businessDays(currentMonth);
    Integer lowerHour = resolvedHour(data.settlementLowerHour(), 8 * workingDays - 4);
    Integer upperHour = resolvedHour(data.settlementUpperHour(), 8 * workingDays + 20);
    String lowerText = integer(lowerHour);
    String upperText = integer(upperHour);
    String underPrice = unitPrice(data.engineerUnitPrice(), lowerHour);
    String overPrice = unitPrice(data.engineerUnitPrice(), upperHour);
    String workingDaysNotice;
    if (isZero(data.settlementLowerHour()) && isZero(data.settlementUpperHour())) {
      workingDaysNotice = "固定";
    } else if (is999(data.settlementLowerHour()) || is999(data.settlementUpperHour())) {
      workingDaysNotice = "稼働日数：" + workingDays + "日\n";
    } else {
      workingDaysNotice = "";
    }
    return new SettlementValues(lowerText, upperText, overPrice, underPrice, workingDaysNotice);
  }

  private static boolean isZero(Integer value) {
    return value != null && value == 0;
  }

  private static boolean is999(Integer value) {
    return value != null && value == 999;
  }

  private static Integer resolvedHour(Integer configuredHour, int calculatedHour) {
    if (configuredHour == null) {
      return null;
    }
    return configuredHour == 999 ? calculatedHour : configuredHour;
  }

  private static String unitPrice(BigDecimal engineerUnitPrice, Integer hour) {
    if (engineerUnitPrice == null || hour == null || hour == 0) {
      return hour == null ? "" : "0";
    }
    BigDecimal price = engineerUnitPrice
        .divide(BigDecimal.valueOf(hour), 10, RoundingMode.DOWN)
        .divide(BigDecimal.TEN, 0, RoundingMode.DOWN)
        .multiply(BigDecimal.TEN);
    return amount(price);
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
    return first.plusDays(offset + (occurrence - 1) * 7L);
  }

  private static int vernalEquinoxDay(int year) {
    return (int) Math.floor(20.8431 + 0.242194 * (year - 1980)
        - Math.floor((year - 1980) / 4.0));
  }

  private static int autumnalEquinoxDay(int year) {
    return (int) Math.floor(23.2488 + 0.242194 * (year - 1980)
        - Math.floor((year - 1980) / 4.0));
  }

  private static Path resolveProjectRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    if ("backend".equalsIgnoreCase(current.getFileName().toString())) {
      return current.getParent();
    }
    return current;
  }

  private static Path orderOutputDirectory() {
    return resolveProjectRoot().resolve("output").resolve("\u6ce8\u6587\u66f8");
  }

  private static Path orderTemplatePath() {
    return resolveProjectRoot().getParent()
        .resolve("\u8981\u4ef6")
        .resolve("\u6ce8\u6587\u66f8_\u6280\u8853\u8005\u6240\u5c5e\u4f1a\u793e\u540d_\u6ce8\u6587\u5e74\u6708_\u6280\u8853\u8005\u540d\u69d8.xlsx");
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }

  private static String compact(String value) {
    return value(value).replaceAll("\\s+", "");
  }

  private static String amount(BigDecimal value) {
    if (value == null) {
      return "";
    }
    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.JAPAN);
    numberFormat.setGroupingUsed(true);
    numberFormat.setMaximumFractionDigits(0);
    numberFormat.setRoundingMode(RoundingMode.DOWN);
    return numberFormat.format(value);
  }

  private static String integer(Integer value) {
    return value == null ? "" : value.toString();
  }

  private static String sanitizeFileName(String value) {
    return value.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private record OrderDocumentData(
      String engineerName,
      BigDecimal contractAmount,
      BigDecimal engineerUnitPrice,
      String engineerCompanyName,
      Integer settlementLowerHour,
      Integer settlementUpperHour,
      String customerName,
      String customerId,
      String engineerCompanyId
  ) {
  }

  private record SettlementValues(
      String lowerHour,
      String upperHour,
      String overUnitPrice,
      String underUnitPrice,
      String workingDaysNotice
  ) {
  }

  private static class OrderDocumentDataMapper implements RowMapper<OrderDocumentData> {

    @Override
    public OrderDocumentData mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new OrderDocumentData(
          rs.getString("engineer_name"),
          rs.getBigDecimal("contract_amount"),
          rs.getBigDecimal("engineer_unit_price"),
          rs.getString("engineer_company_name"),
          (Integer) rs.getObject("settlement_lower_hour"),
          (Integer) rs.getObject("settlement_upper_hour"),
          rs.getString("customer_name"),
          rs.getString("customer_id"),
          rs.getString("engineer_company_id"));
    }
  }
}
