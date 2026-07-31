package com.aba.abms.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "employee_resident_tax_details")
@IdClass(EmployeePaymentId.class)
public class EmployeeResidentTaxDetail {

  @Id
  @Column(name = "employee_no", length = 20)
  private String employeeNo;

  @Id
  @Column(name = "payment_month", length = 7)
  private String paymentMonth;

  @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal paymentAmount;

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

  public BigDecimal getPaymentAmount() {
    return paymentAmount;
  }

  public void setPaymentAmount(BigDecimal paymentAmount) {
    this.paymentAmount = paymentAmount;
  }
}
