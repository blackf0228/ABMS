package com.aba.abms.sales;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

  private static final List<String> ALLOWANCE_EMPLOYEE_NOS = List.of("0010", "0002", "0001", "0006", "0011");
  private static final YearMonth ALLOWANCE_START_MONTH = YearMonth.of(2025, 10);
  private static final YearMonth ALLOWANCE_END_MONTH = YearMonth.of(2026, 9);

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
          count(distinct (s.billing_month, s.engineer_name))::numeric as working_person_count,
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
          count(distinct s.engineer_name)::numeric as working_person_count,
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
            count(distinct engineer_name) filter (where billing_source_company_id = '0000')::numeric as working_person_count,
            coalesce(sum(billing_amount) filter (where billing_source_company_id = '0000'), 0) as sales_amount,
            coalesce(sum(billing_amount) filter (
              where billing_destination_company_id = '0000'
                and billing_source_company_id <> '0000'
            ), 0) as bp_payment_amount
          from sales_imports
          where billing_month between :startMonth and :endMonth
          group by date_trunc('month', billing_month)::date
        ),
        salary_rows as (
          select
            pm.target_month,
            (case when d.employee_no in ('0001', '0002', '0006', '0010', '0011') then
              case when ct.contract_count > 0 then coalesce(ct.contract_amount, 0)
                   else coalesce(c.base_salary, 0) end
            else
              coalesce(c.base_salary, 0)
                + coalesce(c.commuting_allowance, 0)
                + coalesce(c.management_allowance, 0)
                + coalesce(c.transportation_adjustment, 0)
                + coalesce(c.overtime_pay, 0)
                + coalesce(d.company_total_amount, 0)
            end) + coalesce(c.advance_expense, 0)
              - case
                  when d.employee_no = '0002' and replace(d.payment_month, '-', '') = '202602' then 585000
                  when d.employee_no = '0010' and replace(d.payment_month, '-', '') = '202602' then 303600
                  when d.employee_no = '0002' and replace(d.payment_month, '-', '') = '202607' then 918874
                  when d.employee_no = '0010' and replace(d.payment_month, '-', '') = '202608' then 183500
                  when d.employee_no = '0010' and replace(d.payment_month, '-', '') = '202609' then 177300
                  when d.employee_no = '0010' and replace(d.payment_month, '-', '') = '202610' then 177300
                  else 0
                end as salary_payment_amount,
            case when d.employee_no in ('0001', '0002', '0006', '0010', '0011')
                   and ct.contract_count > 1 then
              d.employee_no || '（' || to_char(pm.target_month, 'YYYY-MM') || '）'
            end as duplicate_contract_employee
          from monthly_salary_details d
          join lateral employee_masters_for_month(d.payment_month) e
            on e.employee_no = d.employee_no
          left join monthly_salary_calculation_conditions c
            on c.employee_no = d.employee_no
           and c.payment_month = d.payment_month
          cross join lateral (
            select (to_date(replace(d.payment_month, '-', '') || '01', 'YYYYMMDD')
                    - interval '1 month')::date as target_month
          ) pm
          left join lateral (
            select count(*) as contract_count, max(contract_amount) as contract_amount
            from contracts
            where engineer_name = d.employee_no
              and contract_start_date <= pm.target_month + interval '1 month' - interval '1 day'
              and (contract_end_date is null or contract_end_date >= pm.target_month)
              and d.employee_no in ('0001', '0002', '0006', '0010', '0011')
          ) ct on true
          where pm.target_month between :startMonth and :endMonth
            and replace(e.employee_name, '　', '') <> '陳百歳'
        ),
        salaries as (
          select target_month, sum(salary_payment_amount) as salary_payment_amount,
                 string_agg(duplicate_contract_employee, '、' order by duplicate_contract_employee)
                   as duplicate_contract_employees
          from salary_rows
          group by target_month
        )
        select
          to_char(m.target_month, 'YYYY-MM') as posting_month,
          coalesce(s.working_person_count, 0) as working_person_count,
          coalesce(s.sales_amount, 0) as sales_amount,
          coalesce(s.bp_payment_amount, 0) as bp_payment_amount,
          coalesce(sa.salary_payment_amount, 0) as salary_payment_amount,
          coalesce(s.sales_amount, 0)
            - coalesce(s.bp_payment_amount, 0)
            - coalesce(sa.salary_payment_amount, 0) as profit_amount,
          sa.duplicate_contract_employees
        from months m
        left join sales s
          on s.target_month = m.target_month
        left join salaries sa
          on sa.target_month = m.target_month
        order by m.target_month
        """;

    return jdbcTemplate.query(sql, params, new ProfitAnalysisItemMapper());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleProfitAnalysisError(ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode())
        .body(Map.of("message", error.getReason() == null ? "利益解析に失敗しました。" : error.getReason()));
  }

  @GetMapping("/fiscal-profit")
  public FiscalProfitAnalysisResponse fiscalProfitAnalysis() {
    YearMonth start = ALLOWANCE_START_MONTH;
    YearMonth end = ALLOWANCE_END_MONTH;
    List<String> months = new ArrayList<>();
    for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
      months.add(month.toString());
    }
    ProfitContributionService.Result result = new ProfitContributionService(jdbcTemplate).calculate(start, end);
    return new FiscalProfitAnalysisResponse(start.toString(), end.toString(), months,
        result.rows(), result.details(), result.warnings(), result.unallocatedProfit());
  }

  @GetMapping("/allowance-analysis")
  public AllowanceAnalysisResponse allowanceAnalysis() {
    YearMonth fiscalStart = ALLOWANCE_START_MONTH;
    YearMonth fiscalEnd = ALLOWANCE_END_MONTH;
    List<YearMonth> fiscalMonths = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      fiscalMonths.add(fiscalStart.plusMonths(i));
    }

    Map<String, String> allowanceEmployeeNames = new HashMap<>();
    jdbcTemplate.query("""
        select employee_no,
               coalesce(nullif(trim(concat_ws(' ', last_name, first_name)), ''), employee_name, employee_no) as employee_name
        from current_employee_masters
        where employee_no in (:employeeNos)
        """, Map.of("employeeNos", ALLOWANCE_EMPLOYEE_NOS), (rs, rowNum) -> {
          allowanceEmployeeNames.put(rs.getString("employee_no"), rs.getString("employee_name"));
          return 0;
        });

    Map<String, AllowanceAccumulator> grouped = new LinkedHashMap<>();
    for (String employeeNo : ALLOWANCE_EMPLOYEE_NOS) {
      String employeeName = allowanceEmployeeNames.getOrDefault(employeeNo, employeeNo);
      grouped.put(employeeNo + "\u0000" + employeeName,
          new AllowanceAccumulator(employeeNo, employeeName, fiscalMonths));
    }

    List<AllowanceContract> contracts = jdbcTemplate.query("""
        select
          c.project_affiliation as employee_no,
          coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, c.project_affiliation) as employee_name,
          c.engineer_name as engineer_no,
          c.contract_amount,
          c.engineer_unit_price,
          c.customer_name,
          c.contract_start_date,
          c.contract_end_date
        from contracts c
        left join lateral employee_masters_for_period(c.contract_start_date,
          coalesce(c.contract_end_date, greatest(c.contract_start_date, current_date))) e
          on e.employee_no = c.project_affiliation
        left join lateral employee_masters_for_period(c.contract_start_date,
          coalesce(c.contract_end_date, greatest(c.contract_start_date, current_date))) engineer
          on engineer.employee_no = c.engineer_name
        where nullif(trim(c.project_affiliation), '') is not null
          and c.project_affiliation in (:employeeNos)
          and engineer.employee_type in ('1', '2')
          and coalesce(trim(c.engineer_company_name), '') <> '株式会社CMCソフト'
          and c.contract_start_date is not null
          and c.contract_start_date <= :fiscalEnd
          and (c.contract_end_date is null or c.contract_end_date >= :fiscalStart)
        order by employee_name, c.contract_no
        """, Map.of(
        "employeeNos", ALLOWANCE_EMPLOYEE_NOS,
        "fiscalStart", fiscalStart.atDay(1),
        "fiscalEnd", fiscalEnd.atEndOfMonth()),
        (rs, rowNum) -> new AllowanceContract(
            rs.getString("employee_no"),
            rs.getString("employee_name"),
            rs.getString("engineer_no"),
            rs.getBigDecimal("contract_amount"),
            rs.getBigDecimal("engineer_unit_price"),
            rs.getString("customer_name"),
            rs.getDate("contract_start_date").toLocalDate(),
            rs.getDate("contract_end_date") == null ? null : rs.getDate("contract_end_date").toLocalDate()));

    for (AllowanceContract contract : contracts) {
      YearMonth contractStart = YearMonth.from(contract.contractStartDate());
      YearMonth contractEnd = contract.contractEndDate() == null
          ? fiscalEnd
          : YearMonth.from(contract.contractEndDate());
      String key = contract.employeeNo() + "\u0000" + contract.employeeName();
      AllowanceAccumulator accumulator = grouped.computeIfAbsent(
          key,
          ignored -> new AllowanceAccumulator(contract.employeeNo(), contract.employeeName(), fiscalMonths));
      for (int i = 0; i < fiscalMonths.size(); i++) {
        YearMonth month = fiscalMonths.get(i);
        if (!month.isBefore(contractStart) && !month.isAfter(contractEnd)) {
          accumulator.add(i, calculateMonthlyAllowance(contract, month));
        }
      }
    }

    List<AllowanceAnalysisItem> rows = grouped.values().stream()
        .map(AllowanceAccumulator::toItem)
        .toList();
    Map<String, Map<String, BigDecimal>> recruitmentAmounts = new HashMap<>();
    Map<String, Map<String, BigDecimal>> specificAmounts = new HashMap<>();
    jdbcTemplate.query("""
        select employee_no, payment_month, recruitment_allowance, specific_allowance
        from employee_allowances
        where employee_no in (:employeeNos)
        """, Map.of("employeeNos", ALLOWANCE_EMPLOYEE_NOS), (rs, rowNum) -> {
          recruitmentAmounts.computeIfAbsent(rs.getString("employee_no"), ignored -> new HashMap<>())
              .put(rs.getString("payment_month"), rs.getBigDecimal("recruitment_allowance"));
          specificAmounts.computeIfAbsent(rs.getString("employee_no"), ignored -> new HashMap<>())
              .put(rs.getString("payment_month"), rs.getBigDecimal("specific_allowance"));
          return 0;
        });
    return new AllowanceAnalysisResponse(
        fiscalStart.toString(),
        fiscalEnd.toString(),
        fiscalMonths.stream().map(YearMonth::toString).toList(),
        rows,
        recruitmentAmounts,
        specificAmounts);
  }

  @PutMapping("/allowance-analysis/recruitment")
  @Transactional
  public void saveRecruitmentAllowances(@org.springframework.web.bind.annotation.RequestBody AllowanceRecruitmentSaveRequest request) {
    if (request.rows() == null) {
      throw new IllegalArgumentException("募集手当の登録データがありません。");
    }
    for (AllowanceRecruitmentSaveRequest.Row row : request.rows()) {
      if (row == null || row.employeeNo() == null || !ALLOWANCE_EMPLOYEE_NOS.contains(row.employeeNo().trim())) {
        throw new IllegalArgumentException("募集手当の対象社員が不正です。");
      }
      if (row.amounts() == null) {
        continue;
      }
      for (AllowanceRecruitmentSaveRequest.Amount allowanceAmount : row.amounts()) {
        YearMonth.parse(allowanceAmount.paymentMonth());
        jdbcTemplate.update("""
            insert into employee_allowances (employee_no, payment_month, recruitment_allowance)
            values (:employeeNo, :paymentMonth, :allowance)
            on conflict (employee_no, payment_month) do update set
              recruitment_allowance = excluded.recruitment_allowance
            """, Map.of(
            "employeeNo", row.employeeNo().trim(),
            "paymentMonth", allowanceAmount.paymentMonth(),
            "allowance", amount(allowanceAmount.amount())));
      }
    }
  }

  @PutMapping("/allowance-analysis/manual")
  @Transactional
  public void saveManualAllowances(@org.springframework.web.bind.annotation.RequestBody AllowanceManualSaveRequest request) {
    if (request.rows() == null || request.rows().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手当の登録データがありません。");
    }
    List<Map<String, Object>> updates = new ArrayList<>();
    for (AllowanceManualSaveRequest.Row row : request.rows()) {
      if (row == null || row.employeeNo() == null || !ALLOWANCE_EMPLOYEE_NOS.contains(row.employeeNo().trim())
          || row.amounts() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手当の対象社員または登録データが不正です。");
      }
      for (AllowanceManualSaveRequest.Amount allowanceAmount : row.amounts()) {
        if (allowanceAmount == null || allowanceAmount.paymentMonth() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手当の対象年月が不正です。");
        }
        YearMonth month;
        try {
          month = YearMonth.parse(allowanceAmount.paymentMonth());
        } catch (java.time.DateTimeException exception) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手当の対象年月が不正です。");
        }
        if (month.isBefore(ALLOWANCE_START_MONTH) || month.isAfter(ALLOWANCE_END_MONTH)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手当の対象年月は2025/10～2026/9です。");
        }
        updates.add(Map.of(
            "employeeNo", row.employeeNo().trim(),
            "paymentMonth", month.toString(),
            "recruitmentAllowance", amount(allowanceAmount.recruitmentAllowance()),
            "specificAllowance", amount(allowanceAmount.specificAllowance())));
      }
    }
    for (Map<String, Object> params : updates) {
      jdbcTemplate.update("""
          insert into employee_allowances (employee_no, payment_month, recruitment_allowance, specific_allowance)
          values (:employeeNo, :paymentMonth, :recruitmentAllowance, :specificAllowance)
          on conflict (employee_no, payment_month) do update set
            recruitment_allowance = excluded.recruitment_allowance,
            specific_allowance = excluded.specific_allowance
          """, params);
    }
  }

  @GetMapping("/allowance-analysis/details")
  public List<AllowanceAnalysisDetail> allowanceAnalysisDetails(
      @RequestParam String employeeNo,
      @RequestParam String month) {
    YearMonth targetMonth = parseMonth(month, YearMonth.now());
    if (targetMonth.isBefore(ALLOWANCE_START_MONTH) || targetMonth.isAfter(ALLOWANCE_END_MONTH)) {
      return List.of();
    }
    LocalDate monthStart = targetMonth.atDay(1);
    LocalDate monthEnd = targetMonth.atEndOfMonth();
    return jdbcTemplate.query("""
        select
          :month as month,
          c.contract_no,
          c.engineer_name as engineer_no,
          coalesce(nullif(trim(concat_ws(' ', engineer.last_name, engineer.first_name)), ''), engineer.employee_name, c.engineer_name) as engineer_name,
          c.contract_amount,
          c.engineer_unit_price,
          c.customer_name
        from contracts c
        left join employee_masters_for_period(:monthStart, :monthEnd) owner
          on owner.employee_no = c.project_affiliation
        left join employee_masters_for_period(:monthStart, :monthEnd) engineer
          on engineer.employee_no = c.engineer_name
        where c.project_affiliation = :employeeNo
          and engineer.employee_type in ('1', '2')
          and coalesce(trim(c.engineer_company_name), '') <> '株式会社CMCソフト'
          and c.contract_start_date <= :monthEnd
          and (c.contract_end_date is null or c.contract_end_date >= :monthStart)
        order by c.contract_no
        """, Map.of(
        "month", targetMonth.toString(),
        "employeeNo", employeeNo.trim(),
        "monthStart", monthStart,
        "monthEnd", monthEnd),
        (rs, rowNum) -> {
          BigDecimal workload = allowanceWorkload(rs.getString("engineer_no"), targetMonth);
          BigDecimal contractAmount = amount(rs.getBigDecimal("contract_amount")).multiply(workload);
          BigDecimal engineerUnitPrice = amount(rs.getBigDecimal("engineer_unit_price")).multiply(workload);
          String customerName = rs.getString("customer_name");
          return new AllowanceAnalysisDetail(
              rs.getString("month"),
              rs.getInt("contract_no"),
              rs.getString("engineer_name"),
              contractAmount,
              engineerUnitPrice,
              customerName,
              allowanceProfitAdjustment(customerName),
              calculateAllowance(contractAmount, engineerUnitPrice, customerName));
        });
  }

  // 指定された技術者・月の実稼働人月を、手当計算用の契約金額と原価に適用する。
  private static BigDecimal allowanceWorkload(String engineerNo, YearMonth month) {
    if ("0005".equals(engineerNo) && YearMonth.of(2025, 11).equals(month)) {
      return new BigDecimal("0.22"); // 蘇 芮烽
    }
    if ("B011".equals(engineerNo) && YearMonth.of(2026, 1).equals(month)) {
      return new BigDecimal("0.52"); // 楊 月
    }
    if ("B000".equals(engineerNo) && YearMonth.of(2026, 4).equals(month)) {
      return new BigDecimal("0.29"); // 于 占玖
    }
    return BigDecimal.ONE;
  }

  private static BigDecimal calculateMonthlyAllowance(AllowanceContract contract, YearMonth month) {
    return calculateContributionAllowance(contract.engineerNo(), month, contract.contractAmount(),
        contract.engineerUnitPrice(), contract.customerName());
  }

  static BigDecimal calculateContributionAllowance(String engineerNo, YearMonth month,
      BigDecimal contractAmount, BigDecimal engineerUnitPrice, String customerName) {
    BigDecimal workload = allowanceWorkload(engineerNo, month);
    return calculateAllowance(amount(contractAmount).multiply(workload),
        amount(engineerUnitPrice).multiply(workload), customerName);
  }

  private static BigDecimal allowanceProfitAdjustment(String customerName) {
    return "アクセンチュア株式会社".equals(customerName == null ? "" : customerName.trim())
        ? new BigDecimal("-30000") : BigDecimal.ZERO;
  }

  private static BigDecimal calculateAllowance(BigDecimal contractAmount, BigDecimal engineerUnitPrice, String customerName) {
    BigDecimal companyProfit = amount(contractAmount).subtract(amount(engineerUnitPrice))
        .add(allowanceProfitAdjustment(customerName));
    if (companyProfit.compareTo(new BigDecimal("30000")) <= 0) {
      return BigDecimal.ZERO.setScale(1);
    }
    BigDecimal allowance = companyProfit.subtract(new BigDecimal("30000"))
        .multiply(new BigDecimal("0.3"))
        .setScale(1, RoundingMode.HALF_UP);
    return allowance.max(new BigDecimal("10000.0"));
  }

  private static BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static BigDecimal total(List<BigDecimal> values) {
    return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private record AllowanceContract(
      String employeeNo,
      String employeeName,
      String engineerNo,
      BigDecimal contractAmount,
      BigDecimal engineerUnitPrice,
      String customerName,
      LocalDate contractStartDate,
      LocalDate contractEndDate) {
  }

  private static final class AllowanceAccumulator {
    private final String employeeNo;
    private final String employeeName;
    private final List<BigDecimal> amounts;

    private AllowanceAccumulator(String employeeNo, String employeeName, List<YearMonth> months) {
      this.employeeNo = employeeNo;
      this.employeeName = employeeName;
      this.amounts = new ArrayList<>();
      for (int i = 0; i < months.size(); i++) {
        this.amounts.add(BigDecimal.ZERO.setScale(1));
      }
    }

    private void add(int index, BigDecimal amount) {
      amounts.set(index, amounts.get(index).add(amount));
    }

    private AllowanceAnalysisItem toItem() {
      List<AllowanceMonthlyAmount> monthlyAmounts = new ArrayList<>();
      BigDecimal subtotal = BigDecimal.ZERO.setScale(1);
      for (int i = 0; i < amounts.size(); i++) {
        BigDecimal amount = amounts.get(i).setScale(1, RoundingMode.HALF_UP);
        monthlyAmounts.add(new AllowanceMonthlyAmount(null, amount));
        subtotal = subtotal.add(amount);
      }
      return new AllowanceAnalysisItem(
          employeeNo,
          employeeName,
          "営業手当",
          monthlyAmounts,
          subtotal,
          subtotal);
    }
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
          rs.getBigDecimal("billing_amount"),
          rs.getString("item_type"));
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
      String duplicates = rs.getString("duplicate_contract_employees");
      if (duplicates != null && !duplicates.isBlank()) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
            "同月の契約が複数あるため利益を計算できません。社員番号（売上・稼働年月）：" + duplicates);
      }
      return new ProfitAnalysisItem(
          rs.getString("posting_month"),
          rs.getBigDecimal("working_person_count"),
          rs.getBigDecimal("sales_amount"),
          rs.getBigDecimal("bp_payment_amount"),
          rs.getBigDecimal("salary_payment_amount"),
          rs.getBigDecimal("profit_amount"));
    }
  }
}
