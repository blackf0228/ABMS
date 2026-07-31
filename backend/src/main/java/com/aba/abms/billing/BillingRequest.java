package com.aba.abms.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "billing_requests")
public class BillingRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "no")
  private Long no;

  @Column(name = "request_source", nullable = false, length = 100)
  private String requestSource;

  @Column(name = "request_destination", nullable = false, length = 100)
  private String requestDestination;

  @Column(name = "request_month", nullable = false)
  private LocalDate requestMonth;

  @Column(name = "engineer", nullable = false, length = 100)
  private String engineer;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(name = "remarks", columnDefinition = "text")
  private String remarks;

  public Long getNo() {
    return no;
  }

  public String getRequestSource() {
    return requestSource;
  }

  public void setRequestSource(String requestSource) {
    this.requestSource = requestSource;
  }

  public String getRequestDestination() {
    return requestDestination;
  }

  public void setRequestDestination(String requestDestination) {
    this.requestDestination = requestDestination;
  }

  public LocalDate getRequestMonth() {
    return requestMonth;
  }

  public void setRequestMonth(LocalDate requestMonth) {
    this.requestMonth = requestMonth;
  }

  public String getEngineer() {
    return engineer;
  }

  public void setEngineer(String engineer) {
    this.engineer = engineer;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
