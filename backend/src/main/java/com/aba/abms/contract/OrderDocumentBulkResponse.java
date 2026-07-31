package com.aba.abms.contract;

import java.util.List;

public record OrderDocumentBulkResponse(
    int createdCount,
    List<String> fileNames,
    String outputDirectory
) {
}
