package com.aba.abms.payroll;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @PutMapping("/{employeeNo}")
  public EmployeeMasterDto update(
      @PathVariable String employeeNo,
      @RequestBody EmployeeMasterDto request) {
    EmployeeMaster employee = repository.findById(employeeNo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee was not found."));

    employee.setEmployeeName(request.employeeName());
    employee.setEmployeeType(request.employeeType());
    employee.setBaseSalary(request.baseSalary());
    employee.setDependentCount(request.dependentCount());
    employee.setDeductionCount(request.deductionCount());
    employee.setConsumptionTaxRate(request.consumptionTaxRate());

    return EmployeeMasterDto.fromEntity(repository.save(employee));
  }
}
