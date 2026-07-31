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
@Table(name = "employment_insurance_rates")
public class EmploymentInsuranceRate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "no")
  private Long no;

  @Column(name = "fiscal_year", nullable = false, length = 20)
  private String fiscalYear;

  @Column(name = "effective_start_date", nullable = false)
  private LocalDate effectiveStartDate;

  @Column(name = "effective_end_date", nullable = false)
  private LocalDate effectiveEndDate;

  @Column(name = "business_category", nullable = false, length = 100)
  private String businessCategory;

  @Column(name = "worker_rate", nullable = false, precision = 7, scale = 4)
  private BigDecimal workerRate;

  @Column(name = "employer_rate", nullable = false, precision = 7, scale = 4)
  private BigDecimal employerRate;

  @Column(name = "total_rate", nullable = false, precision = 7, scale = 4)
  private BigDecimal totalRate;

  @Column(name = "remarks", columnDefinition = "text")
  private String remarks;

  public Long getNo() {
    return no;
  }

  public String getFiscalYear() {
    return fiscalYear;
  }

  public void setFiscalYear(String fiscalYear) {
    this.fiscalYear = fiscalYear;
  }

  public LocalDate getEffectiveStartDate() {
    return effectiveStartDate;
  }

  public void setEffectiveStartDate(LocalDate effectiveStartDate) {
    this.effectiveStartDate = effectiveStartDate;
  }

  public LocalDate getEffectiveEndDate() {
    return effectiveEndDate;
  }

  public void setEffectiveEndDate(LocalDate effectiveEndDate) {
    this.effectiveEndDate = effectiveEndDate;
  }

  public String getBusinessCategory() {
    return businessCategory;
  }

  public void setBusinessCategory(String businessCategory) {
    this.businessCategory = businessCategory;
  }

  public BigDecimal getWorkerRate() {
    return workerRate;
  }

  public void setWorkerRate(BigDecimal workerRate) {
    this.workerRate = workerRate;
  }

  public BigDecimal getEmployerRate() {
    return employerRate;
  }

  public void setEmployerRate(BigDecimal employerRate) {
    this.employerRate = employerRate;
  }

  public BigDecimal getTotalRate() {
    return totalRate;
  }

  public void setTotalRate(BigDecimal totalRate) {
    this.totalRate = totalRate;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
