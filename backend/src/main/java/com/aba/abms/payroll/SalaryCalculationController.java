package com.aba.abms.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll/calculations")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class SalaryCalculationController {

  private static final DateTimeFormatter PAYMENT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public SalaryCalculationController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/conditions")
  public SalaryConditionResponse conditions(@RequestParam(required = false) String paymentMonth) {
    String targetMonth = normalizePaymentMonth(paymentMonth);
    List<SalaryConditionRow> rows = loadConditionRows(targetMonth);
    return new SalaryConditionResponse(targetMonth, previousMonth(targetMonth), !rows.isEmpty(), rows);
  }

  @GetMapping("/conditions/copy-previous")
  public SalaryConditionResponse copyPrevious(@RequestParam String paymentMonth) {
    String targetMonth = normalizePaymentMonth(paymentMonth);
    String sourceMonth = previousMonth(targetMonth);
    List<SalaryConditionRow> rows = loadConditionRows(sourceMonth).stream()
        .map(row -> new SalaryConditionRow(
            row.employeeNo(),
            row.employeeName(),
            row.employeeType(),
            targetMonth,
            row.baseSalary(),
            row.consumptionTaxRate(),
            row.commutingAllowance(),
            row.managementAllowance(),
            row.transportationAdjustment(),
            row.overtimePay(),
            row.advanceExpense(),
            row.advanceExpense2(),
            row.otherAmount(),
            row.otherItemName()))
        .toList();
    return new SalaryConditionResponse(targetMonth, sourceMonth, false, rows);
  }

  @PutMapping("/conditions")
  @Transactional
  public SalaryConditionResponse saveConditions(@RequestBody SalaryConditionSaveRequest request) {
    String targetMonth = normalizePaymentMonth(request.paymentMonth());
    for (SalaryConditionRow row : request.rows()) {
      if (row.employeeNo() == null || row.employeeNo().isBlank()) {
        continue;
      }
      upsertEmployee(row);
      upsertCondition(targetMonth, row);
    }
    return conditions(targetMonth);
  }

  @GetMapping("/salary-details/draft")
  public List<SalaryDetailDraftRow> draftSalaryDetails(@RequestParam String paymentMonth) {
    return loadConditionRows(normalizePaymentMonth(paymentMonth)).stream()
        .map(this::calculateSalaryDetail)
        .toList();
  }

  @PostMapping("/salary-details")
  @Transactional
  public List<SalaryDetailDraftRow> saveSalaryDetails(@RequestBody SalaryDetailSaveRequest request) {
    String targetMonth = normalizePaymentMonth(request.paymentMonth());
    for (SalaryDetailDraftRow row : request.rows()) {
      if (row.employeeNo() == null || row.employeeNo().isBlank()) {
        continue;
      }
      boolean employeeTypeOne = isEmployeeTypeOne(findEmployeeType(row.employeeNo()));
      boolean employmentInsuranceExempt = isEmploymentInsuranceExemptEmployee(row.employeeNo());
      Map<String, Object> params = new HashMap<>();
      params.put("employeeNo", row.employeeNo().trim());
      params.put("paymentMonth", targetMonth);
      params.put("employmentInsuranceAmount", employeeTypeOne || employmentInsuranceExempt ? ZERO : amount(row.employmentInsuranceAmount()));
      params.put("pensionInsuranceAmount", employeeTypeOne ? ZERO : amount(row.pensionInsuranceAmount()));
      params.put("healthInsuranceAmount", employeeTypeOne ? ZERO : amount(row.healthInsuranceAmount()));
      params.put("incomeTax", employeeTypeOne ? ZERO : amount(row.incomeTax()));
      params.put("childSupportAmount", employeeTypeOne ? ZERO : amount(row.childSupportAmount()));
      params.put("taxableIncome", employeeTypeOne ? ZERO : amount(row.taxableIncome()));
      params.put("transferPaymentAmount", amount(row.transferPaymentAmount()));
      jdbcTemplate.update("""
          insert into monthly_salary_details (
            employee_no,
            payment_month,
            employment_insurance_amount,
            pension_insurance_amount,
            health_insurance_amount,
            income_tax,
            child_support_amount,
            taxable_income,
            transfer_payment_amount
          ) values (
            :employeeNo,
            :paymentMonth,
            :employmentInsuranceAmount,
            :pensionInsuranceAmount,
            :healthInsuranceAmount,
            :incomeTax,
            :childSupportAmount,
            :taxableIncome,
            :transferPaymentAmount
          )
          on conflict (employee_no, payment_month) do update set
            employment_insurance_amount = excluded.employment_insurance_amount,
            pension_insurance_amount = excluded.pension_insurance_amount,
            health_insurance_amount = excluded.health_insurance_amount,
            income_tax = excluded.income_tax,
            child_support_amount = excluded.child_support_amount,
            taxable_income = excluded.taxable_income,
            transfer_payment_amount = excluded.transfer_payment_amount
          """, params);
    }
    return request.rows();
  }

  private List<SalaryConditionRow> loadConditionRows(String paymentMonth) {
    return jdbcTemplate.query("""
        select
          c.employee_no,
          coalesce(e.employee_name, '') as employee_name,
          coalesce(e.employee_type, '') as employee_type,
          c.payment_month,
          coalesce(e.base_salary, 0) as base_salary,
          coalesce(e.consumption_tax_rate, 0) as consumption_tax_rate,
          c.commuting_allowance,
          c.management_allowance,
          c.transportation_adjustment,
          c.overtime_pay,
          c.advance_expense,
          c.advance_expense2,
          c.other_amount,
          c.other_item_name
        from monthly_salary_calculation_conditions c
        left join employee_masters e
          on e.employee_no = c.employee_no
        where c.payment_month = :paymentMonth
        order by c.employee_no
        """, Map.of("paymentMonth", paymentMonth), new SalaryConditionRowMapper());
  }

  private void upsertEmployee(SalaryConditionRow row) {
    Map<String, Object> params = new HashMap<>();
    params.put("employeeNo", row.employeeNo().trim());
    params.put("employeeName", row.employeeName() == null ? "" : row.employeeName().trim());
    jdbcTemplate.update("""
        insert into employee_masters (
          employee_no,
          employee_name,
          employee_type,
          base_salary,
          dependent_count,
          deduction_count
        ) values (
          :employeeNo,
          :employeeName,
          '1',
          0,
          0,
          0
        )
        on conflict (employee_no) do update set
          employee_name = case
            when :employeeName = '' then employee_masters.employee_name
            else :employeeName
          end
        """, params);
  }

  private void upsertCondition(String paymentMonth, SalaryConditionRow row) {
    Map<String, Object> params = new HashMap<>();
    params.put("employeeNo", row.employeeNo().trim());
    params.put("paymentMonth", paymentMonth);
    params.put("commutingAllowance", amount(row.commutingAllowance()));
    params.put("managementAllowance", amount(row.managementAllowance()));
    params.put("transportationAdjustment", amount(row.transportationAdjustment()));
    params.put("overtimePay", amount(row.overtimePay()));
    params.put("advanceExpense", amount(row.advanceExpense()));
    params.put("advanceExpense2", amount(row.advanceExpense2()));
    params.put("otherAmount", amount(row.otherAmount()));
    params.put("otherItemName", row.otherItemName() == null ? "" : row.otherItemName().trim());
    jdbcTemplate.update("""
        insert into monthly_salary_calculation_conditions (
          employee_no,
          payment_month,
          commuting_allowance,
          management_allowance,
          transportation_adjustment,
          overtime_pay,
          advance_expense,
          advance_expense2,
          other_amount,
          other_item_name
        ) values (
          :employeeNo,
          :paymentMonth,
          :commutingAllowance,
          :managementAllowance,
          :transportationAdjustment,
          :overtimePay,
          :advanceExpense,
          :advanceExpense2,
          :otherAmount,
          :otherItemName
        )
        on conflict (employee_no, payment_month) do update set
          commuting_allowance = excluded.commuting_allowance,
          management_allowance = excluded.management_allowance,
          transportation_adjustment = excluded.transportation_adjustment,
          overtime_pay = excluded.overtime_pay,
          advance_expense = excluded.advance_expense,
          advance_expense2 = excluded.advance_expense2,
          other_amount = excluded.other_amount,
          other_item_name = excluded.other_item_name
        """, params);
  }

  private SalaryDetailDraftRow calculateSalaryDetail(SalaryConditionRow row) {
    BigDecimal baseSalary = amount(row.baseSalary());
    if (isEmployeeTypeOne(row.employeeType())) {
      BigDecimal remuneration = baseSalary
          .add(amount(row.overtimePay()))
          .multiply(BigDecimal.ONE.add(amount(row.consumptionTaxRate())));
      BigDecimal supportTotal = remuneration
          .add(amount(row.transportationAdjustment()))
          .add(amount(row.commutingAllowance()));
      BigDecimal transferPayment = supportTotal.add(amount(row.otherAmount()));
      return new SalaryDetailDraftRow(
          row.employeeNo(),
          row.employeeName(),
          row.employeeType(),
          row.paymentMonth(),
          money(baseSalary),
          money(remuneration),
          money(supportTotal),
          ZERO,
          ZERO,
          ZERO,
          ZERO,
          ZERO,
          ZERO,
          ZERO,
          ZERO,
          money(transferPayment),
          money(row.otherAmount()));
    }

    BigDecimal remuneration = baseSalary
        .add(amount(row.commutingAllowance()))
        .add(amount(row.managementAllowance()))
        .add(amount(row.transportationAdjustment()))
        .add(amount(row.overtimePay()));
    BigDecimal supportTotal = remuneration
        .add(amount(row.advanceExpense()))
        .add(amount(row.advanceExpense2()));
    BigDecimal employmentInsurance = isEmploymentInsuranceExemptEmployee(row.employeeNo())
        ? ZERO
        : calculateEmploymentInsurance(row.paymentMonth(), remuneration);
    SocialPremium premium = findSocialPremium(row.paymentMonth(), remuneration);
    BigDecimal healthInsurance = decimalOne(premium.healthInsuranceEmployeeAmount());
    BigDecimal pensionInsurance = premium.pensionInsuranceEmployeeAmount();
    BigDecimal childSupport = decimalOne(premium.childSupportEmployeeAmount());
    BigDecimal residentTax = findResidentTax(row.employeeNo(), row.paymentMonth());
    BigDecimal taxableIncome = remuneration
        .subtract(amount(row.commutingAllowance()))
        .subtract(employmentInsurance)
        .subtract(pensionInsurance)
        .subtract(healthInsurance);
    BigDecimal incomeTax = calculateIncomeTax(row.employeeNo(), row.paymentMonth(), taxableIncome);
    BigDecimal deductionTotal = employmentInsurance
        .add(pensionInsurance)
        .add(healthInsurance)
        .add(residentTax)
        .add(childSupport)
        .add(incomeTax);
    deductionTotal = truncate(deductionTotal);
    BigDecimal transferPayment = supportTotal
        .subtract(deductionTotal)
        .add(amount(row.otherAmount()));
    return new SalaryDetailDraftRow(
        row.employeeNo(),
        row.employeeName(),
        row.employeeType(),
        row.paymentMonth(),
        money(baseSalary),
        money(remuneration),
        money(supportTotal),
        money(employmentInsurance),
        money(pensionInsurance),
        decimalOne(healthInsurance),
        money(residentTax),
        decimalOne(childSupport),
        money(taxableIncome),
        money(incomeTax),
        truncate(deductionTotal),
        money(transferPayment),
        money(row.otherAmount()));
  }

  private BigDecimal calculateEmploymentInsurance(String paymentMonth, BigDecimal remuneration) {
    LocalDate targetDate = paymentMonthDate(paymentMonth);
    List<BigDecimal> rates = jdbcTemplate.query("""
        select worker_rate
        from employment_insurance_rates
        where effective_start_date <= :targetDate
          and effective_end_date >= :targetDate
        order by no
        limit 1
        """, Map.of("targetDate", targetDate), (rs, rowNum) -> rs.getBigDecimal("worker_rate"));
    if (rates.isEmpty()) {
      return ZERO;
    }
    return money(remuneration.multiply(rates.get(0)));
  }

  private SocialPremium findSocialPremium(String paymentMonth, BigDecimal remuneration) {
    Map<String, Object> params = new HashMap<>();
    params.put("paymentMonth", paymentMonth);
    params.put("remuneration", remuneration);
    List<SocialPremium> premiums = jdbcTemplate.query("""
        select
          coalesce(health_insurance_employee_amount, 0) as health_insurance_employee_amount,
          coalesce(pension_insurance_employee_amount, 0) as pension_insurance_employee_amount,
          coalesce(child_support_employee_amount, 0) as child_support_employee_amount
        from social_insurance_premium_amounts
        where payment_month <= :paymentMonth
          and (monthly_remuneration_from is null or monthly_remuneration_from <= :remuneration)
          and (monthly_remuneration_to is null or monthly_remuneration_to > :remuneration)
        order by payment_month desc, no
        limit 1
        """, params, (rs, rowNum) -> new SocialPremium(
            amount(rs, "health_insurance_employee_amount"),
            amount(rs, "pension_insurance_employee_amount"),
            amount(rs, "child_support_employee_amount")));
    if (premiums.isEmpty()) {
      return new SocialPremium(ZERO, ZERO, ZERO);
    }
    return premiums.get(0);
  }

  private BigDecimal findResidentTax(String employeeNo, String paymentMonth) {
    List<BigDecimal> taxes = jdbcTemplate.query("""
        select payment_amount
        from employee_resident_tax_details
        where employee_no = :employeeNo
          and payment_month = :paymentMonth
        """, Map.of("employeeNo", employeeNo, "paymentMonth", paymentMonth),
        (rs, rowNum) -> rs.getBigDecimal("payment_amount"));
    return taxes.isEmpty() ? ZERO : amount(taxes.get(0));
  }

  private BigDecimal calculateIncomeTax(String employeeNo, String paymentMonth, BigDecimal taxableIncome) {
    if (taxableIncome.compareTo(ZERO) <= 0) {
      return ZERO;
    }

    int deductionCount = findDeductionCount(employeeNo);
    int lookupDeductionCount = Math.min(deductionCount, 7);
    Map<String, Object> params = new HashMap<>();
    params.put("taxYear", paymentMonth.substring(0, 4));
    params.put("dependentCount", lookupDeductionCount);
    params.put("taxableIncome", taxableIncome);
    List<IncomeTaxRate> rates = jdbcTemplate.query("""
        select
          tax_amount,
          base_tax_amount,
          additional_base_amount,
          additional_rate
        from income_tax_withholding_monthly_rates
        where tax_year = (
            select max(tax_year)
            from income_tax_withholding_monthly_rates
            where tax_year <= :taxYear
              and salary_type = 'MONTHLY'
              and withholding_type = 'KO'
          )
          and salary_type = 'MONTHLY'
          and withholding_type = 'KO'
          and dependent_count = :dependentCount
          and income_from <= :taxableIncome
          and (income_to is null or income_to > :taxableIncome)
        order by income_from desc
        limit 1
        """, params, (rs, rowNum) -> new IncomeTaxRate(
            amount(rs, "tax_amount"),
            amount(rs, "base_tax_amount"),
            amount(rs, "additional_base_amount"),
            amount(rs, "additional_rate")));
    if (rates.isEmpty()) {
      return ZERO;
    }

    IncomeTaxRate rate = rates.get(0);
    BigDecimal tax = rate.taxAmount();
    if (rate.additionalRate().compareTo(ZERO) > 0) {
      tax = rate.baseTaxAmount()
          .add(taxableIncome.subtract(rate.additionalBaseAmount()).multiply(rate.additionalRate()));
    }
    if (deductionCount > 7) {
      tax = tax.subtract(BigDecimal.valueOf(deductionCount - 7).multiply(BigDecimal.valueOf(1610)));
    }
    return money(tax.max(ZERO));
  }

  private int findDeductionCount(String employeeNo) {
    List<Integer> counts = jdbcTemplate.query("""
        select deduction_count
        from employee_masters
        where employee_no = :employeeNo
        """, Map.of("employeeNo", employeeNo), (rs, rowNum) -> rs.getInt("deduction_count"));
    return counts.isEmpty() ? 0 : Math.max(counts.get(0), 0);
  }

  private String findEmployeeType(String employeeNo) {
    List<String> types = jdbcTemplate.query("""
        select employee_type
        from employee_masters
        where employee_no = :employeeNo
        """, Map.of("employeeNo", employeeNo), (rs, rowNum) -> rs.getString("employee_type"));
    return types.isEmpty() ? "" : types.get(0);
  }

  private static boolean isEmployeeTypeOne(String employeeType) {
    return employeeType != null && "1".equals(employeeType.trim());
  }

  private static boolean isEmploymentInsuranceExemptEmployee(String employeeNo) {
    return employeeNo != null && "0010".equals(employeeNo.trim());
  }

  private String normalizePaymentMonth(String paymentMonth) {
    if (paymentMonth != null && !paymentMonth.isBlank()) {
      return paymentMonth.trim().replace("-", "");
    }
    return YearMonth.now().format(PAYMENT_MONTH_FORMATTER);
  }

  private String previousMonth(String paymentMonth) {
    YearMonth yearMonth = YearMonth.parse(paymentMonth, PAYMENT_MONTH_FORMATTER);
    return yearMonth.minusMonths(1).format(PAYMENT_MONTH_FORMATTER);
  }

  private LocalDate paymentMonthDate(String paymentMonth) {
    return YearMonth.parse(paymentMonth, PAYMENT_MONTH_FORMATTER).atDay(1);
  }

  private static BigDecimal amount(BigDecimal value) {
    return value == null ? ZERO : value;
  }

  private static BigDecimal amount(ResultSet rs, String name) throws SQLException {
    BigDecimal value = rs.getBigDecimal(name);
    return value == null ? ZERO : value;
  }

  private static BigDecimal money(BigDecimal value) {
    return amount(value).setScale(0, RoundingMode.HALF_UP);
  }

  private static BigDecimal decimalOne(BigDecimal value) {
    return amount(value).setScale(1, RoundingMode.HALF_UP);
  }

  private static BigDecimal truncate(BigDecimal value) {
    return amount(value).setScale(0, RoundingMode.DOWN);
  }

  private record SocialPremium(
      BigDecimal healthInsuranceEmployeeAmount,
      BigDecimal pensionInsuranceEmployeeAmount,
      BigDecimal childSupportEmployeeAmount
  ) {
  }

  private record IncomeTaxRate(
      BigDecimal taxAmount,
      BigDecimal baseTaxAmount,
      BigDecimal additionalBaseAmount,
      BigDecimal additionalRate
  ) {
  }

  private static class SalaryConditionRowMapper implements RowMapper<SalaryConditionRow> {

    @Override
    public SalaryConditionRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new SalaryConditionRow(
          rs.getString("employee_no"),
          rs.getString("employee_name"),
          rs.getString("employee_type"),
          rs.getString("payment_month"),
          amount(rs, "base_salary"),
          amount(rs, "consumption_tax_rate"),
          amount(rs, "commuting_allowance"),
          amount(rs, "management_allowance"),
          amount(rs, "transportation_adjustment"),
          amount(rs, "overtime_pay"),
          amount(rs, "advance_expense"),
          amount(rs, "advance_expense2"),
          amount(rs, "other_amount"),
          rs.getString("other_item_name"));
    }
  }
}
