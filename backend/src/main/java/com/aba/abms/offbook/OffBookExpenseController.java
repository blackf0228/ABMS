package com.aba.abms.offbook;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/off-book-expenses")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class OffBookExpenseController {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public OffBookExpenseController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public List<OffBookExpenseItem> search(
      @RequestParam(required = false) String receivedDateFrom,
      @RequestParam(required = false) String receivedDateTo,
      @RequestParam(required = false) String handlerEmployeeNo) {
    StringBuilder sql = new StringBuilder("""
        select
          o.expense_no,
          o.received_date,
          o.handler_employee_no,
          coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, o.handler_employee_no) as handler_name,
          o.amount,
          o.currency,
          o.reason
        from off_book_expenses o
        left join employee_masters e on e.employee_no = o.handler_employee_no
        where 1 = 1
        """);
    Map<String, Object> params = new HashMap<>();
    appendDateCondition(sql, params, receivedDateFrom, "receivedDateFrom", ">=");
    appendDateCondition(sql, params, receivedDateTo, "receivedDateTo", "<=");
    if (handlerEmployeeNo != null && !handlerEmployeeNo.isBlank()) {
      sql.append(" and o.handler_employee_no = :handlerEmployeeNo ");
      params.put("handlerEmployeeNo", handlerEmployeeNo.trim());
    }
    sql.append(" order by o.received_date desc, o.expense_no desc");
    return jdbcTemplate.query(sql.toString(), params, new OffBookExpenseMapper());
  }

  @PostMapping
  public OffBookExpenseItem create(@RequestBody OffBookExpenseRequest request) {
    validate(request);
    Integer expenseNo = jdbcTemplate.queryForObject(
        "select coalesce(max(expense_no), 0) + 1 from off_book_expenses",
        Map.of(),
        Integer.class);
    jdbcTemplate.update("""
        insert into off_book_expenses (
          expense_no, received_date, handler_employee_no, amount, currency, reason
        ) values (
          :expenseNo, :receivedDate, :handlerEmployeeNo, :amount, :currency, :reason
        )
        """, params(expenseNo, request));
    return findByExpenseNo(expenseNo);
  }

  @PutMapping("/{expenseNo}")
  public OffBookExpenseItem update(
      @PathVariable Integer expenseNo,
      @RequestBody OffBookExpenseRequest request) {
    validate(request);
    int updated = jdbcTemplate.update("""
        update off_book_expenses
        set received_date = :receivedDate,
            handler_employee_no = :handlerEmployeeNo,
            amount = :amount,
            currency = :currency,
            reason = :reason
        where expense_no = :expenseNo
        """, params(expenseNo, request));
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "勘定外費用が見つかりません。");
    }
    return findByExpenseNo(expenseNo);
  }

  private OffBookExpenseItem findByExpenseNo(Integer expenseNo) {
    return jdbcTemplate.queryForObject("""
        select
          o.expense_no,
          o.received_date,
          o.handler_employee_no,
          coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, o.handler_employee_no) as handler_name,
          o.amount,
          o.currency,
          o.reason
        from off_book_expenses o
        left join employee_masters e on e.employee_no = o.handler_employee_no
        where o.expense_no = :expenseNo
        """, Map.of("expenseNo", expenseNo), new OffBookExpenseMapper());
  }

  private Map<String, Object> params(Integer expenseNo, OffBookExpenseRequest request) {
    Map<String, Object> params = new HashMap<>();
    params.put("expenseNo", expenseNo);
    params.put("receivedDate", request.receivedDate());
    params.put("handlerEmployeeNo", request.handlerEmployeeNo().trim());
    params.put("amount", request.amount());
    params.put("currency", request.currency().trim());
    params.put("reason", request.reason().trim());
    return params;
  }

  private void validate(OffBookExpenseRequest request) {
    if (request == null || request.receivedDate() == null
        || request.handlerEmployeeNo() == null || request.handlerEmployeeNo().isBlank()
        || request.amount() == null || request.currency() == null || request.currency().isBlank()
        || request.reason() == null || request.reason().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必須項目を入力してください。");
    }
    Integer employeeCount = jdbcTemplate.queryForObject(
        "select count(*) from employee_masters where employee_no = :employeeNo and employee_type = '0'",
        Map.of("employeeNo", request.handlerEmployeeNo().trim()),
        Integer.class);
    if (employeeCount == null || employeeCount == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "預かり担当には正社員を指定してください。");
    }
  }

  private static void appendDateCondition(
      StringBuilder sql,
      Map<String, Object> params,
      String value,
      String parameterName,
      String operator) {
    if (value == null || value.isBlank()) {
      return;
    }
    try {
      LocalDate date = LocalDate.parse(value.trim());
      sql.append(" and o.received_date ").append(operator).append(" :").append(parameterName).append(" ");
      params.put(parameterName, date);
    } catch (java.time.format.DateTimeParseException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日付の形式が正しくありません。");
    }
  }

  private static class OffBookExpenseMapper implements RowMapper<OffBookExpenseItem> {
    @Override
    public OffBookExpenseItem mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
      return new OffBookExpenseItem(
          rs.getInt("expense_no"),
          rs.getDate("received_date").toLocalDate(),
          rs.getString("handler_employee_no"),
          rs.getString("handler_name"),
          rs.getBigDecimal("amount"),
          rs.getString("currency"),
          rs.getString("reason"));
    }
  }
}
