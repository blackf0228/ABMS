package com.aba.abms.contract;

import java.util.List;

public record ContractPageResponse(
    List<ContractListItem> items,
    long totalElements,
    int page,
    int size,
    int totalPages
) {
}
