package com.aba.abms.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "employee_masters")
public class EmployeeMaster {

  @Id
  @Column(name = "employee_no", length = 20)
  private String employeeNo;

  @Column(name = "employee_name", nullable = false, length = 100)
  private String employeeName;

  @Column(name = "last_name", nullable = false, length = 50)
  private String lastName;

  @Column(name = "first_name", nullable = false, length = 50)
  private String firstName;

  @Column(name = "employee_type", nullable = false, length = 50)
  private String employeeType;

  @Column(name = "base_salary", nullable = false, precision = 15, scale = 2)
  private BigDecimal baseSalary;

  @Column(name = "dependent_count", nullable = false)
  private Integer dependentCount;

  @Column(name = "deduction_count", nullable = false)
  private Integer deductionCount;

  @Column(name = "consumption_tax_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal consumptionTaxRate = BigDecimal.ZERO;

  @Column(name = "deletion_flag", nullable = false, length = 1)
  private String deletionFlag = "0";

  public String getEmployeeNo() {
    return employeeNo;
  }

  public void setEmployeeNo(String employeeNo) {
    this.employeeNo = employeeNo;
  }

  public String getEmployeeName() {
    return employeeName;
  }

  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getEmployeeType() {
    return employeeType;
  }

  public void setEmployeeType(String employeeType) {
    this.employeeType = employeeType;
  }

  public BigDecimal getBaseSalary() {
    return baseSalary;
  }

  public void setBaseSalary(BigDecimal baseSalary) {
    this.baseSalary = baseSalary;
  }

  public Integer getDependentCount() {
    return dependentCount;
  }

  public void setDependentCount(Integer dependentCount) {
    this.dependentCount = dependentCount;
  }

  public Integer getDeductionCount() {
    return deductionCount;
  }

  public void setDeductionCount(Integer deductionCount) {
    this.deductionCount = deductionCount;
  }

  public BigDecimal getConsumptionTaxRate() {
    return consumptionTaxRate;
  }

  public void setConsumptionTaxRate(BigDecimal consumptionTaxRate) {
    this.consumptionTaxRate = consumptionTaxRate;
  }

  public String getDeletionFlag() {
    return deletionFlag;
  }

  public void setDeletionFlag(String deletionFlag) {
    this.deletionFlag = deletionFlag;
  }
}
