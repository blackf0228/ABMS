package com.aba.abms.contract;

import java.util.List;

public record InvoiceDocumentBulkResponse(
    int createdCount,
    List<String> fileNames,
    String outputDirectory) {
}
