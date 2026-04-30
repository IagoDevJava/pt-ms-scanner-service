package com.egorov.ms_scanner_service.mapper;

import com.egorov.ms_scanner_service.model.ExternalComplexScanRequest;
import com.egorov.ms_scanner_service.model.InternalComplexScanRequest;

public class ComplexScanRequestMapper {

  public static InternalComplexScanRequest toInternalRequest(
      ExternalComplexScanRequest request,
      String imageUrl
  ) {
    return new InternalComplexScanRequest(
        request.taskId(),
        imageUrl,
        request.scanType(),
        request.userId(),
        null,
        request.timestamp()
    );
  }


}
