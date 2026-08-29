package com.aba.abms.contract;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class ContractController {

  private static final int DEFAULT_PAGE_SIZE = 30;
  private static final String ENGINEER_DISPLAY_SQL = "coalesce(nullif(trim(concat_ws(' ', engineer.last_name, engineer.first_name)), ''), engineer.employee_name, c.engineer_name)";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ContractController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public ContractPageResponse search(
      @RequestParam(required = false) String engineerName,
      @RequestParam(required = false) String operationStatus,
      @RequestParam(required = false) String contractStartDateFrom,
      @RequestParam(required = false) String contractEndDateTo,
      @RequestParam(required = false) String effectiveMonthFrom,
      @RequestParam(required = false) String effectiveMonthTo,
      @RequestParam(required = false) String customerId,
      @RequestParam(required = false) String engineerCompanyId,
      @RequestParam(required = false) String projectAffiliation,
      @RequestParam(required = false) String engineerProposer,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    int pageSize = normalizeSize(size);
    int currentPage = Math.max(page, 0);
    int offset = currentPage * pageSize;

    StringBuilder fromWhere = new StringBuilder("""
        from contracts c
        left join customer_masters customer
          on customer.company_name = c.customer_name
        left join customer_masters engineer_company
          on engineer_company.company_name = c.engineer_company_name
        left join employee_masters engineer
          on engineer.employee_no = c.engineer_name
        where 1 = 1
        """);
    Map<String, Object> params = new HashMap<>();

    if (engineerName != null && !engineerName.isBlank()) {
      fromWhere.append(" and " + ENGINEER_DISPLAY_SQL + " like :engineerName ");
      params.put("engineerName", "%" + engineerName.trim() + "%");
    }
    if ("今期".equals(operationStatus)) {
      LocalDate currentTermStart = LocalDate.of(LocalDate.now().getYear() - 1, 10, 1);
      fromWhere.append("""
           and (
             c.contract_start_date >= :currentTermStart
             or (
               c.contract_start_date <= :currentTermStart
               and (c.contract_end_date is null or c.contract_end_date >= :currentTermStart)
             )
           )
          """);
      params.put("currentTermStart", currentTermStart);
    } else if (operationStatus != null && !operationStatus.isBlank()) {
      fromWhere.append(" and c.operation_status = :operationStatus ");
      params.put("operationStatus", operationStatus.trim());
    }
    if (contractStartDateFrom != null && !contractStartDateFrom.isBlank()) {
      fromWhere.append(" and c.contract_start_date >= cast(:contractStartDateFrom as date) ");
      params.put("contractStartDateFrom", contractStartDateFrom.trim());
    }
    if (contractEndDateTo != null && !contractEndDateTo.isBlank()) {
      fromWhere.append(" and c.contract_end_date <= cast(:contractEndDateTo as date) ");
      params.put("contractEndDateTo", contractEndDateTo.trim());
    }
    if (effectiveMonthFrom != null && !effectiveMonthFrom.isBlank()) {
      fromWhere.append(" and (c.contract_end_date is null or c.contract_end_date >= cast(:effectiveMonthFrom || '-01' as date)) ");
      params.put("effectiveMonthFrom", effectiveMonthFrom.trim());
    }
    if (effectiveMonthTo != null && !effectiveMonthTo.isBlank()) {
      fromWhere.append(" and c.contract_start_date <= (cast(:effectiveMonthTo || '-01' as date) + interval '1 month - 1 day') ");
      params.put("effectiveMonthTo", effectiveMonthTo.trim());
    }
    if (customerId != null && !customerId.isBlank()) {
      fromWhere.append(" and customer.company_id = :customerId ");
      params.put("customerId", customerId.trim());
    }
    if (engineerCompanyId != null && !engineerCompanyId.isBlank()) {
      fromWhere.append(" and engineer_company.company_id = :engineerCompanyId ");
      params.put("engineerCompanyId", engineerCompanyId.trim());
    }
    if (projectAffiliation != null && !projectAffiliation.isBlank()) {
      fromWhere.append(" and c.project_affiliation = :projectAffiliation ");
      params.put("projectAffiliation", projectAffiliation.trim());
    }
    if (engineerProposer != null && !engineerProposer.isBlank()) {
      fromWhere.append(" and c.engineer_proposer = :engineerProposer ");
      params.put("engineerProposer", engineerProposer.trim());
    }

    long totalElements = jdbcTemplate.queryForObject(
        "select count(*) " + fromWhere,
        params,
        Long.class);

    params.put("limit", pageSize);
    params.put("offset", offset);
    String sql = "select c.contract_no, c.engineer_type, "
        + ENGINEER_DISPLAY_SQL + " as engineer_name, c.contract_amount, c.customer_name, "
        + "c.engineer_unit_price, c.engineer_company_name, c.operation_status, "
        + "c.contract_start_date, c.contract_end_date, c.project_affiliation, c.engineer_proposer, "
        + "c.settlement_lower_hour, c.settlement_upper_hour "
        + fromWhere
        + " order by " + ENGINEER_DISPLAY_SQL
        + " , c.operation_status, c.contract_start_date, c.contract_no limit :limit offset :offset";

    List<ContractListItem> items = jdbcTemplate.query(sql, params, new ContractListItemMapper());
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);

    return new ContractPageResponse(
        items,
        totalElements,
        currentPage,
        pageSize,
        totalPages);
  }

  @PostMapping
  public ContractListItem create(@RequestBody ContractListItem request) {
    Integer contractNo = jdbcTemplate.queryForObject(
        "select coalesce(max(contract_no), 0) + 1 from contracts",
        Map.of(),
        Integer.class);
    Map<String, Object> params = contractParams(contractNo, request);
    params.put("engineerName", resolveEngineerEmployeeNo(request.engineerName()));

    jdbcTemplate.update("""
        insert into contracts (
          contract_no,
          engineer_type,
          engineer_name,
          contract_amount,
          customer_name,
          contract_start_date,
          contract_end_date,
          engineer_unit_price,
          engineer_company_name,
          operation_status,
          project_affiliation,
          engineer_proposer,
          settlement_lower_hour,
          settlement_upper_hour
        ) values (
          :contractNo,
          :engineerType,
          :engineerName,
          :contractAmount,
          :customerName,
          cast(:contractStartDate as date),
          cast(:contractEndDate as date),
          :engineerUnitPrice,
          :engineerCompanyName,
          :operationStatus,
          :projectAffiliation,
          :engineerProposer,
          :settlementLowerHour,
          :settlementUpperHour
        )
        """, params);

    return findByContractNo(contractNo);
  }

  @PutMapping("/{contractNo}")
  public ContractListItem update(
      @PathVariable Integer contractNo,
      @RequestBody ContractListItem request) {
    Map<String, Object> params = contractParams(contractNo, request);

    jdbcTemplate.update("""
        update contracts
        set
          engineer_type = :engineerType,
          contract_amount = :contractAmount,
          customer_name = :customerName,
          contract_start_date = cast(:contractStartDate as date),
          contract_end_date = cast(:contractEndDate as date),
          engineer_unit_price = :engineerUnitPrice,
          engineer_company_name = :engineerCompanyName,
          operation_status = :operationStatus,
          project_affiliation = :projectAffiliation,
          engineer_proposer = :engineerProposer,
          settlement_lower_hour = :settlementLowerHour,
          settlement_upper_hour = :settlementUpperHour
        where contract_no = :contractNo
        """, params);

    return findByContractNo(contractNo);
  }

  @DeleteMapping("/{contractNo}")
  public void delete(@PathVariable Integer contractNo) {
    jdbcTemplate.update(
        "delete from contracts where contract_no = :contractNo",
        Map.of("contractNo", contractNo));
  }

  private static Map<String, Object> contractParams(Integer contractNo, ContractListItem request) {
    Map<String, Object> params = new HashMap<>();
    params.put("contractNo", contractNo);
    params.put("engineerType", blankToNull(request.engineerType()));
    params.put("contractAmount", request.contractAmount());
    params.put("customerName", blankToNull(request.customerName()));
    params.put("contractStartDate", request.contractStartDate());
    params.put("contractEndDate", request.contractEndDate());
    params.put("engineerUnitPrice", request.engineerUnitPrice());
    params.put("engineerCompanyName", blankToNull(request.engineerCompanyName()));
    params.put("operationStatus", blankToNull(request.operationStatus()));
    params.put("projectAffiliation", emptyIfNull(request.projectAffiliation()));
    params.put("engineerProposer", emptyIfNull(request.engineerProposer()));
    params.put("settlementLowerHour", request.settlementLowerHour());
    params.put("settlementUpperHour", request.settlementUpperHour());
    return params;
  }

  private ContractListItem findByContractNo(Integer contractNo) {
    String sql = """
        select
          c.contract_no,
          c.engineer_type,
          coalesce(nullif(trim(concat_ws(' ', engineer.last_name, engineer.first_name)), ''), engineer.employee_name, c.engineer_name) as engineer_name,
          c.contract_amount,
          c.customer_name,
          c.engineer_unit_price,
          c.engineer_company_name,
          c.operation_status,
          c.contract_start_date,
          c.contract_end_date,
          c.project_affiliation,
          c.engineer_proposer,
          c.settlement_lower_hour,
          c.settlement_upper_hour
        from contracts c
        left join employee_masters engineer
          on engineer.employee_no = c.engineer_name
        where c.contract_no = :contractNo
        """;
    return jdbcTemplate.queryForObject(sql, Map.of("contractNo", contractNo), new ContractListItemMapper());
  }

  private String resolveEngineerEmployeeNo(String value) {
    String normalized = blankToNull(value);
    if (normalized == null) {
      return null;
    }
    Integer exists = jdbcTemplate.queryForObject(
        "select count(*) from employee_masters where employee_no = :employeeNo",
        Map.of("employeeNo", normalized), Integer.class);
    if (exists != null && exists > 0) {
      return normalized;
    }
    return jdbcTemplate.queryForObject("""
        select employee_no
        from employee_masters
        where replace(replace(trim(coalesce(last_name, '') || ' ' || coalesce(first_name, '')), ' ', ''), '　', '')
            = replace(replace(trim(:engineerName), ' ', ''), '　', '')
        order by case when deletion_flag = '0' then 0 else 1 end, employee_no
        limit 1
        """, Map.of("engineerName", normalized), String.class);
  }

  private static int normalizeSize(int size) {
    if (size <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(size, DEFAULT_PAGE_SIZE);
  }

  private static LocalDate date(ResultSet rs, String name) throws SQLException {
    java.sql.Date date = rs.getDate(name);
    return date == null ? null : date.toLocalDate();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String emptyIfNull(String value) {
    return value == null ? "" : value.trim();
  }

  private static class ContractListItemMapper implements RowMapper<ContractListItem> {

    @Override
    public ContractListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ContractListItem(
          rs.getInt("contract_no"),
          rs.getString("engineer_type"),
          rs.getString("engineer_name"),
          rs.getBigDecimal("contract_amount"),
          rs.getString("customer_name"),
          rs.getBigDecimal("engineer_unit_price"),
          rs.getString("engineer_company_name"),
          rs.getString("operation_status"),
          date(rs, "contract_start_date"),
          date(rs, "contract_end_date"),
          rs.getString("project_affiliation"),
          rs.getString("engineer_proposer"),
          (Integer) rs.getObject("settlement_lower_hour"),
          (Integer) rs.getObject("settlement_upper_hour"));
    }
  }
}
