package com.aba.abms.payroll;

import java.math.BigDecimal;

public record EmployeeMasterDto(
    String employeeNo,
    String employeeName,
    String employeeType,
    BigDecimal baseSalary,
    Integer dependentCount,
    Integer deductionCount,
    BigDecimal consumptionTaxRate
) {

  public static EmployeeMasterDto fromEntity(EmployeeMaster entity) {
    return new EmployeeMasterDto(
        entity.getEmployeeNo(),
        entity.getEmployeeName(),
        entity.getEmployeeType(),
        entity.getBaseSalary(),
        entity.getDependentCount(),
        entity.getDeductionCount(),
        entity.getConsumptionTaxRate());
  }
}
