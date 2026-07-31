package com.aba.abms.insurance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "social_insurance_premium_amounts")
public class SocialInsurancePremiumAmount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "no")
  private Long no;

  @Column(name = "effective_month", nullable = false, length = 7)
  private String effectiveMonth;

  @Column(name = "payment_month", nullable = false, length = 7)
  private String paymentMonth;

  @Column(name = "prefecture", nullable = false, length = 20)
  private String prefecture;

  @Column(name = "grade", nullable = false)
  private Integer grade;

  @Column(name = "pension_grade")
  private Integer pensionGrade;

  @Column(name = "standard_monthly_remuneration", nullable = false)
  private Integer standardMonthlyRemuneration;

  @Column(name = "monthly_remuneration_from")
  private Integer monthlyRemunerationFrom;

  @Column(name = "monthly_remuneration_to")
  private Integer monthlyRemunerationTo;

  @Column(name = "health_insurance_rate", precision = 7, scale = 4)
  private BigDecimal healthInsuranceRate;

  @Column(name = "health_insurance_total_amount", precision = 15, scale = 2)
  private BigDecimal healthInsuranceTotalAmount;

  @Column(name = "health_insurance_employee_amount", precision = 15, scale = 2)
  private BigDecimal healthInsuranceEmployeeAmount;

  @Column(name = "health_insurance_employer_amount", precision = 15, scale = 2)
  private BigDecimal healthInsuranceEmployerAmount;

  @Column(name = "care_insurance_rate", precision = 7, scale = 4)
  private BigDecimal careInsuranceRate;

  @Column(name = "care_insurance_total_amount", precision = 15, scale = 2)
  private BigDecimal careInsuranceTotalAmount;

  @Column(name = "care_insurance_employee_amount", precision = 15, scale = 2)
  private BigDecimal careInsuranceEmployeeAmount;

  @Column(name = "care_insurance_employer_amount", precision = 15, scale = 2)
  private BigDecimal careInsuranceEmployerAmount;

  @Column(name = "child_support_rate", precision = 7, scale = 4)
  private BigDecimal childSupportRate;

  @Column(name = "child_support_total_amount", precision = 15, scale = 2)
  private BigDecimal childSupportTotalAmount;

  @Column(name = "child_support_employee_amount", precision = 15, scale = 2)
  private BigDecimal childSupportEmployeeAmount;

  @Column(name = "child_support_employer_amount", precision = 15, scale = 2)
  private BigDecimal childSupportEmployerAmount;

  @Column(name = "pension_insurance_rate", precision = 7, scale = 4)
  private BigDecimal pensionInsuranceRate;

  @Column(name = "pension_insurance_total_amount", precision = 15, scale = 2)
  private BigDecimal pensionInsuranceTotalAmount;

  @Column(name = "pension_insurance_employee_amount", precision = 15, scale = 2)
  private BigDecimal pensionInsuranceEmployeeAmount;

  @Column(name = "pension_insurance_employer_amount", precision = 15, scale = 2)
  private BigDecimal pensionInsuranceEmployerAmount;

  @Column(name = "source_name", length = 200)
  private String sourceName;

  @Column(name = "source_date")
  private LocalDate sourceDate;

  @Column(name = "remarks", columnDefinition = "text")
  private String remarks;

  public Long getNo() {
    return no;
  }

  public String getEffectiveMonth() {
    return effectiveMonth;
  }

  public void setEffectiveMonth(String effectiveMonth) {
    this.effectiveMonth = effectiveMonth;
  }

  public String getPaymentMonth() {
    return paymentMonth;
  }

  public void setPaymentMonth(String paymentMonth) {
    this.paymentMonth = paymentMonth;
  }

  public String getPrefecture() {
    return prefecture;
  }

  public void setPrefecture(String prefecture) {
    this.prefecture = prefecture;
  }

  public Integer getGrade() {
    return grade;
  }

  public void setGrade(Integer grade) {
    this.grade = grade;
  }

  public Integer getPensionGrade() {
    return pensionGrade;
  }

  public void setPensionGrade(Integer pensionGrade) {
    this.pensionGrade = pensionGrade;
  }

  public Integer getStandardMonthlyRemuneration() {
    return standardMonthlyRemuneration;
  }

  public void setStandardMonthlyRemuneration(Integer standardMonthlyRemuneration) {
    this.standardMonthlyRemuneration = standardMonthlyRemuneration;
  }

  public Integer getMonthlyRemunerationFrom() {
    return monthlyRemunerationFrom;
  }

  public void setMonthlyRemunerationFrom(Integer monthlyRemunerationFrom) {
    this.monthlyRemunerationFrom = monthlyRemunerationFrom;
  }

  public Integer getMonthlyRemunerationTo() {
    return monthlyRemunerationTo;
  }

  public void setMonthlyRemunerationTo(Integer monthlyRemunerationTo) {
    this.monthlyRemunerationTo = monthlyRemunerationTo;
  }

  public BigDecimal getHealthInsuranceRate() {
    return healthInsuranceRate;
  }

  public void setHealthInsuranceRate(BigDecimal healthInsuranceRate) {
    this.healthInsuranceRate = healthInsuranceRate;
  }

  public BigDecimal getHealthInsuranceTotalAmount() {
    return healthInsuranceTotalAmount;
  }

  public void setHealthInsuranceTotalAmount(BigDecimal healthInsuranceTotalAmount) {
    this.healthInsuranceTotalAmount = healthInsuranceTotalAmount;
  }

  public BigDecimal getHealthInsuranceEmployeeAmount() {
    return healthInsuranceEmployeeAmount;
  }

  public void setHealthInsuranceEmployeeAmount(BigDecimal healthInsuranceEmployeeAmount) {
    this.healthInsuranceEmployeeAmount = healthInsuranceEmployeeAmount;
  }

  public BigDecimal getHealthInsuranceEmployerAmount() {
    return healthInsuranceEmployerAmount;
  }

  public void setHealthInsuranceEmployerAmount(BigDecimal healthInsuranceEmployerAmount) {
    this.healthInsuranceEmployerAmount = healthInsuranceEmployerAmount;
  }

  public BigDecimal getCareInsuranceRate() {
    return careInsuranceRate;
  }

  public void setCareInsuranceRate(BigDecimal careInsuranceRate) {
    this.careInsuranceRate = careInsuranceRate;
  }

  public BigDecimal getCareInsuranceTotalAmount() {
    return careInsuranceTotalAmount;
  }

  public void setCareInsuranceTotalAmount(BigDecimal careInsuranceTotalAmount) {
    this.careInsuranceTotalAmount = careInsuranceTotalAmount;
  }

  public BigDecimal getCareInsuranceEmployeeAmount() {
    return careInsuranceEmployeeAmount;
  }

  public void setCareInsuranceEmployeeAmount(BigDecimal careInsuranceEmployeeAmount) {
    this.careInsuranceEmployeeAmount = careInsuranceEmployeeAmount;
  }

  public BigDecimal getCareInsuranceEmployerAmount() {
    return careInsuranceEmployerAmount;
  }

  public void setCareInsuranceEmployerAmount(BigDecimal careInsuranceEmployerAmount) {
    this.careInsuranceEmployerAmount = careInsuranceEmployerAmount;
  }

  public BigDecimal getChildSupportRate() {
    return childSupportRate;
  }

  public void setChildSupportRate(BigDecimal childSupportRate) {
    this.childSupportRate = childSupportRate;
  }

  public BigDecimal getChildSupportTotalAmount() {
    return childSupportTotalAmount;
  }

  public void setChildSupportTotalAmount(BigDecimal childSupportTotalAmount) {
    this.childSupportTotalAmount = childSupportTotalAmount;
  }

  public BigDecimal getChildSupportEmployeeAmount() {
    return childSupportEmployeeAmount;
  }

  public void setChildSupportEmployeeAmount(BigDecimal childSupportEmployeeAmount) {
    this.childSupportEmployeeAmount = childSupportEmployeeAmount;
  }

  public BigDecimal getChildSupportEmployerAmount() {
    return childSupportEmployerAmount;
  }

  public void setChildSupportEmployerAmount(BigDecimal childSupportEmployerAmount) {
    this.childSupportEmployerAmount = childSupportEmployerAmount;
  }

  public BigDecimal getPensionInsuranceRate() {
    return pensionInsuranceRate;
  }

  public void setPensionInsuranceRate(BigDecimal pensionInsuranceRate) {
    this.pensionInsuranceRate = pensionInsuranceRate;
  }

  public BigDecimal getPensionInsuranceTotalAmount() {
    return pensionInsuranceTotalAmount;
  }

  public void setPensionInsuranceTotalAmount(BigDecimal pensionInsuranceTotalAmount) {
    this.pensionInsuranceTotalAmount = pensionInsuranceTotalAmount;
  }

  public BigDecimal getPensionInsuranceEmployeeAmount() {
    return pensionInsuranceEmployeeAmount;
  }

  public void setPensionInsuranceEmployeeAmount(BigDecimal pensionInsuranceEmployeeAmount) {
    this.pensionInsuranceEmployeeAmount = pensionInsuranceEmployeeAmount;
  }

  public BigDecimal getPensionInsuranceEmployerAmount() {
    return pensionInsuranceEmployerAmount;
  }

  public void setPensionInsuranceEmployerAmount(BigDecimal pensionInsuranceEmployerAmount) {
    this.pensionInsuranceEmployerAmount = pensionInsuranceEmployerAmount;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public LocalDate getSourceDate() {
    return sourceDate;
  }

  public void setSourceDate(LocalDate sourceDate) {
    this.sourceDate = sourceDate;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
