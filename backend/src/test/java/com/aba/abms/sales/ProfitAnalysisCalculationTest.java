package com.aba.abms.sales;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

/** Executes the controller's actual SQL against inline fixtures, with no database writes. */
class ProfitAnalysisCalculationTest {
  private SalesController controller(String employeeNo, String contracts, boolean excluded) {
    return controller(employeeNo, contracts, excluded, "2026-05-01", "202606");
  }

  private SalesController controller(String employeeNo, String contracts, boolean excluded,
      String salesMonth, String paymentMonth) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource() {
      @Override public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        connection.setReadOnly(true);
        try (java.sql.Statement statement = connection.createStatement();
             java.sql.ResultSet result = statement.executeQuery("SHOW transaction_read_only")) {
          if (!result.next() || !"on".equals(result.getString(1))) {
            connection.close();
            throw new SQLException("Profit calculation tests require server-side READ ONLY");
          }
        }
        return connection;
      }
    };
    String jdbcUrl = System.getProperty("profit.test.jdbcUrl", "jdbc:postgresql://localhost:5432/abms");
    dataSource.setUrl(jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "readOnlyMode=always");
    dataSource.setUsername(System.getenv().getOrDefault("ABMS_DB_USER", "abms"));
    dataSource.setPassword(System.getenv().getOrDefault("ABMS_DB_PASSWORD", "abms_password"));
    String fixture = """
        with test_employees as (
          select '%s'::text as employee_no, '%s'::text as employee_name
        ), sales_imports(billing_month,billing_source_company_id,billing_destination_company_id,billing_amount,engineer_name) as (
          values (date '%s','0000','customer',600000::numeric,'0009'),
                 (date '%s','0000','customer',400000::numeric,'0009'),
                 (date '%s','bp','0000',200000::numeric,'bp'),
                 (date '%s','bp','unrelated',100000::numeric,'bp')
        ), monthly_salary_details as (
          select '%s'::text as employee_no,'%s'::text as payment_month,
                 999::numeric as transfer_payment_amount,80000::numeric as company_total_amount
        ), monthly_salary_calculation_conditions as (
          select '%s'::text as employee_no,'%s'::text as payment_month,
                 500000::numeric as base_salary,20000::numeric as commuting_allowance,
                 10000::numeric as management_allowance,5000::numeric as transportation_adjustment,
                 15000::numeric as overtime_pay,99000::numeric as advance_expense,
                 88888::numeric as advance_expense2,77777::numeric as other_amount
        ), contracts(contract_no,engineer_name,contract_amount,contract_start_date,contract_end_date) as (
          %s
        ), months as (
        """.formatted(employeeNo, excluded ? "陳　百歳" : "テスト社員",
            salesMonth, salesMonth, salesMonth, salesMonth, employeeNo, paymentMonth,
            employeeNo, paymentMonth, contracts);
    NamedParameterJdbcTemplate real = new NamedParameterJdbcTemplate(dataSource);
    NamedParameterJdbcTemplate fixtureTemplate = new NamedParameterJdbcTemplate(dataSource) {
      @Override public <T> List<T> query(String sql, Map<String, ?> params, RowMapper<T> mapper) {
        String fixtureSql = sql.replace("with months as (", fixture)
            .replace("employee_masters_for_month(d.payment_month)", "(select * from test_employees)");
        return real.query(fixtureSql, params, mapper);
      }
    };
    return new SalesController(fixtureTemplate);
  }

  private String none() {
    return "select 1::integer, 'none'::text, 0::numeric, date '2000-01-01', date '2000-01-31' where false";
  }

  private BigDecimal profit(SalesController controller) {
    return controller.profitAnalysis("2026-05", "2026-05").get(0).profitAmount();
  }

  private BigDecimal profit(SalesController controller, String postingMonth) {
    return controller.profitAnalysis(postingMonth, postingMonth).get(0).profitAmount();
  }

  @Test void ordinaryEmployeeIncludesAdvanceExpenseButNotAdvanceExpense2() {
    assertEquals(0, new BigDecimal("71000").compareTo(profit(controller("0009", none(), false))));
  }

  @ParameterizedTest
  @CsvSource({
      "0002,202602,2026-01,786000",
      "0010,202602,2026-01,504600",
      "0002,202607,2026-06,1119874",
      "0010,202608,2026-07,384500",
      "0010,202609,2026-08,378300",
      "0010,202610,2026-09,378300"
  })
  void specifiedTaxDoubleCountsAreDeductedFromAdvanceExpense(
      String employeeNo, String paymentMonth, String postingMonth, String expectedProfit) {
    SalesController controller = controller(
        employeeNo, none(), false, postingMonth + "-01", paymentMonth);
    assertEquals(0, new BigDecimal(expectedProfit).compareTo(profit(controller, postingMonth)));
  }

  @Test void workingPersonCountDeduplicatesMultipleSalesLinesForTheSameEngineer() {
    ProfitAnalysisItem row = controller("0009", none(), false)
        .profitAnalysis("2026-05", "2026-05").get(0);
    assertEquals(0, BigDecimal.ONE.compareTo(row.workingPersonCount()));
  }

  @Test void salaryPaidInSalesMonthIsNotDeductedFromThatMonth() {
    assertEquals(0, new BigDecimal("800000").compareTo(
        profit(controller("0009", none(), false, "2026-05-01", "202605"))));
  }

  @Test void decemberSalesUseSalaryPaidInJanuaryOfNextYear() {
    ProfitAnalysisItem row = controller("0009", none(), false, "2026-12-01", "202701")
        .profitAnalysis("2026-12", "2026-12").get(0);
    assertEquals("2026-12", row.postingMonth());
    assertEquals(0, new BigDecimal("71000").compareTo(row.profitAmount()));
  }

  @Test void hyphenatedPaymentMonthAlsoUsesPreviousSalesMonth() {
    assertEquals(0, new BigDecimal("71000").compareTo(
        profit(controller("0009", none(), false, "2026-05-01", "2026-06"))));
  }

  @Test void apiExposesTheThreeAmountsUsedToCalculateProfit() throws Exception {
    org.springframework.test.web.servlet.MockMvc mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders
            .standaloneSetup(controller("0009", none(), false)).build();
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/api/sales/profit-analysis").param("startMonth", "2026-05").param("endMonth", "2026-06"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].workingPersonCount").value(1))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].salesAmount").value(1000000))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].bpPaymentAmount").value(200000))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].salaryPaymentAmount").value(729000))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].profitAmount").value(71000))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[1].salesAmount").value(0))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[1].bpPaymentAmount").value(0))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[1].salaryPaymentAmount").value(0));
  }

  @ParameterizedTest @ValueSource(strings={"0001","0002","0006","0010","0011"})
  void designatedEmployeeAddsAdvanceExpenseToContractAmount(String employeeNo) {
    String contract = "values (1,'%s',650000::numeric,date '2026-05-31',null::date)".formatted(employeeNo);
    assertEquals(0, new BigDecimal("51000").compareTo(profit(controller(employeeNo, contract, false))));
  }

  @Test void designatedEmployeeWithoutContractAddsAdvanceExpenseToBaseSalary() {
    assertEquals(0, new BigDecimal("201000").compareTo(profit(controller("0001", none(), false))));
  }

  @Test void contractsOutsideTheMonthAreNotSelected() {
    String contracts = """
      values (1,'0001',650000::numeric,date '2026-01-01',date '2026-04-30'),
             (2,'0001',750000::numeric,date '2026-06-01',null::date)
      """;
    assertEquals(0, new BigDecimal("201000").compareTo(profit(controller("0001", contracts, false))));
  }

  @Test void contractEndingOnFirstDayStillOverlapsMonth() {
    String contract = "values (1,'0001',650000::numeric,date '2026-01-01',date '2026-05-01')";
    assertEquals(0, new BigDecimal("51000").compareTo(profit(controller("0001", contract, false))));
  }

  @Test void zeroContractAmountIsNotTreatedAsMissingContract() {
    String contract = "values (1,'0001',0::numeric,date '2026-05-01',null::date)";
    assertEquals(0, new BigDecimal("701000").compareTo(profit(controller("0001", contract, false))));
  }

  @Test void multipleContractsFailWithEmployeeAndMonth() {
    String contracts = """
      values (1,'0001',650000::numeric,date '2026-05-01',null::date),
             (2,'0001',750000::numeric,date '2026-05-31',null::date)
      """;
    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> profit(controller("0001", contracts, false)));
    assertEquals(422, error.getStatusCode().value());
    assertTrue(error.getReason().contains("0001"));
    assertTrue(error.getReason().contains("2026-05"));
  }

  @Test void excludedEmployeeRemainsExcluded() {
    assertEquals(0, new BigDecimal("800000").compareTo(profit(controller("0022", none(), true))));
  }

  @Test void duplicateContractErrorIsVisibleInApiResponse() throws Exception {
    String contracts = """
      values (1,'0001',650000::numeric,date '2026-05-01',null::date),
             (2,'0001',750000::numeric,date '2026-05-31',null::date)
      """;
    org.springframework.test.web.servlet.MockMvc mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders
            .standaloneSetup(controller("0001", contracts, false)).build();
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/api/sales/profit-analysis").param("startMonth", "2026-05").param("endMonth", "2026-05"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnprocessableEntity())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message",
            org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("0001"),
                org.hamcrest.Matchers.containsString("2026-05"))));
  }

  @Test void monthsWithNoDataReturnZeroProfit() {
    List<ProfitAnalysisItem> rows = controller("0009", none(), false).profitAnalysis("2026-04", "2026-06");
    assertEquals(3, rows.size());
    assertEquals(0, rows.get(0).profitAmount().compareTo(BigDecimal.ZERO));
    assertEquals(0, rows.get(2).profitAmount().compareTo(BigDecimal.ZERO));
  }
}
