package com.aba.abms.payroll;

import java.io.Serializable;
import java.util.Objects;

public class EmployeePaymentId implements Serializable {

  private String employeeNo;
  private String paymentMonth;

  public EmployeePaymentId() {
  }

  public EmployeePaymentId(String employeeNo, String paymentMonth) {
    this.employeeNo = employeeNo;
    this.paymentMonth = paymentMonth;
  }

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

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof EmployeePaymentId that)) {
      return false;
    }
    return Objects.equals(employeeNo, that.employeeNo)
        && Objects.equals(paymentMonth, that.paymentMonth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeNo, paymentMonth);
  }
}
