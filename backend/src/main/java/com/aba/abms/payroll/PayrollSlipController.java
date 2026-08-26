package com.aba.abms.payroll;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll/slips")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class PayrollSlipController {

  private static final DateTimeFormatter PAYMENT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
  private static final NumberFormat MONEY_FORMATTER = NumberFormat.getNumberInstance(Locale.JAPAN);

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PayrollSlipController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public List<PayrollSlipListItem> search(
      @RequestParam(required = false) String paymentMonth,
      @RequestParam(required = false) String employeeNo,
      @RequestParam(required = false) String employeeName) {
    StringBuilder sql = new StringBuilder("""
        select
          e.employee_no,
          e.employee_name,
          c.payment_month,
          c.base_salary,
          c.commuting_allowance,
          c.management_allowance,
          c.transportation_adjustment,
          c.overtime_pay,
          c.advance_expense,
          c.advance_expense2,
          c.other_amount,
          d.transfer_payment_amount
        from employee_masters e
        join monthly_salary_calculation_conditions c
          on c.employee_no = e.employee_no
        left join monthly_salary_details d
          on d.employee_no = e.employee_no
         and d.payment_month = c.payment_month
        left join employee_resident_tax_details r
          on r.employee_no = e.employee_no
         and r.payment_month = c.payment_month
        where 1 = 1
        """);

    Map<String, Object> params = new HashMap<>();
    if (paymentMonth != null && !paymentMonth.isBlank()) {
      sql.append(" and c.payment_month = :paymentMonth");
      params.put("paymentMonth", paymentMonth.trim());
    }
    if (employeeNo != null && !employeeNo.isBlank()) {
      sql.append(" and e.employee_no like :employeeNo");
      params.put("employeeNo", "%" + employeeNo.trim() + "%");
    }
    if (employeeName != null && !employeeName.isBlank()) {
      sql.append(" and e.employee_name like :employeeName");
      params.put("employeeName", "%" + employeeName.trim() + "%");
    }
    sql.append(" order by c.payment_month desc, e.employee_no");

    return jdbcTemplate.query(sql.toString(), params, new PayrollSlipListItemMapper());
  }

  @GetMapping("/{employeeNo}/{paymentMonth}")
  public PayrollSlipDetail detail(
      @PathVariable String employeeNo,
      @PathVariable String paymentMonth) {
    String sql = """
        select
          e.employee_no,
          e.employee_name,
          e.employee_type,
          c.payment_month,
          c.base_salary,
          c.commuting_allowance,
          c.management_allowance,
          c.transportation_adjustment,
          c.overtime_pay,
          c.advance_expense,
          c.advance_expense2,
          c.other_amount,
          c.other_item_name,
          case when e.employee_no = '0010' then 0 else d.employment_insurance_amount end as employment_insurance_amount,
          d.pension_insurance_amount,
          d.health_insurance_amount,
          d.income_tax,
          d.child_support_amount,
          coalesce(r.payment_amount, 0) as resident_tax_amount,
          case when e.employee_no = '0010'
            then coalesce(d.taxable_income, 0) + coalesce(d.employment_insurance_amount, 0)
            else d.taxable_income
          end as taxable_income,
          case when e.employee_no = '0010'
            then coalesce(d.transfer_payment_amount, 0) + coalesce(d.employment_insurance_amount, 0)
            else d.transfer_payment_amount
          end as transfer_payment_amount
        from employee_masters e
        join monthly_salary_calculation_conditions c
          on c.employee_no = e.employee_no
        left join monthly_salary_details d
          on d.employee_no = e.employee_no
         and d.payment_month = c.payment_month
        left join employee_resident_tax_details r
          on r.employee_no = e.employee_no
         and r.payment_month = c.payment_month
        where e.employee_no = :employeeNo
          and c.payment_month = :paymentMonth
        """;

    Map<String, Object> params = Map.of(
        "employeeNo", employeeNo,
        "paymentMonth", paymentMonth);

    return jdbcTemplate.queryForObject(sql, params, new PayrollSlipDetailMapper());
  }

  @PostMapping("/{employeeNo}/{paymentMonth}/pdf")
  public PayrollPdfResponse createPdf(
      @PathVariable String employeeNo,
      @PathVariable String paymentMonth) throws IOException {
    PayrollSlipDetail detail = detail(employeeNo, paymentMonth);
    Path outputDirectory = resolveOutputDirectory();
    Files.createDirectories(outputDirectory);

    String fileName = pdfFileName(detail);
    Path outputPath = outputDirectory.resolve(fileName);
    byte[] jpeg = renderPayrollSlipJpeg(detail);
    writeImagePdf(outputPath, jpeg, 3508, 2480);
    return new PayrollPdfResponse(fileName, outputPath.toString());
  }

  private static BigDecimal amount(ResultSet rs, String name) throws SQLException {
    return rs.getBigDecimal(name);
  }

  private static Path resolveOutputDirectory() {
    Path current = Path.of("").toAbsolutePath().normalize();
    if ("backend".equalsIgnoreCase(current.getFileName().toString())) {
      return current.getParent().resolve("output");
    }
    return current.resolve("output");
  }

  private static String pdfFileName(PayrollSlipDetail detail) {
    String normalizedName = detail.employeeName().replaceAll("\\s+", "");
    return sanitizeFileName(japaneseEraMonth(detail.paymentMonth()) + "給与明細書_"
        + detail.employeeNo() + "_" + normalizedName + ".pdf");
  }

  private static String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private static byte[] renderPayrollSlipJpeg(PayrollSlipDetail detail) throws IOException {
    int logicalWidth = 1754;
    int logicalHeight = 1240;
    int scale = 2;
    int width = logicalWidth * scale;
    int height = logicalHeight * scale;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.scale(scale, scale);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, logicalWidth, logicalHeight);

    Font titleFont = new Font("Yu Gothic", Font.BOLD, 40);
    Font headerFont = new Font("Yu Gothic", Font.BOLD, 24);
    Font textFont = new Font("Yu Gothic", Font.PLAIN, 22);
    Font boldFont = new Font("Yu Gothic", Font.BOLD, 22);

    g.setColor(Color.BLACK);
    g.setFont(titleFont);
    drawCentered(g, payrollSlipTitle(detail), logicalWidth / 2, 70);

    g.setFont(textFont);
    drawText(g, "振込日", 1380, 62);
    drawText(g, payrollTransferDate(detail), 1480, 62);

    drawSummary(g, detail, textFont, boldFont);
    drawSlipTable(g, "支給", 90, 210, 480, supportRows(detail), headerFont, textFont, boldFont);
    drawSlipTable(g, "控除", 635, 210, 480, deductionRows(detail), headerFont, textFont, boldFont);
    drawSlipTable(g, "その他", 1180, 210, 430, otherRows(detail), headerFont, textFont, boldFont);
    drawSlipTable(g, "差引合計", 1180, 540, 430, transferRows(detail), headerFont, textFont, boldFont);

    g.dispose();
    return writeJpeg(image);
  }

  private static void drawSummary(Graphics2D g, PayrollSlipDetail detail, Font textFont, Font boldFont) {
    g.setFont(textFont);
    drawText(g, "氏名", 90, 150);
    g.setFont(boldFont);
    drawText(g, detail.employeeName() + " 様", 160, 150);
    g.setFont(textFont);
    drawText(g, "所属", 600, 150);
    g.setFont(boldFont);
    drawText(g, "株式会社CMCソフト", 670, 150);
    g.setFont(textFont);
    drawText(g, "従業員コード", 1120, 150);
    g.setFont(boldFont);
    drawText(g, detail.employeeNo(), 1280, 150);
  }

  private static List<Row> supportRows(PayrollSlipDetail detail) {
    List<Row> rows = new ArrayList<>();
    rows.add(new Row("基本給料", detail.baseSalary()));
    rows.add(new Row("通勤手当", detail.commutingAllowance()));
    rows.add(new Row("管理手当", detail.managementAllowance()));
    rows.add(new Row("営業手当", BigDecimal.ZERO));
    rows.add(new Row("残業代", detail.overtimePay()));
    rows.add(new Row("交通費精算", detail.transportationAdjustment()));
    if (!isZero(detail.advanceExpense())) {
      rows.add(new Row("立替経費", detail.advanceExpense()));
    }
    if (!isZero(detail.advanceExpense2())) {
      rows.add(new Row("立替経費2", detail.advanceExpense2()));
    }
    rows.add(new Row("合計", payrollSupportTotal(detail), true));
    return rows;
  }

  private static List<Row> deductionRows(PayrollSlipDetail detail) {
    List<Row> rows = new ArrayList<>();
    rows.add(new Row("雇用保険料", detail.employmentInsuranceAmount()));
    rows.add(new Row("厚生年金保険料", detail.pensionInsuranceAmount()));
    rows.add(new Row("健康保険料", detail.healthInsuranceAmount()));
    rows.add(new Row("所得税", detail.incomeTax()));
    rows.add(new Row("住民税", detail.residentTaxAmount()));
    rows.add(new Row("子ども・子育て支援金", detail.childSupportAmount()));
    rows.add(new Row("", BigDecimal.ZERO));
    rows.add(new Row("", BigDecimal.ZERO));
    rows.add(new Row("合計", payrollDeductionTotal(detail), true));
    return rows;
  }

  private static List<Row> otherRows(PayrollSlipDetail detail) {
    String label = otherItemLabel(detail);
    return List.of(
        new Row(label, label.isBlank() ? null : detail.otherAmount()),
        new Row("", null));
  }

  private static List<Row> transferRows(PayrollSlipDetail detail) {
    return List.of(
        new Row("振込支給額", detail.transferPaymentAmount()),
        new Row("差引支給合計", detail.transferPaymentAmount(), true));
  }

  private static void drawSlipTable(
      Graphics2D g,
      String title,
      int x,
      int y,
      int width,
      List<Row> rows,
      Font headerFont,
      Font textFont,
      Font boldFont) {
    int headerHeight = 48;
    int rowHeight = 46;
    int labelWidth = (int) (width * 0.58);
    int valueWidth = width - labelWidth;
    int totalHeight = headerHeight + rowHeight * rows.size();

    g.setStroke(new BasicStroke(2));
    g.setColor(Color.BLACK);
    g.drawRect(x, y, width, totalHeight);
    g.setColor(new Color(230, 240, 221));
    g.fillRect(x + 1, y + 1, width - 1, headerHeight - 1);
    g.setColor(Color.BLACK);
    g.drawRect(x, y, width, headerHeight);
    g.setFont(headerFont);
    drawCentered(g, title, x + width / 2, y + 32);

    int currentY = y + headerHeight;
    for (Row row : rows) {
      if (row.total()) {
        g.setColor(new Color(255, 250, 241));
        g.fillRect(x + 1, currentY + 1, width - 1, rowHeight - 1);
      }
      g.setColor(new Color(220, 234, 246));
      g.fillRect(x + 1, currentY + 1, labelWidth - 1, rowHeight - 1);
      g.setColor(Color.BLACK);
      g.drawRect(x, currentY, labelWidth, rowHeight);
      g.drawRect(x + labelWidth, currentY, valueWidth, rowHeight);
      g.setFont(row.total() ? boldFont : textFont);
      drawText(g, row.label(), x + 12, currentY + 31);
      drawRight(g, isBlank(row.label()) ? "" : money(row.amount()), x + width - 12, currentY + 31);
      currentY += rowHeight;
    }
  }

  private static byte[] writeJpeg(BufferedImage image) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    var writers = ImageIO.getImageWritersByFormatName("jpg");
    if (!writers.hasNext()) {
      ImageIO.write(image, "jpg", out);
      return out.toByteArray();
    }

    ImageWriter writer = writers.next();
    ImageWriteParam params = writer.getDefaultWriteParam();
    if (params.canWriteCompressed()) {
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(0.98f);
    }
    try (ImageOutputStream imageOut = ImageIO.createImageOutputStream(out)) {
      writer.setOutput(imageOut);
      writer.write(null, new IIOImage(image, null, null), params);
    } finally {
      writer.dispose();
    }
    return out.toByteArray();
  }

  private static void writeImagePdf(Path outputPath, byte[] jpeg, int imageWidth, int imageHeight) throws IOException {
    String header = "%PDF-1.4\n";
    List<byte[]> objects = new ArrayList<>();
    objects.add(ascii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));
    objects.add(ascii("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"));
    objects.add(ascii("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /XObject << /Im1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"));
    objects.add(join(
        ascii("4 0 obj\n<< /Type /XObject /Subtype /Image /Width " + imageWidth
            + " /Height " + imageHeight
            + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
            + jpeg.length + " >>\nstream\n"),
        jpeg,
        ascii("\nendstream\nendobj\n")));
    byte[] content = ascii("q\n842 0 0 595 0 0 cm\n/Im1 Do\nQ\n");
    objects.add(join(
        ascii("5 0 obj\n<< /Length " + content.length + " >>\nstream\n"),
        content,
        ascii("endstream\nendobj\n")));

    ByteArrayOutputStream pdf = new ByteArrayOutputStream();
    pdf.write(ascii(header));
    List<Integer> offsets = new ArrayList<>();
    for (byte[] object : objects) {
      offsets.add(pdf.size());
      pdf.write(object);
    }
    int xrefOffset = pdf.size();
    pdf.write(ascii("xref\n0 " + (objects.size() + 1) + "\n"));
    pdf.write(ascii("0000000000 65535 f \n"));
    for (int offset : offsets) {
      pdf.write(ascii(String.format("%010d 00000 n \n", offset)));
    }
    pdf.write(ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
        + xrefOffset + "\n%%EOF\n"));
    Files.write(outputPath, pdf.toByteArray());
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }

  private static byte[] join(byte[]... chunks) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] chunk : chunks) {
      out.write(chunk);
    }
    return out.toByteArray();
  }

  private static void drawText(Graphics2D g, String text, int x, int y) {
    g.drawString(text == null ? "" : text, x, y);
  }

  private static void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
    FontMetrics metrics = g.getFontMetrics();
    String value = text == null ? "" : text;
    g.drawString(value, centerX - metrics.stringWidth(value) / 2, baselineY);
  }

  private static void drawRight(Graphics2D g, String text, int rightX, int baselineY) {
    FontMetrics metrics = g.getFontMetrics();
    String value = text == null ? "" : text;
    g.drawString(value, rightX - metrics.stringWidth(value), baselineY);
  }

  private static String money(BigDecimal value) {
    return MONEY_FORMATTER.format(amount(value));
  }

  private static BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static boolean isZero(BigDecimal value) {
    return amount(value).compareTo(BigDecimal.ZERO) == 0;
  }

  private static BigDecimal payrollSupportTotal(PayrollSlipDetail detail) {
    return amount(detail.baseSalary())
        .add(amount(detail.commutingAllowance()))
        .add(amount(detail.managementAllowance()))
        .add(amount(detail.transportationAdjustment()))
        .add(amount(detail.overtimePay()))
        .add(amount(detail.advanceExpense()))
        .add(amount(detail.advanceExpense2()));
  }

  private static BigDecimal payrollDeductionTotal(PayrollSlipDetail detail) {
    return amount(detail.employmentInsuranceAmount())
        .add(amount(detail.pensionInsuranceAmount()))
        .add(amount(detail.healthInsuranceAmount()))
        .add(amount(detail.incomeTax()))
        .add(amount(detail.residentTaxAmount()))
        .add(amount(detail.childSupportAmount()))
        .setScale(0, java.math.RoundingMode.DOWN);
  }

  private static String otherItemLabel(PayrollSlipDetail detail) {
    return detail.otherItemName() == null || detail.otherItemName().isBlank()
        ? ""
        : detail.otherItemName().trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String payrollSlipTitle(PayrollSlipDetail detail) {
    return japaneseEraMonth(detail.paymentMonth()) + "明細書";
  }

  private static String payrollTransferDate(PayrollSlipDetail detail) {
    String compact = detail.paymentMonth().replaceAll("[^0-9]", "");
    if (compact.length() < 6) {
      return "";
    }
    return compact.substring(0, 4) + "/" + compact.substring(4, 6) + "/25";
  }

  private static String japaneseEraMonth(String paymentMonth) {
    String compact = paymentMonth.replaceAll("[^0-9]", "");
    if (compact.length() < 6) {
      return paymentMonth;
    }
    YearMonth yearMonth = YearMonth.parse(compact.substring(0, 6), PAYMENT_MONTH_FORMATTER);
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthValue();
    if (year >= 2019) {
      int eraYear = year - 2018;
      return "令和" + (eraYear == 1 ? "元" : eraYear) + "年" + month + "月";
    }
    return year + "年" + month + "月";
  }

  private record Row(String label, BigDecimal amount, boolean total) {

    private Row(String label, BigDecimal amount) {
      this(label, amount, false);
    }
  }

  private static class PayrollSlipListItemMapper implements RowMapper<PayrollSlipListItem> {

    @Override
    public PayrollSlipListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new PayrollSlipListItem(
          rs.getString("employee_no"),
          rs.getString("employee_name"),
          rs.getString("payment_month"),
          amount(rs, "base_salary"),
          amount(rs, "commuting_allowance"),
          amount(rs, "management_allowance"),
          amount(rs, "transportation_adjustment"),
          amount(rs, "overtime_pay"),
          amount(rs, "advance_expense"),
          amount(rs, "advance_expense2"),
          amount(rs, "other_amount"),
          amount(rs, "transfer_payment_amount"));
    }
  }

  private static class PayrollSlipDetailMapper implements RowMapper<PayrollSlipDetail> {

    @Override
    public PayrollSlipDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new PayrollSlipDetail(
          rs.getString("employee_no"),
          rs.getString("employee_name"),
          rs.getString("employee_type"),
          rs.getString("payment_month"),
          amount(rs, "base_salary"),
          amount(rs, "commuting_allowance"),
          amount(rs, "management_allowance"),
          amount(rs, "transportation_adjustment"),
          amount(rs, "overtime_pay"),
          amount(rs, "advance_expense"),
          amount(rs, "advance_expense2"),
          amount(rs, "other_amount"),
          rs.getString("other_item_name"),
          amount(rs, "employment_insurance_amount"),
          amount(rs, "pension_insurance_amount"),
          amount(rs, "health_insurance_amount"),
          amount(rs, "income_tax"),
          amount(rs, "child_support_amount"),
          amount(rs, "resident_tax_amount"),
          amount(rs, "taxable_income"),
          amount(rs, "transfer_payment_amount"));
    }
  }
}
