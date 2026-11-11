package com.aba.abms.sales;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** The real save SQL writes only to a connection-local temporary table. */
class AllowanceManualSaveTest {
  private Connection connection;
  private JdbcTemplate jdbc;
  private MockMvc mvc;

  @BeforeEach void createTemporaryTable() throws Exception {
    DriverManagerDataSource source = new DriverManagerDataSource();
    source.setUrl(System.getProperty("profit.test.jdbcUrl", "jdbc:postgresql://localhost:5432/abms"));
    source.setUsername(System.getenv().getOrDefault("ABMS_DB_USER", "abms"));
    source.setPassword(System.getenv().getOrDefault("ABMS_DB_PASSWORD", "abms_password"));
    connection = source.getConnection();
    SingleConnectionDataSource temporarySource = new SingleConnectionDataSource(connection, true);
    jdbc = new JdbcTemplate(temporarySource);
    jdbc.execute("""
        CREATE TEMP TABLE employee_allowances (
          employee_no varchar(20) NOT NULL, payment_month varchar(7) NOT NULL,
          recruitment_allowance numeric(15,2) NOT NULL DEFAULT 0,
          specific_allowance numeric(15,2) NOT NULL DEFAULT 0,
          PRIMARY KEY (employee_no,payment_month)
        )
        """);
    assertEquals("t", jdbc.queryForObject(
        "select relpersistence::text from pg_class where oid='employee_allowances'::regclass", String.class));
    mvc = MockMvcBuilders.standaloneSetup(new SalesController(new NamedParameterJdbcTemplate(temporarySource))).build();
  }

  @AfterEach void closeConnection() throws Exception {
    if (connection != null) connection.close();
  }

  private void save(String json) throws Exception {
    mvc.perform(put("/api/sales/allowance-analysis/manual").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk());
  }

  @Test void savesBothManualAllowancesAndUpdatesOnlyRequestedMonths() throws Exception {
    save("""
        {"rows":[{"employeeNo":"0001","amounts":[
          {"paymentMonth":"2025-10","recruitmentAllowance":1000,"specificAllowance":2500},
          {"paymentMonth":"2025-11","recruitmentAllowance":2000,"specificAllowance":4000}]}]}
        """);
    save("""
        {"rows":[{"employeeNo":"0001","amounts":[
          {"paymentMonth":"2025-10","recruitmentAllowance":3000,"specificAllowance":4500}]}]}
        """);
    assertEquals(2, jdbc.queryForObject("select count(*) from employee_allowances", Integer.class));
    assertEquals(7500, jdbc.queryForObject("select recruitment_allowance+specific_allowance from employee_allowances where payment_month='2025-10'", Integer.class));
    assertEquals(6000, jdbc.queryForObject("select recruitment_allowance+specific_allowance from employee_allowances where payment_month='2025-11'", Integer.class));
  }

  @Test void recruitmentOnlyEndpointPreservesSpecificAllowance() throws Exception {
    jdbc.update("insert into employee_allowances values ('0001','2025-10',1000,2500)");
    mvc.perform(put("/api/sales/allowance-analysis/recruitment").contentType(MediaType.APPLICATION_JSON).content("""
        {"rows":[{"employeeNo":"0001","amounts":[{"paymentMonth":"2025-10","amount":3000}]}]}
        """))
        .andExpect(status().isOk());
    assertEquals(3000, jdbc.queryForObject("select recruitment_allowance from employee_allowances", Integer.class));
    assertEquals(2500, jdbc.queryForObject("select specific_allowance from employee_allowances", Integer.class));
  }

  @Test void malformedLaterMonthDoesNotPartiallySaveEarlierAmounts() throws Exception {
    mvc.perform(put("/api/sales/allowance-analysis/manual").contentType(MediaType.APPLICATION_JSON).content("""
        {"rows":[{"employeeNo":"0001","amounts":[
          {"paymentMonth":"2025-10","recruitmentAllowance":1000,"specificAllowance":2500},
          {"paymentMonth":"2025-13","recruitmentAllowance":2000,"specificAllowance":4000}]}]}
        """))
        .andExpect(status().isBadRequest());
    assertEquals(0, jdbc.queryForObject("select count(*) from employee_allowances", Integer.class));
  }

  @Test void rejectsEmployeesOutsideTheAllowanceRecipients() throws Exception {
    mvc.perform(put("/api/sales/allowance-analysis/manual").contentType(MediaType.APPLICATION_JSON).content("""
        {"rows":[{"employeeNo":"0030","amounts":[
          {"paymentMonth":"2025-10","recruitmentAllowance":1000,"specificAllowance":2500}]}]}
        """))
        .andExpect(status().isBadRequest());
    assertEquals(0, jdbc.queryForObject("select count(*) from employee_allowances", Integer.class));
  }
}
