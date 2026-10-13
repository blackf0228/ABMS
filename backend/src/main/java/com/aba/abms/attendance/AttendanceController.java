package com.aba.abms.attendance;

import java.math.BigDecimal;
import java.time.YearMonth;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class AttendanceController {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public AttendanceController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public List<AttendanceItem> search(
      @RequestParam(required = false) String engineerName,
      @RequestParam(required = false) String workingMonth) {
    StringBuilder sql = new StringBuilder("""
        select
          a.contract_no,
          coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, a.engineer_name) as engineer_name,
          a.working_month,
          c.engineer_company_name,
          c.customer_name,
          a.working_hours,
          a.remarks
        from attendance_hours a
        left join contracts c on c.contract_no = a.contract_no
        left join lateral employee_masters_for_month(a.working_month) e on e.employee_no = a.engineer_name
        where 1 = 1
        """);
    Map<String, Object> params = new HashMap<>();
    appendSearchCondition(sql, params, engineerName, workingMonth);
    sql.append(" order by c.customer_name, engineer_name, a.working_month, a.contract_no");
    return jdbcTemplate.query(sql.toString(), params, new AttendanceItemMapper());
  }

  @GetMapping("/active-contracts")
  public List<AttendanceItem> activeContracts(@RequestParam String workingMonth) {
    String month = normalizeMonth(workingMonth);
    YearMonth target = parseMonth(month);
    return jdbcTemplate.query("""
        select
          c.contract_no,
          coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, c.engineer_name) as engineer_name,
          :workingMonth as working_month,
          c.engineer_company_name,
          c.customer_name,
          a.working_hours,
          a.remarks
        from contracts c
        left join employee_masters_for_month(:workingMonth) e on e.employee_no = c.engineer_name
        left join attendance_hours a
          on a.contract_no = c.contract_no
         and a.engineer_name = c.engineer_name
         and a.working_month = :workingMonth
        where c.operation_status = '契約中'
          and c.contract_start_date <= :monthEnd
          and (c.contract_end_date is null or c.contract_end_date >= :monthStart)
        order by c.customer_name, engineer_name, c.contract_no
        """,
        Map.of(
            "workingMonth", month,
            "monthStart", target.atDay(1),
            "monthEnd", target.atEndOfMonth()),
        new AttendanceItemMapper());
  }

  @PostMapping("/bulk")
  @Transactional
  public void saveBulk(@RequestBody AttendanceBulkSaveRequest request) {
    String month = normalizeMonth(request.workingMonth());
    YearMonth target = parseMonth(month);
    List<AttendanceBulkRow> rows = request.rows() == null ? List.of() : request.rows();
    for (AttendanceBulkRow row : rows) {
      if (row.contractNo() == null || row.engineerName() == null || row.engineerName().isBlank()) {
        continue;
      }
      Map<String, Object> params = new HashMap<>();
      params.put("contractNo", row.contractNo());
      String engineerEmployeeNo = jdbcTemplate.queryForObject(
          "select engineer_name from contracts where contract_no = :contractNo",
          Map.of("contractNo", row.contractNo()), String.class);
      params.put("engineerName", engineerEmployeeNo);
      params.put("workingMonth", month);
      params.put("workingHours", row.workingHours() == null ? BigDecimal.ZERO : row.workingHours());
      params.put("remarks", row.remarks() == null ? "" : row.remarks().trim());

      jdbcTemplate.update("""
          delete from attendance_hours
          where contract_no = :contractNo
            and engineer_name = :engineerName
            and working_month = :workingMonth
          """, params);
      jdbcTemplate.update("""
          insert into attendance_hours (
            contract_no, engineer_name, working_month, working_hours, remarks
          ) values (
            :contractNo, :engineerName, :workingMonth, :workingHours, :remarks
          )
          """, params);
    }
  }

  @PutMapping("/{contractNo}")
  @Transactional
  public AttendanceItem update(
      @PathVariable Integer contractNo,
      @RequestBody AttendanceUpdateRequest request) {
    String month = normalizeMonth(request.workingMonth());
    String engineerName = jdbcTemplate.queryForObject(
        "select engineer_name from contracts where contract_no = :contractNo",
        Map.of("contractNo", contractNo),
        String.class);
    if (engineerName == null || engineerName.isBlank()) {
      throw new IllegalArgumentException("契約が見つかりません。");
    }
    Map<String, Object> params = new HashMap<>();
    params.put("contractNo", contractNo);
    params.put("engineerName", engineerName);
    params.put("workingMonth", month);
    params.put("workingHours", request.workingHours() == null ? BigDecimal.ZERO : request.workingHours());
    params.put("remarks", request.remarks() == null ? "" : request.remarks().trim());
    jdbcTemplate.update("""
        delete from attendance_hours
        where contract_no = :contractNo
          and engineer_name = :engineerName
          and working_month = :workingMonth
        """, params);
    jdbcTemplate.update("""
        insert into attendance_hours (
          contract_no, engineer_name, working_month, working_hours, remarks
        ) values (
          :contractNo, :engineerName, :workingMonth, :workingHours, :remarks
        )
        """, params);
    return jdbcTemplate.queryForObject("""
        select
          a.contract_no,
          coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, a.engineer_name) as engineer_name,
          a.working_month,
          c.engineer_company_name,
          c.customer_name,
          a.working_hours,
          a.remarks
        from attendance_hours a
        left join contracts c on c.contract_no = a.contract_no
        left join lateral employee_masters_for_month(a.working_month) e on e.employee_no = a.engineer_name
        where a.contract_no = :contractNo
          and a.engineer_name = :engineerName
          and a.working_month = :workingMonth
        """, params, new AttendanceItemMapper());
  }

  private static void appendSearchCondition(
      StringBuilder sql,
      Map<String, Object> params,
      String engineerName,
      String workingMonth) {
    if (engineerName != null && !engineerName.isBlank()) {
      sql.append(" and coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, a.engineer_name) like :engineerName");
      params.put("engineerName", "%" + engineerName.trim() + "%");
    }
    if (workingMonth != null && !workingMonth.isBlank()) {
      String month = normalizeMonth(workingMonth);
      sql.append(" and a.working_month = :workingMonth");
      params.put("workingMonth", month);
    }
  }

  private static String normalizeMonth(String value) {
    if (value == null) {
      throw new IllegalArgumentException("稼働年月は必須です。");
    }
    String month = value.replaceAll("[^0-9]", "");
    if (month.length() != 6) {
      throw new IllegalArgumentException("稼働年月はYYYYMMまたはYYYY-MMで指定してください。");
    }
    return month;
  }

  private static YearMonth parseMonth(String value) {
    int year = Integer.parseInt(value.substring(0, 4));
    int month = Integer.parseInt(value.substring(4, 6));
    return YearMonth.of(year, month);
  }

  private static class AttendanceItemMapper implements RowMapper<AttendanceItem> {
    @Override
    public AttendanceItem mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
      return new AttendanceItem(
          rs.getInt("contract_no"),
          rs.getString("engineer_name"),
          rs.getString("working_month"),
          rs.getString("engineer_company_name"),
          rs.getString("customer_name"),
          rs.getBigDecimal("working_hours"),
          rs.getString("remarks"));
    }
  }
}
