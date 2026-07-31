package com.aba.abms.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "monthly_salary_calculation_conditions")
@IdClass(EmployeePaymentId.class)
public class MonthlySalaryCalculationCondition {

  @Id
  @Column(name = "employee_no", length = 20)
  private String employeeNo;

  @Id
  @Column(name = "payment_month", length = 7)
  private String paymentMonth;

  @Column(name = "commuting_allowance", precision = 15, scale = 2)
  private BigDecimal commutingAllowance;

  @Column(name = "management_allowance", precision = 15, scale = 2)
  private BigDecimal managementAllowance;

  @Column(name = "transportation_adjustment", precision = 15, scale = 2)
  private BigDecimal transportationAdjustment;

  @Column(name = "overtime_pay", precision = 15, scale = 2)
  private BigDecimal overtimePay;

  @Column(name = "advance_expense", precision = 15, scale = 2)
  private BigDecimal advanceExpense;

  @Column(name = "advance_expense2", precision = 15, scale = 2)
  private BigDecimal advanceExpense2;

  @Column(name = "other_amount", precision = 15, scale = 2)
  private BigDecimal otherAmount;

  @Column(name = "other_item_name", length = 100)
  private String otherItemName;

  public String getEmployeeNo() {
    return employeeNo;
  }

  public void setEmployeeNo(String employeeNo) {
    this.employeeNo = employeeNo;
  }

  public String getPaymentMonth() {
    return paymentMonth;
  }

  public void setPaymentMonth(String paymentMonth) {
    this.paymentMonth = paymentMonth;
  }

  public BigDecimal getCommutingAllowance() {
    return commutingAllowance;
  }

  public void setCommutingAllowance(BigDecimal commutingAllowance) {
    this.commutingAllowance = commutingAllowance;
  }

  public BigDecimal getManagementAllowance() {
    return managementAllowance;
  }

  public void setManagementAllowance(BigDecimal managementAllowance) {
    this.managementAllowance = managementAllowance;
  }

  public BigDecimal getTransportationAdjustment() {
    return transportationAdjustment;
  }

  public void setTransportationAdjustment(BigDecimal transportationAdjustment) {
    this.transportationAdjustment = transportationAdjustment;
  }

  public BigDecimal getOvertimePay() {
    return overtimePay;
  }

  public void setOvertimePay(BigDecimal overtimePay) {
    this.overtimePay = overtimePay;
  }

  public BigDecimal getAdvanceExpense() {
    return advanceExpense;
  }

  public void setAdvanceExpense(BigDecimal advanceExpense) {
    this.advanceExpense = advanceExpense;
  }

  public BigDecimal getAdvanceExpense2() {
    return advanceExpense2;
  }

  public void setAdvanceExpense2(BigDecimal advanceExpense2) {
    this.advanceExpense2 = advanceExpense2;
  }

  public BigDecimal getOtherAmount() {
    return otherAmount;
  }

  public void setOtherAmount(BigDecimal otherAmount) {
    this.otherAmount = otherAmount;
  }

  public String getOtherItemName() {
    return otherItemName;
  }

  public void setOtherItemName(String otherItemName) {
    this.otherItemName = otherItemName;
  }
}
