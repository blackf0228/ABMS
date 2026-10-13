package com.aba.abms.payroll;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class EmployeeMasterId implements Serializable {
  private String employeeNo;
  private LocalDate effectiveStartDate;

  public EmployeeMasterId() {}
  public EmployeeMasterId(String employeeNo, LocalDate effectiveStartDate) {
    this.employeeNo = employeeNo;
    this.effectiveStartDate = effectiveStartDate;
  }
  @Override public boolean equals(Object other) {
    return other instanceof EmployeeMasterId id
        && Objects.equals(employeeNo, id.employeeNo)
        && Objects.equals(effectiveStartDate, id.effectiveStartDate);
  }
  @Override public int hashCode() { return Objects.hash(employeeNo, effectiveStartDate); }
}
