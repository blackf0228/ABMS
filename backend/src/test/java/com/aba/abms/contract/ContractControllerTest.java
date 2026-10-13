package com.aba.abms.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ContractControllerTest {

  private static final String OTHER_THAN_CMC = "__CMC_SOFT_OTHER__";
  private static final String CMC_SOFT = "株式会社CMCソフト";

  private NamedParameterJdbcTemplate jdbcTemplate;
  private ContractController controller;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    controller = new ContractController(jdbcTemplate);
    when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.query(anyString(), any(Map.class), any(RowMapper.class))).thenReturn(List.of());
  }

  @Test
  void filtersCustomersOtherThanCmcSoft() {
    controller.search(null, null, null, null, OTHER_THAN_CMC, null, null, null, 0, 30);

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, ?>> parameters = mapCaptor();
    verify(jdbcTemplate).queryForObject(sql.capture(), parameters.capture(), eq(Long.class));

    assertThat(sql.getValue()).contains("c.customer_name IS DISTINCT FROM :excludedCustomerName");
    assertThat(parameters.getValue().get("excludedCustomerName")).isEqualTo(CMC_SOFT);
  }

  @Test
  void filtersEngineerCompaniesOtherThanCmcSoft() {
    controller.search(null, null, null, null, null, OTHER_THAN_CMC, null, null, 0, 30);

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, ?>> parameters = mapCaptor();
    verify(jdbcTemplate).queryForObject(sql.capture(), parameters.capture(), eq(Long.class));

    assertThat(sql.getValue()).contains("c.engineer_company_name IS DISTINCT FROM :excludedEngineerCompanyName");
    assertThat(parameters.getValue().get("excludedEngineerCompanyName")).isEqualTo(CMC_SOFT);
  }

  @Test
  void resolvesEngineerNameWithoutInvalidDistinctOrdering() {
    when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(Integer.class)))
        .thenAnswer(invocation -> invocation.<String>getArgument(0).contains("max(contract_no)") ? 99 : 0);
    when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(String.class)))
        .thenReturn("B058");

    controller.create(new ContractListItem(
        0, "1", "劉 林溪", null, null, null, null, null,
        null, null, null, null, null, null));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).queryForObject(sql.capture(), any(Map.class), eq(String.class));

    assertThat(sql.getValue()).contains("group by employee_no");
    assertThat(sql.getValue()).contains("order by min(case when deletion_flag = '0'");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static ArgumentCaptor<Map<String, ?>> mapCaptor() {
    return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
  }
}
