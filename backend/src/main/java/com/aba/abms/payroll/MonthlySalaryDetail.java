package com.aba.abms.payroll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "monthly_salary_details")
@IdClass(EmployeePaymentId.class)
public class MonthlySalaryDetail {

  @Id
  @Column(name = "employee_no", length = 20)
  private String employeeNo;

  @Id
  @Column(name = "payment_month", length = 7)
  private String paymentMonth;

  @Column(name = "employment_insurance_amount", precision = 15, scale = 2)
  private BigDecimal employmentInsuranceAmount;

  @Column(name = "pension_insurance_amount", precision = 15, scale = 2)
  private BigDecimal pensionInsuranceAmount;

  @Column(name = "health_insurance_amount", precision = 15, scale = 2)
  private BigDecimal healthInsuranceAmount;

  @Column(name = "income_tax", precision = 15, scale = 2)
  private BigDecimal incomeTax;

  @Column(name = "child_support_amount", precision = 15, scale = 2)
  private BigDecimal childSupportAmount;

  @Column(name = "company_child_support_amount", precision = 15, scale = 2)
  private BigDecimal companyChildSupportAmount;

  @Column(name = "taxable_income", precision = 15, scale = 2)
  private BigDecimal taxableIncome;

  @Column(name = "transfer_payment_amount", precision = 15, scale = 2)
  private BigDecimal transferPaymentAmount;

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

  public BigDecimal getEmploymentInsuranceAmount() {
    return employmentInsuranceAmount;
  }

  public void setEmploymentInsuranceAmount(BigDecimal employmentInsuranceAmount) {
    this.employmentInsuranceAmount = employmentInsuranceAmount;
  }

  public BigDecimal getPensionInsuranceAmount() {
    return pensionInsuranceAmount;
  }

  public void setPensionInsuranceAmount(BigDecimal pensionInsuranceAmount) {
    this.pensionInsuranceAmount = pensionInsuranceAmount;
  }

  public BigDecimal getHealthInsuranceAmount() {
    return healthInsuranceAmount;
  }

  public void setHealthInsuranceAmount(BigDecimal healthInsuranceAmount) {
    this.healthInsuranceAmount = healthInsuranceAmount;
  }

  public BigDecimal getIncomeTax() {
    return incomeTax;
  }

  public void setIncomeTax(BigDecimal incomeTax) {
    this.incomeTax = incomeTax;
  }

  public BigDecimal getChildSupportAmount() {
    return childSupportAmount;
  }

  public void setChildSupportAmount(BigDecimal childSupportAmount) {
    this.childSupportAmount = childSupportAmount;
  }

  public BigDecimal getCompanyChildSupportAmount() {
    return companyChildSupportAmount;
  }

  public void setCompanyChildSupportAmount(BigDecimal companyChildSupportAmount) {
    this.companyChildSupportAmount = companyChildSupportAmount;
  }

  public BigDecimal getTaxableIncome() {
    return taxableIncome;
  }

  public void setTaxableIncome(BigDecimal taxableIncome) {
    this.taxableIncome = taxableIncome;
  }

  public BigDecimal getTransferPaymentAmount() {
    return transferPaymentAmount;
  }

  public void setTransferPaymentAmount(BigDecimal transferPaymentAmount) {
    this.transferPaymentAmount = transferPaymentAmount;
  }
}
