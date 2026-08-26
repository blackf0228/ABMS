package com.aba.abms.payroll;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
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

  private final EmployeeMasterRepository repository;

  public EmployeeMasterController(EmployeeMasterRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<EmployeeMasterDto> list() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "employeeNo"))
        .stream()
        .map(EmployeeMasterDto::fromEntity)
        .toList();
  }

  @GetMapping("/next-number")
  public String nextNumber() {
    int maxEmployeeNo = repository.findAll().stream()
        .map(EmployeeMaster::getEmployeeNo)
        .filter(value -> value != null && value.matches("\\d+"))
        .mapToInt(Integer::parseInt)
        .max()
        .orElse(0);
    return String.format("%04d", maxEmployeeNo + 1);
  }

  @PostMapping
  public EmployeeMasterDto create(@RequestBody EmployeeMasterDto request) {
    String employeeNo = request.employeeNo() == null ? "" : request.employeeNo().trim();
    if (employeeNo.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee number is required.");
    }
    if (repository.existsById(employeeNo)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number already exists.");
    }

    EmployeeMaster employee = new EmployeeMaster();
    employee.setEmployeeNo(employeeNo);
    applyName(employee, request);
    employee.setEmployeeType(request.employeeType());
    employee.setBaseSalary(request.baseSalary());
    employee.setDependentCount(request.dependentCount());
    employee.setDeductionCount(request.deductionCount());
    employee.setConsumptionTaxRate(request.consumptionTaxRate());
    employee.setDeletionFlag("0");
    return EmployeeMasterDto.fromEntity(repository.save(employee));
  }

  @PutMapping("/{employeeNo}")
  public EmployeeMasterDto update(
      @PathVariable String employeeNo,
      @RequestBody EmployeeMasterDto request) {
    EmployeeMaster employee = repository.findById(employeeNo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee was not found."));

    applyName(employee, request);
    employee.setEmployeeType(request.employeeType());
    employee.setBaseSalary(request.baseSalary());
    employee.setDependentCount(request.dependentCount());
    employee.setDeductionCount(request.deductionCount());
    employee.setConsumptionTaxRate(request.consumptionTaxRate());
    employee.setDeletionFlag("1".equals(request.deletionFlag()) ? "1" : "0");

    return EmployeeMasterDto.fromEntity(repository.save(employee));
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

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
