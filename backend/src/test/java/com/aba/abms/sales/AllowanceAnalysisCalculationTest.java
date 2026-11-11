package com.aba.abms.sales;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Runs the controller's allowance SQL with inline fixtures and a read-only connection. */
class AllowanceAnalysisCalculationTest {
  private SalesController controller(String contracts) {
    return controller(contracts, "外部取引先");
  }

  private SalesController controller(String contracts, String customerName) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource() {
      @Override public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        connection.setReadOnly(true);
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SHOW transaction_read_only")) {
          if (!result.next() || !"on".equals(result.getString(1))) {
            connection.close();
            throw new SQLException("Allowance tests require server-side READ ONLY");
          }
        }
        return connection;
      }
    };
    String url = System.getProperty("profit.test.jdbcUrl", "jdbc:postgresql://localhost:5432/abms");
    dataSource.setUrl(url + (url.contains("?") ? "&" : "?") + "readOnlyMode=always");
    dataSource.setUsername(System.getenv().getOrDefault("ABMS_DB_USER", "abms"));
    dataSource.setPassword(System.getenv().getOrDefault("ABMS_DB_PASSWORD", "abms_password"));
    String fixture = """
        with fixture_employees(employee_no,employee_name,last_name,first_name,employee_type) as (
          values ('0001','担当 者','担当','者','0'),
                 ('individual','個人 技術者','個人','技術者','1'),
                 ('bp','BP 技術者','BP','技術者','2'),
                 ('regular','正社員 技術者','正社員','技術者','0')
        ), fixture_contracts(contract_no,project_affiliation,engineer_name,engineer_company_name,
                     contract_amount,engineer_unit_price,contract_start_date,contract_end_date) as (
          %s
        ), contracts as (
          select *, '%s'::text as customer_name from fixture_contracts
        ), employee_allowances(employee_no,payment_month,recruitment_allowance,specific_allowance) as (
          select '0001'::text,'2025-10'::text,1234::numeric,5678::numeric
        )
        """.formatted(contracts, customerName);
    NamedParameterJdbcTemplate real = new NamedParameterJdbcTemplate(dataSource);
    NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource) {
      @Override public <T> List<T> query(String sql, Map<String, ?> params, RowMapper<T> mapper) {
        String transformed = sql.replace("current_employee_masters", "fixture_employees")
            .replace("employee_masters_for_period(:monthStart, :monthEnd)", "(select * from fixture_employees)")
            .replaceAll("employee_masters_for_period\\(c\\.contract_start_date,\\s*coalesce\\(c\\.contract_end_date, greatest\\(c\\.contract_start_date, current_date\\)\\)\\)",
                "(select * from fixture_employees)");
        return real.query(fixture + transformed, params, mapper);
      }
    };
    return new SalesController(template);
  }

  private String mixedContracts() {
    return """
        values (1,'0001','individual','外部会社',460000::numeric,400000::numeric,date '2025-10-15',date '2025-10-20'),
               (2,'0001','bp','外部会社',480000,400000,date '2025-10-01',date '2025-11-30'),
               (3,'0001','regular','外部会社',900000,400000,date '2025-10-01',null),
               (4,'0001','bp','株式会社CMCソフト',900000,400000,date '2025-10-01',null),
               (5,'0001','bp','外部会社',900000,400000,date '2025-09-01',date '2025-09-30'),
               (6,'0001','bp','外部会社',900000,400000,date '2026-10-01',null),
               (7,'0099','bp','外部会社',900000,400000,date '2025-10-01',null)
        """;
  }

  private void assertMoney(String expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual), "amount=" + actual);
  }

  @ParameterizedTest
  @CsvSource({"-1,0", "0,0", "30000,0", "30000.01,10000", "60000,10000",
      "63333.33,10000", "63333.50,10000.1", "100000,21000"})
  void appliesMinimumPerContractOnlyWhenProfitExceedsThirtyThousand(String profit, String expected) {
    String contracts = "values (1,'0001','bp','external'," + new BigDecimal("400000").add(new BigDecimal(profit))
        + "::numeric,400000::numeric,date '2025-10-01',null::date)";
    List<AllowanceAnalysisDetail> details = controller(contracts).allowanceAnalysisDetails("0001", "2025-10");
    assertEquals(1, details.size());
    assertMoney(expected, details.get(0).allowanceAmount());
  }

  @Test void fixesTheTwelveMonthReportingPeriod() {
    AllowanceAnalysisResponse response = controller(mixedContracts()).allowanceAnalysis();
    assertEquals("2025-10", response.fiscalStartMonth());
    assertEquals("2026-09", response.fiscalEndMonth());
    assertEquals(12, response.months().size());
    assertEquals("2025-10", response.months().get(0));
    assertEquals("2026-09", response.months().get(11));
  }

  @Test void includesIndividualsAndBpAndCountsPartialMonthsInFull() {
    SalesController controller = controller(mixedContracts());
    AllowanceAnalysisItem row = controller.allowanceAnalysis().rows().stream()
        .filter(item -> item.employeeNo().equals("0001")).findFirst().orElseThrow();
    assertMoney("25000", row.monthlyAmounts().get(0).amount());
    assertMoney("15000", row.monthlyAmounts().get(1).amount());
    assertMoney("40000", row.subtotal());
    List<AllowanceAnalysisDetail> details = controller.allowanceAnalysisDetails("0001", "2025-10");
    assertEquals(List.of(1, 2), details.stream().map(AllowanceAnalysisDetail::contractNo).toList());
    assertMoney("25000", details.stream().map(AllowanceAnalysisDetail::allowanceAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2025-09", "2026-10"})
  void returnsNoSalesAllowanceDetailsOutsideTheFixedPeriod(String month) {
    assertTrue(controller(mixedContracts()).allowanceAnalysisDetails("0001", month).isEmpty());
  }

  @Test void fiscalProfitUsesTheSameRulesAndRestrictsSalesAllowancesToTheFixedPeriod() throws Exception {
    var method = SalesController.class.getDeclaredMethod("calculateFiscalAllowanceAmounts", YearMonth.class, YearMonth.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<YearMonth, BigDecimal> amounts = (Map<YearMonth, BigDecimal>) method.invoke(
        controller(mixedContracts()), YearMonth.of(2025, 9), YearMonth.of(2026, 10));
    assertMoney("0", amounts.get(YearMonth.of(2025, 9)));
    assertMoney("31912", amounts.get(YearMonth.of(2025, 10))); // sales 25,000 + recruitment 1,234 + specific 5,678
    assertMoney("15000", amounts.get(YearMonth.of(2025, 11)));
    assertMoney("0", amounts.get(YearMonth.of(2026, 10)));
  }

  @Test void returnsRecruitmentAndSpecificAllowancesAsSeparateMonthlyValues() throws Exception {
    org.springframework.test.web.servlet.MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
        .standaloneSetup(controller(mixedContracts())).build();
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/sales/allowance-analysis"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.recruitmentAmounts['0001']['2025-10']").value(1234))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.specificAmounts['0001']['2025-10']").value(5678));
  }

  @ParameterizedTest
  @CsvSource({"60000,0", "60000.01,10000", "90000,10000", "100000,12000", "130000,21000"})
  void deductsThirtyThousandFromAccentureProfitBeforeApplyingTheAllowanceThreshold(String profit, String expected) {
    String contracts = "values (1,'0001','bp','external'," + new BigDecimal("400000").add(new BigDecimal(profit))
        + "::numeric,400000::numeric,date '2025-10-01',date '2025-10-31')";
    List<AllowanceAnalysisDetail> details = controller(contracts, "アクセンチュア株式会社")
        .allowanceAnalysisDetails("0001", "2025-10");
    assertMoney(expected, details.get(0).allowanceAmount());
  }

  @Test void accentureAdjustmentIsSharedByListDetailsAndFiscalProfit() throws Exception {
    String contracts = "values (1,'0001','individual','external',500000::numeric,400000::numeric,date '2025-10-01',date '2025-10-31')";
    SalesController controller = controller(contracts, "アクセンチュア株式会社");
    AllowanceAnalysisItem row = controller.allowanceAnalysis().rows().stream()
        .filter(item -> item.employeeNo().equals("0001")).findFirst().orElseThrow();
    assertMoney("12000", row.monthlyAmounts().getFirst().amount());
    assertMoney("12000", controller.allowanceAnalysisDetails("0001", "2025-10").getFirst().allowanceAmount());
    var method = SalesController.class.getDeclaredMethod("calculateFiscalAllowanceAmounts", YearMonth.class, YearMonth.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<YearMonth, BigDecimal> amounts = (Map<YearMonth, BigDecimal>) method.invoke(
        controller, YearMonth.of(2025, 10), YearMonth.of(2025, 10));
    assertMoney("18912", amounts.get(YearMonth.of(2025, 10)));
  }

  @Test void detailsExposeTheCustomerAndNegativeProfitAdjustment() throws Exception {
    String contracts = "values (1,'0001','bp','external',500000::numeric,400000::numeric,date '2025-10-01',date '2025-10-31')";
    var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
        .standaloneSetup(controller(contracts, "アクセンチュア株式会社")).build();
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/api/sales/allowance-analysis/details").param("employeeNo", "0001").param("month", "2025-10"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].customerName").value("アクセンチュア株式会社"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].profitAdjustment").value(-30000));
  }
}
