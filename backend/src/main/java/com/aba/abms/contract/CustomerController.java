package com.aba.abms.contract;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class CustomerController {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public CustomerController(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public List<CustomerOption> list() {
    String sql = """
        select
          company_id,
          company_name
        from customer_masters
        order by company_id
        """;
    return jdbcTemplate.query(sql, new CustomerOptionMapper());
  }

  @PostMapping
  public CustomerOption create(@RequestBody CustomerOption request) {
    String companyId = value(request.companyId());
    String companyName = value(request.companyName());
    validate(companyId, companyName);
    if (exists(companyId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Company ID already exists.");
    }

    jdbcTemplate.update("""
        insert into customer_masters (company_id, company_name)
        values (:companyId, :companyName)
        """, Map.of("companyId", companyId, "companyName", companyName));
    return new CustomerOption(companyId, companyName);
  }

  @PutMapping("/{companyId}")
  public CustomerOption update(
      @PathVariable String companyId,
      @RequestBody CustomerOption request) {
    String normalizedId = value(companyId);
    String companyName = value(request.companyName());
    validate(normalizedId, companyName);
    if (!exists(normalizedId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company was not found.");
    }

    jdbcTemplate.update("""
        update customer_masters
        set company_name = :companyName
        where company_id = :companyId
        """, Map.of("companyId", normalizedId, "companyName", companyName));
    return new CustomerOption(normalizedId, companyName);
  }

  private boolean exists(String companyId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from customer_masters where company_id = :companyId",
        Map.of("companyId", companyId), Integer.class);
    return count != null && count > 0;
  }

  private static void validate(String companyId, String companyName) {
    if (companyId.isBlank() || companyName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company ID and company name are required.");
    }
  }

  private static String value(String value) {
    return value == null ? "" : value.trim();
  }

  private static class CustomerOptionMapper implements RowMapper<CustomerOption> {

    @Override
    public CustomerOption mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new CustomerOption(
          rs.getString("company_id"),
          rs.getString("company_name"));
    }
  }
}
