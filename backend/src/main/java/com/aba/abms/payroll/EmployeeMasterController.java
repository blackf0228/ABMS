package com.aba.abms.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class EmployeeMasterController {

  private static final Pattern NAME_SEPARATOR = Pattern.compile("[\\s　]+");
  private static final Pattern BP_EMPLOYEE_NO = Pattern.compile("^B(\\d+)$");

  private final EmployeeMasterRepository repository;

  public EmployeeMasterController(EmployeeMasterRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<EmployeeMasterDto> list() {
    return repository.findCurrent(LocalDate.now(ZoneId.of("Asia/Tokyo")))
        .stream()
        .map(EmployeeMasterDto::fromEntity)
        .toList();
  }

  @GetMapping("/{employeeNo}/history")
  public List<EmployeeMasterDto> history(@PathVariable String employeeNo) {
    return repository.findByEmployeeNoOrderByEffectiveStartDateDesc(employeeNo).stream()
        .map(EmployeeMasterDto::fromEntity).toList();
  }

  @GetMapping("/next-number")
  public String nextNumber(@RequestParam(defaultValue = "0") String employeeType) {
    if ("2".equals(trim(employeeType))) {
      return nextBpEmployeeNumber();
    }
    int maxEmployeeNo = repository.findAll().stream()
        .map(EmployeeMaster::getEmployeeNo)
        .filter(value -> value != null && value.matches("\\d+"))
        .mapToInt(Integer::parseInt)
        .max()
        .orElse(0);
    return String.format("%04d", maxEmployeeNo + 1);
  }

  @PostMapping
  @Transactional
  public synchronized EmployeeMasterDto create(@RequestBody EmployeeMasterDto request) {
    String employeeType = trim(request.employeeType());
    String employeeNo = "2".equals(employeeType)
        ? nextBpEmployeeNumber()
        : trim(request.employeeNo());
    if (employeeNo.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee number is required.");
    }
    repository.lockEmployee(employeeNo);
    if (repository.existsByEmployeeNo(employeeNo)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number already exists.");
    }

    EmployeeMaster employee = new EmployeeMaster();
    employee.setEmployeeNo(employeeNo);
    employee.setEffectiveStartDate(requiredStartDate(request));
    employee.setEffectiveEndDate(LocalDate.of(9999, 12, 31));
    applyName(employee, request);
    employee.setEmployeeType(employeeType);
    employee.setBaseSalary(request.baseSalary());
    employee.setDependentCount("2".equals(employeeType) ? 0 : request.dependentCount());
    employee.setDeductionCount("2".equals(employeeType) ? 0 : request.deductionCount());
    employee.setConsumptionTaxRate("2".equals(employeeType)
        ? BigDecimal.ZERO : request.consumptionTaxRate());
    employee.setDeletionFlag("0");
    return EmployeeMasterDto.fromEntity(repository.save(employee));
  }

  @PutMapping("/{employeeNo}")
  @Transactional
  public EmployeeMasterDto update(
      @PathVariable String employeeNo,
      @RequestBody EmployeeMasterDto request) {
    LocalDate start = requiredStartDate(request);
    repository.lockEmployee(employeeNo);
    List<EmployeeMaster> versions = repository.findByEmployeeNoOrderByEffectiveStartDateDesc(employeeNo);
    if (versions.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "社員情報がありません。");
    }
    EmployeeMaster previous = versions.get(0);
    if (!start.isAfter(previous.getEffectiveStartDate())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "効力発生日は最新履歴の効力発生日より後の日付を指定してください。");
    }
    EmployeeMaster employee = new EmployeeMaster();
    employee.setEmployeeNo(employeeNo);
    employee.setEffectiveStartDate(start);
    employee.setEffectiveEndDate(LocalDate.of(9999, 12, 31));

    applyName(employee, request);
    employee.setEmployeeType(request.employeeType());
    employee.setBaseSalary(request.baseSalary());
    employee.setDependentCount("2".equals(request.employeeType()) ? 0 : request.dependentCount());
    employee.setDeductionCount("2".equals(request.employeeType()) ? 0 : request.deductionCount());
    employee.setConsumptionTaxRate("2".equals(request.employeeType()) ? BigDecimal.ZERO : request.consumptionTaxRate());
    employee.setDeletionFlag("1".equals(request.deletionFlag()) ? "1" : "0");

    previous.setEffectiveEndDate(start.minusDays(1));
    repository.saveAndFlush(previous);
    return EmployeeMasterDto.fromEntity(repository.save(employee));
  }

  private LocalDate requiredStartDate(EmployeeMasterDto request) {
    LocalDate date = request.effectiveStartDate();
    if (date == null || date.isBefore(LocalDate.of(1900, 1, 1)) || date.isAfter(LocalDate.of(9999, 12, 31))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "効力発生日を1900/01/01〜9999/12/31で指定してください。");
    }
    return date;
  }

  private void applyName(EmployeeMaster employee, EmployeeMasterDto request) {
    String lastName = trim(request.lastName());
    String firstName = trim(request.firstName());
    if (lastName.isBlank() || firstName.isBlank()) {
      String legacyName = trim(request.employeeName());
      String[] parts = NAME_SEPARATOR.split(legacyName, 2);
      if (lastName.isBlank() && parts.length > 0) {
        lastName = parts[0];
      }
      if (firstName.isBlank() && parts.length > 1) {
        firstName = parts[1];
      }
    }
    if (lastName.isBlank() || firstName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Last name and first name are required.");
    }
    employee.setLastName(lastName);
    employee.setFirstName(firstName);
    employee.setEmployeeName(lastName + "　" + firstName);
  }

  private String nextBpEmployeeNumber() {
    int maxSequence = repository.findAll().stream()
        .filter(employee -> "2".equals(trim(employee.getEmployeeType())))
        .map(EmployeeMaster::getEmployeeNo)
        .filter(value -> value != null)
        .map(BP_EMPLOYEE_NO::matcher)
        .filter(Matcher::matches)
        .mapToInt(matcher -> parseSequence(matcher.group(1)))
        .max()
        .orElse(0);
    return String.format(Locale.ROOT, "B%03d", maxSequence + 1);
  }

  private int parseSequence(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return 0;
    }
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
