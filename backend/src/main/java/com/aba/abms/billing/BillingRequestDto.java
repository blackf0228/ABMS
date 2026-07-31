package com.aba.abms.billing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingRequestDto(
    Long no,
    @NotBlank @Size(max = 100) String requestSource,
    @NotBlank @Size(max = 100) String requestDestination,
    @NotNull LocalDate requestMonth,
    @NotBlank @Size(max = 100) String engineer,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @Size(max = 2000) String remarks
) {

  static BillingRequestDto fromEntity(BillingRequest entity) {
    return new BillingRequestDto(
        entity.getNo(),
        entity.getRequestSource(),
        entity.getRequestDestination(),
        entity.getRequestMonth(),
        entity.getEngineer(),
        entity.getAmount(),
        entity.getRemarks());
  }

  BillingRequest toEntity() {
    BillingRequest entity = new BillingRequest();
    entity.setRequestSource(requestSource);
    entity.setRequestDestination(requestDestination);
    entity.setRequestMonth(requestMonth);
    entity.setEngineer(engineer);
    entity.setAmount(amount);
    entity.setRemarks(remarks);
    return entity;
  }
}
