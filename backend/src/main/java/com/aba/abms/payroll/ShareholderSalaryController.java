package com.aba.abms.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll/shareholder-salaries")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class ShareholderSalaryController {

  private static final List<String> EMPLOYEE_NOS = List.of("0010", "0001", "0002", "0006", "0011");

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ShareholderSalaryController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public ShareholderSalaryResponse load(@RequestParam String employeeNo) {
    validateEmployee(employeeNo);
    YearMonth current = YearMonth.from(LocalDate.now());
    YearMonth fiscalStart = YearMonth.of(current.getMonthValue() >= 11 ? current.getYear() : current.getYear() - 1, 11);
    YearMonth fiscalEnd = fiscalStart.plusMonths(11);

    Map<String, Map<String, BigDecimal>> saved = new HashMap<>();
    Set<String> manualCompanyMonths = new HashSet<>();
    jdbcTemplate.query("""
        select payment_month, contract_amount, overtime_amount, base_salary,
               advance_expense2, company_total_amount, company_total_manual
        from shareholder_salaries
        where employee_no = :employeeNo
        """, Map.of("employeeNo", employeeNo.trim()), rs -> {
      Map<String, BigDecimal> values = new HashMap<>();
      values.put("contractAmount", amount(rs.getBigDecimal("contract_amount")));
      values.put("overtimeAmount", amount(rs.getBigDecimal("overtime_amount")));
      values.put("baseSalary", amount(rs.getBigDecimal("base_salary")));
      values.put("advanceExpense2", amount(rs.getBigDecimal("advance_expense2")));
      values.put("companyTotalAmount", amount(rs.getBigDecimal("company_total_amount")));
      String paymentMonth = rs.getString("payment_month");
      saved.put(paymentMonth, values);
      if (rs.getBoolean("company_total_manual")) {
        manualCompanyMonths.add(paymentMonth);
      }
    });

    Map<String, Object> employee = jdbcTemplate.queryForMap("""
        select coalesce(nullif(trim(concat_ws(' ', last_name, first_name)), ''), employee_name, employee_no) as employee_name,
               coalesce(base_salary, 0) as base_salary
        from employee_masters
        where employee_no = :employeeNo
        """, Map.of("employeeNo", employeeNo.trim()));

    Map<String, Map<String, BigDecimal>> payroll = new HashMap<>();
    jdbcTemplate.query("""
        select coalesce(c.payment_month, d.payment_month) as payment_month,
               coalesce(c.advance_expense2, 0) as advance_expense2,
               coalesce(d.company_total_amount, 0) as company_total_amount
        from (
          select payment_month, advance_expense2
          from monthly_salary_calculation_conditions
          where employee_no = :employeeNo
        ) c
        full join (
          select payment_month, company_total_amount
          from monthly_salary_details
          where employee_no = :employeeNo
        ) d using (payment_month)
        """, Map.of("employeeNo", employeeNo.trim()), rs -> {
      Map<String, BigDecimal> values = new HashMap<>();
      values.put("advanceExpense2", amount(rs.getBigDecimal("advance_expense2")));
      values.put("companyTotalAmount", amount(rs.getBigDecimal("company_total_amount")));
      payroll.put(toDisplayMonth(rs.getString("payment_month")), values);
    });

    List<ShareholderSalaryRow> rows = new ArrayList<>();
    BigDecimal employeeBaseSalary = amount((BigDecimal) employee.get("base_salary"));
    for (YearMonth month = fiscalStart; !month.isAfter(fiscalEnd); month = month.plusMonths(1)) {
      String paymentMonth = month.toString();
      Map<String, BigDecimal> savedValues = saved.get(paymentMonth);
      Map<String, BigDecimal> payrollValues = payroll.getOrDefault(paymentMonth, Map.of());
      BigDecimal contractAmount = value(savedValues, "contractAmount", BigDecimal.ZERO);
      BigDecimal overtimeAmount = value(savedValues, "overtimeAmount", BigDecimal.ZERO);
      BigDecimal baseSalary = value(savedValues, "baseSalary", employeeBaseSalary);
      BigDecimal advanceExpense2 = value(savedValues, "advanceExpense2", value(payrollValues, "advanceExpense2", BigDecimal.ZERO));
      boolean companyTotalManual = manualCompanyMonths.contains(paymentMonth);
      BigDecimal companyTotalAmount = companyTotalManual
          ? value(savedValues, "companyTotalAmount", BigDecimal.ZERO)
          : value(payrollValues, "companyTotalAmount", BigDecimal.ZERO);
      BigDecimal overUnder = contractAmount.add(overtimeAmount)
          .subtract(baseSalary).subtract(advanceExpense2).subtract(companyTotalAmount);
      rows.add(new ShareholderSalaryRow(paymentMonth, contractAmount, overtimeAmount, baseSalary,
          advanceExpense2, companyTotalAmount, overUnder, companyTotalManual));
    }
    BigDecimal total = rows.stream().map(ShareholderSalaryRow::overUnder)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ShareholderSalaryResponse(employeeNo.trim(), String.valueOf(employee.get("employee_name")),
        fiscalStart.toString(), fiscalEnd.toString(), rows, total);
  }

  @PutMapping
  @Transactional
  public ShareholderSalaryResponse save(@RequestBody ShareholderSalarySaveRequest request) {
    validateEmployee(request.employeeNo());
    YearMonth current = YearMonth.from(LocalDate.now());
    YearMonth fiscalStart = YearMonth.of(current.getMonthValue() >= 11 ? current.getYear() : current.getYear() - 1, 11);
    YearMonth fiscalEnd = fiscalStart.plusMonths(11);
    if (request.rows() == null || request.rows().size() != 12) {
      throw new IllegalArgumentException("株主給与の対象月が不正です。");
    }
    for (ShareholderSalarySaveRequest.Row row : request.rows()) {
      YearMonth paymentMonth = YearMonth.parse(row.paymentMonth());
      if (paymentMonth.isBefore(fiscalStart) || paymentMonth.isAfter(fiscalEnd)) {
        throw new IllegalArgumentException("株主給与の支払年月が決算期外です。");
      }
      Map<String, Object> params = new HashMap<>();
      params.put("employeeNo", request.employeeNo().trim());
      params.put("paymentMonth", row.paymentMonth());
      params.put("contractAmount", amount(row.contractAmount()));
      params.put("overtimeAmount", amount(row.overtimeAmount()));
      params.put("baseSalary", amount(row.baseSalary()));
      params.put("advanceExpense2", amount(row.advanceExpense2()));
      params.put("companyTotalAmount", row.companyTotalManual()
          ? amount(row.companyTotalAmount())
          : companyBurden(request.employeeNo().trim(), toCompactMonth(row.paymentMonth())));
      params.put("companyTotalManual", row.companyTotalManual());
      jdbcTemplate.update("""
          insert into shareholder_salaries (
            employee_no, payment_month, contract_amount, overtime_amount,
            base_salary, advance_expense2, company_total_amount, company_total_manual
          ) values (
            :employeeNo, :paymentMonth, :contractAmount, :overtimeAmount,
            :baseSalary, :advanceExpense2, :companyTotalAmount, :companyTotalManual
          )
          on conflict (employee_no, payment_month) do update set
            contract_amount = excluded.contract_amount,
            overtime_amount = excluded.overtime_amount,
            base_salary = excluded.base_salary,
            advance_expense2 = excluded.advance_expense2,
            company_total_amount = excluded.company_total_amount,
            company_total_manual = excluded.company_total_manual
          """, params);
    }
    return load(request.employeeNo());
  }

  private void validateEmployee(String employeeNo) {
    if (employeeNo == null || !EMPLOYEE_NOS.contains(employeeNo.trim())) {
      throw new IllegalArgumentException("対象外の社員です。");
    }
  }

  private BigDecimal companyBurden(String employeeNo, String paymentMonth) {
    BigDecimal value = jdbcTemplate.queryForObject("""
        select coalesce(max(company_total_amount), 0)
        from monthly_salary_details
        where employee_no = :employeeNo and payment_month = :paymentMonth
        """, Map.of("employeeNo", employeeNo, "paymentMonth", paymentMonth), BigDecimal.class);
    return amount(value);
  }

  private String toCompactMonth(String paymentMonth) {
    return YearMonth.parse(paymentMonth).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
  }

  private BigDecimal value(Map<String, BigDecimal> values, String key, BigDecimal fallback) {
    return values == null || !values.containsKey(key) ? fallback : amount(values.get(key));
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }

  private String toDisplayMonth(String paymentMonth) {
    if (paymentMonth != null && paymentMonth.matches("\\d{6}")) {
      return paymentMonth.substring(0, 4) + "-" + paymentMonth.substring(4, 6);
    }
    return paymentMonth;
  }
}
