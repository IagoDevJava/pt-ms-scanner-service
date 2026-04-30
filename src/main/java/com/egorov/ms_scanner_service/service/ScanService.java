package com.egorov.ms_scanner_service.service;

import com.egorov.ms_scanner_service.model.BarcodeScanRequest;
import com.egorov.ms_scanner_service.model.BarcodeScanResponse;
import com.egorov.ms_scanner_service.model.ExternalComplexScanRequest;
import com.egorov.ms_scanner_service.model.ScanResult;

public interface ScanService {

  BarcodeScanResponse scanBarcode(BarcodeScanRequest request);

  ScanResult complexScan(ExternalComplexScanRequest request);
}