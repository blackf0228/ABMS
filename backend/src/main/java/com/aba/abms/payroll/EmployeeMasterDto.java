package com.aba.abms.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeMasterDto(
    String employeeNo,
    String employeeName,
    String lastName,
    String firstName,
    String employeeType,
    BigDecimal baseSalary,
    Integer dependentCount,
    Integer deductionCount,
    BigDecimal consumptionTaxRate,
    String deletionFlag,
    LocalDate effectiveStartDate,
    LocalDate effectiveEndDate
) {

  public static EmployeeMasterDto fromEntity(EmployeeMaster entity) {
    return new EmployeeMasterDto(
        entity.getEmployeeNo(),
        entity.getEmployeeName(),
        entity.getLastName(),
        entity.getFirstName(),
        entity.getEmployeeType(),
        entity.getBaseSalary(),
        entity.getDependentCount(),
        entity.getDeductionCount(),
        entity.getConsumptionTaxRate(),
        entity.getDeletionFlag(),
        entity.getEffectiveStartDate(),
        entity.getEffectiveEndDate());
  }
}
