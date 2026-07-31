package com.aba.abms.contract;

public record InvoiceDocumentResponse(
    String fileName,
    String filePath,
    int detailCount
) {
}
