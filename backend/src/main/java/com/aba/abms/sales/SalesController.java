package com.aba.abms.sales;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class SalesController {

  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public SalesController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public List<SalesSummaryItem> search(
      @RequestParam(required = false) String startMonth,
      @RequestParam(required = false) String endMonth,
      @RequestParam(required = false) String billingDestinationCompanyId) {
    YearMonth currentMonth = YearMonth.now();
    LocalDate start = parseMonth(startMonth, currentMonth).atDay(1);
    LocalDate end = parseMonth(endMonth, currentMonth).atEndOfMonth();

    StringBuilder where = new StringBuilder("""
        where s.billing_source_company_id = '0000'
          and s.billing_month between :startMonth and :endMonth
        """);
    Map<String, Object> params = new HashMap<>();
    params.put("startMonth", start);
    params.put("endMonth", end);

    if (billingDestinationCompanyId != null && !billingDestinationCompanyId.isBlank()) {
      where.append(" and s.billing_destination_company_id = :billingDestinationCompanyId ");
      params.put("billingDestinationCompanyId", billingDestinationCompanyId.trim());
    }

    String sql = """
        select
          row_number() over (order by min(s.no)) as display_no,
          to_char(s.billing_month, 'YYYY-MM') as billing_month,
          s.billing_destination_company_id,
          coalesce(c.company_name, s.billing_destination_company_id) as billing_destination_company_name,
          sum(s.billing_amount) as billing_amount
        from sales_imports s
        left join customer_masters c
          on c.company_id = s.billing_destination_company_id
        """ + where + """
        group by
          s.billing_month,
          s.billing_destination_company_id,
          c.company_name
        order by min(s.no)
        """;

    return jdbcTemplate.query(sql, params, new SalesSummaryItemMapper());
  }

  @GetMapping("/details")
  public SalesDetailResponse details(
      @RequestParam String billingMonth,
      @RequestParam String billingDestinationCompanyId) {
    YearMonth month = parseMonth(billingMonth, YearMonth.now());
    Map<String, Object> params = Map.of(
        "startMonth", month.atDay(1),
        "endMonth", month.atEndOfMonth(),
        "billingDestinationCompanyId", billingDestinationCompanyId.trim());

    String headerSql = """
        select
          to_char(s.billing_month, 'YYYY-MM') as billing_month,
          s.billing_destination_company_id,
          coalesce(c.company_name, s.billing_destination_company_id) as billing_destination_company_name
        from sales_imports s
        left join customer_masters c
          on c.company_id = s.billing_destination_company_id
        where s.billing_source_company_id = '0000'
          and s.billing_month between :startMonth and :endMonth
          and s.billing_destination_company_id = :billingDestinationCompanyId
        order by s.no
        limit 1
        """;

    SalesDetailResponse header = jdbcTemplate.query(
        headerSql,
        params,
        rs -> rs.next()
            ? new SalesDetailResponse(
                rs.getString("billing_month"),
                rs.getString("billing_destination_company_id"),
                rs.getString("billing_destination_company_name"),
                List.of())
            : new SalesDetailResponse(
                month.format(MONTH_FORMATTER),
                billingDestinationCompanyId.trim(),
                billingDestinationCompanyId.trim(),
                List.of()));

    String detailSql = """
        select
          s.engineer_name,
          s.billing_amount
        from sales_imports s
        where s.billing_source_company_id = '0000'
          and s.billing_month between :startMonth and :endMonth
          and s.billing_destination_company_id = :billingDestinationCompanyId
        order by s.no
        """;

    List<SalesDetailItem> items = jdbcTemplate.query(detailSql, params, new SalesDetailItemMapper());
    return new SalesDetailResponse(
        header.billingMonth(),
        header.billingDestinationCompanyId(),
        header.billingDestinationCompanyName(),
        items);
  }

  @GetMapping("/analysis")
  public List<SalesAnalysisItem> analysis(
      @RequestParam(required = false) String startMonth,
      @RequestParam(required = false) String endMonth) {
    YearMonth currentMonth = YearMonth.now();
    LocalDate start = parseMonth(startMonth, currentMonth).atDay(1);
    LocalDate end = parseMonth(endMonth, currentMonth).atEndOfMonth();
    Map<String, Object> params = Map.of(
        "startMonth", start,
        "endMonth", end);

    String sql = """
        select
          s.billing_destination_company_id,
          coalesce(c.company_name, s.billing_destination_company_id) as billing_destination_company_name,
          count(*)::numeric as working_person_count,
          sum(s.billing_amount) as sales_amount
        from sales_imports s
        left join customer_masters c
          on c.company_id = s.billing_destination_company_id
        where s.billing_source_company_id = '0000'
          and s.billing_month between :startMonth and :endMonth
        group by
          s.billing_destination_company_id,
          c.company_name
        order by
          sum(s.billing_amount) desc,
          coalesce(c.company_name, s.billing_destination_company_id)
        """;

    return jdbcTemplate.query(sql, params, new SalesAnalysisItemMapper());
  }

  @GetMapping("/analysis/monthly")
  public List<SalesMonthlyAnalysisItem> monthlyAnalysis(
      @RequestParam(required = false) String startMonth,
      @RequestParam(required = false) String endMonth,
      @RequestParam(required = false) String billingDestinationCompanyId) {
    YearMonth currentMonth = YearMonth.now();
    LocalDate start = parseMonth(startMonth, currentMonth).atDay(1);
    LocalDate end = parseMonth(endMonth, currentMonth).atEndOfMonth();

    StringBuilder where = new StringBuilder("""
        where s.billing_source_company_id = '0000'
          and s.billing_month between :startMonth and :endMonth
        """);
    Map<String, Object> params = new HashMap<>();
    params.put("startMonth", start);
    params.put("endMonth", end);

    if (billingDestinationCompanyId != null && !billingDestinationCompanyId.isBlank()) {
      where.append(" and s.billing_destination_company_id = :billingDestinationCompanyId ");
      params.put("billingDestinationCompanyId", billingDestinationCompanyId.trim());
    }

    String sql = """
        select
          to_char(s.billing_month, 'YYYY-MM') as billing_month,
          count(*)::numeric as working_person_count,
          sum(s.billing_amount) as sales_amount
        from sales_imports s
        """ + where + """
        group by
          s.billing_month
        order by
          s.billing_month
        """;

    return jdbcTemplate.query(sql, params, new SalesMonthlyAnalysisItemMapper());
  }

  @GetMapping("/profit-analysis")
  public List<ProfitAnalysisItem> profitAnalysis(
      @RequestParam(required = false) String startMonth,
      @RequestParam(required = false) String endMonth) {
    YearMonth currentMonth = YearMonth.now();
    LocalDate start = parseMonth(startMonth, currentMonth).atDay(1);
    LocalDate end = parseMonth(endMonth, currentMonth).atEndOfMonth();
    Map<String, Object> params = Map.of(
        "startMonth", start,
        "endMonth", end);

    String sql = """
        with months as (
          select generate_series(
            date_trunc('month', cast(:startMonth as date)),
            date_trunc('month', cast(:endMonth as date)),
            interval '1 month'
          )::date as target_month
        ),
        sales as (
          select
            date_trunc('month', billing_month)::date as target_month,
            count(*) filter (where billing_source_company_id = '0000')::numeric as working_person_count,
            coalesce(sum(billing_amount) filter (where billing_source_company_id = '0000'), 0) as sales_amount,
            coalesce(sum(billing_amount) filter (where billing_source_company_id <> '0000'), 0) as bp_payment_amount
          from sales_imports
          where billing_month between :startMonth and :endMonth
          group by date_trunc('month', billing_month)::date
        ),
        salaries as (
          select
            case
              when d.payment_month ~ '^\\d{6}$' then to_date(d.payment_month || '01', 'YYYYMMDD')
              else to_date(d.payment_month || '-01', 'YYYY-MM-DD')
            end as target_month,
            sum(
              coalesce(d.transfer_payment_amount, 0)
              + coalesce(d.company_total_amount, 0)
              - coalesce(c.advance_expense2, 0)
            ) as salary_payment_amount
          from monthly_salary_details d
          join employee_masters e
            on e.employee_no = d.employee_no
          left join monthly_salary_calculation_conditions c
            on c.employee_no = d.employee_no
           and c.payment_month = d.payment_month
          where case
              when d.payment_month ~ '^\\d{6}$' then to_date(d.payment_month || '01', 'YYYYMMDD')
              else to_date(d.payment_month || '-01', 'YYYY-MM-DD')
            end between :startMonth and :endMonth
            and replace(e.employee_name, '　', '') <> '陳百歳'
          group by case
              when d.payment_month ~ '^\\d{6}$' then to_date(d.payment_month || '01', 'YYYYMMDD')
              else to_date(d.payment_month || '-01', 'YYYY-MM-DD')
            end
        )
        select
          to_char(m.target_month, 'YYYY-MM') as posting_month,
          coalesce(s.working_person_count, 0) as working_person_count,
          coalesce(s.sales_amount, 0)
            - coalesce(s.bp_payment_amount, 0)
            - coalesce(sa.salary_payment_amount, 0) as profit_amount
        from months m
        left join sales s
          on s.target_month = m.target_month
        left join salaries sa
          on sa.target_month = m.target_month
        order by m.target_month
        """;

    return jdbcTemplate.query(sql, params, new ProfitAnalysisItemMapper());
  }

  private static YearMonth parseMonth(String value, YearMonth fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String trimmed = value.trim();
    if (trimmed.matches("\\d{6}")) {
      return YearMonth.of(
          Integer.parseInt(trimmed.substring(0, 4)),
          Integer.parseInt(trimmed.substring(4, 6)));
    }
    if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
      return YearMonth.from(LocalDate.parse(trimmed));
    }
    return YearMonth.parse(trimmed, MONTH_FORMATTER);
  }

  private static class SalesSummaryItemMapper implements RowMapper<SalesSummaryItem> {

    @Override
    public SalesSummaryItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new SalesSummaryItem(
          rs.getInt("display_no"),
          rs.getString("billing_month"),
          rs.getString("billing_destination_company_id"),
          rs.getString("billing_destination_company_name"),
          rs.getBigDecimal("billing_amount"));
    }
  }

  private static class SalesDetailItemMapper implements RowMapper<SalesDetailItem> {

    @Override
    public SalesDetailItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new SalesDetailItem(
          rs.getString("engineer_name"),
          rs.getBigDecimal("billing_amount"));
    }
  }

  private static class SalesAnalysisItemMapper implements RowMapper<SalesAnalysisItem> {

    @Override
    public SalesAnalysisItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new SalesAnalysisItem(
          rs.getString("billing_destination_company_id"),
          rs.getString("billing_destination_company_name"),
          rs.getBigDecimal("working_person_count"),
          rs.getBigDecimal("sales_amount"));
    }
  }

  private static class SalesMonthlyAnalysisItemMapper implements RowMapper<SalesMonthlyAnalysisItem> {

    @Override
    public SalesMonthlyAnalysisItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new SalesMonthlyAnalysisItem(
          rs.getString("billing_month"),
          rs.getBigDecimal("working_person_count"),
          rs.getBigDecimal("sales_amount"));
    }
  }

  private static class ProfitAnalysisItemMapper implements RowMapper<ProfitAnalysisItem> {

    @Override
    public ProfitAnalysisItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ProfitAnalysisItem(
          rs.getString("posting_month"),
          rs.getBigDecimal("working_person_count"),
          rs.getBigDecimal("profit_amount"));
    }
  }
}
