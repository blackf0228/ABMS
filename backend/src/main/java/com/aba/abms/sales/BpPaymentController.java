package com.aba.abms.sales;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bp-payments")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class BpPaymentController {
  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
  private final NamedParameterJdbcTemplate jdbcTemplate;
  public BpPaymentController(NamedParameterJdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
  @GetMapping
  public List<BpPaymentSummaryItem> search(
      @RequestParam(required = false) String startMonth,
      @RequestParam(required = false) String endMonth,
      @RequestParam(required = false) String billingSourceCompanyId) {
    YearMonth currentMonth = YearMonth.now();
    LocalDate start = parseMonth(startMonth, currentMonth).atDay(1);
    LocalDate end = parseMonth(endMonth, currentMonth).atEndOfMonth();
    if (start.isAfter(end)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "開始年月は終了年月以前を指定してください。");

    StringBuilder where = new StringBuilder("""
        where s.billing_destination_company_id = '0000' and s.billing_source_company_id <> '0000'
          and s.billing_month between :startMonth and :endMonth
        """);
    Map<String, Object> params = new HashMap<>();
    params.put("startMonth", start);
    params.put("endMonth", end);

    if (billingSourceCompanyId != null && !billingSourceCompanyId.isBlank()) {
      where.append(" and s.billing_source_company_id = :billingSourceCompanyId ");
      params.put("billingSourceCompanyId", billingSourceCompanyId.trim());
    }

    String sql = """
        select
          row_number() over (order by min(s.no)) as display_no,
          to_char(s.billing_month, 'YYYY-MM') as billing_month,
          s.billing_source_company_id,
          coalesce(c.company_name, s.billing_source_company_id) as billing_source_company_name,
          coalesce(sum(s.billing_amount), 0) as billing_amount
        from sales_imports s
        left join customer_masters c
          on c.company_id = s.billing_source_company_id
        """ + where + """
        group by
          s.billing_month,
          s.billing_source_company_id,
          c.company_name
        order by min(s.no)
        """;

    return jdbcTemplate.query(sql, params, (rs, rowNum) -> new BpPaymentSummaryItem(rs.getInt("display_no"),
        rs.getString("billing_month"), rs.getString("billing_source_company_id"),
        rs.getString("billing_source_company_name"), rs.getBigDecimal("billing_amount")));
  }

  @GetMapping("/details")
  public BpPaymentDetailResponse details(
      @RequestParam String billingMonth,
      @RequestParam String billingSourceCompanyId) {
    YearMonth month = parseMonth(billingMonth, YearMonth.now());
    Map<String, Object> params = Map.of(
        "startMonth", month.atDay(1),
        "endMonth", month.atEndOfMonth(),
        "billingSourceCompanyId", billingSourceCompanyId.trim());

    String headerSql = """
        select
          to_char(s.billing_month, 'YYYY-MM') as billing_month,
          s.billing_source_company_id,
          coalesce(c.company_name, s.billing_source_company_id) as billing_source_company_name
        from sales_imports s
        left join customer_masters c
          on c.company_id = s.billing_source_company_id
        where s.billing_destination_company_id = '0000' and s.billing_source_company_id <> '0000'
          and s.billing_month between :startMonth and :endMonth
          and s.billing_source_company_id = :billingSourceCompanyId
        order by s.no
        limit 1
        """;

    BpPaymentDetailResponse header = jdbcTemplate.query(
        headerSql,
        params,
        rs -> rs.next()
            ? new BpPaymentDetailResponse(
                rs.getString("billing_month"),
                rs.getString("billing_source_company_id"),
                rs.getString("billing_source_company_name"),
                List.of())
            : new BpPaymentDetailResponse(
                month.format(MONTH_FORMATTER),
                billingSourceCompanyId.trim(),
                billingSourceCompanyId.trim(),
                List.of()));

    String detailSql = """
        select
          coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, s.engineer_name) as engineer_name,
          s.billing_amount,
          case
            when s.item_type in ('TRANSPORT', 'NON_TAX_ADJ') then s.item_type
            when trim(coalesce(s.engineer_name, '')) = '' then 'NON_TAX_BLANK'
            else s.item_type
          end as item_type
        from sales_imports s
        left join lateral employee_masters_for_month(to_char(s.billing_month, 'YYYYMM')) e
          on e.employee_no = s.engineer_name
        where s.billing_destination_company_id = '0000' and s.billing_source_company_id <> '0000'
          and s.billing_month between :startMonth and :endMonth
          and s.billing_source_company_id = :billingSourceCompanyId
        order by s.no
        """;

    List<SalesDetailItem> items = jdbcTemplate.query(detailSql, params, (rs, rowNum) -> new SalesDetailItem(rs.getString("engineer_name"),
        rs.getBigDecimal("billing_amount"), rs.getString("item_type")));
    return new BpPaymentDetailResponse(
        header.billingMonth(),
        header.billingSourceCompanyId(),
        header.billingSourceCompanyName(),
        items);
  }


  private static YearMonth parseMonth(String value, YearMonth fallback) {
    if (value == null || value.isBlank()) return fallback;
    try {
      return YearMonth.parse(value.trim(), MONTH_FORMATTER);
    } catch (java.time.format.DateTimeParseException error) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "年月はYYYY-MM形式で指定してください。");
    }
  }
}
