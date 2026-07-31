package com.aba.abms.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractListItem(
    Integer contractNo,
    String engineerType,
    String engineerName,
    BigDecimal contractAmount,
    String customerName,
    BigDecimal engineerUnitPrice,
    String engineerCompanyName,
    String operationStatus,
    LocalDate contractStartDate,
    LocalDate contractEndDate,
    String projectAffiliation,
    String engineerProposer,
    Integer settlementLowerHour,
    Integer settlementUpperHour
) {
}
