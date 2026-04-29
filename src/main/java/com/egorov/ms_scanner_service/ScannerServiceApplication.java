package com.egorov.ms_scanner_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients
@EnableConfigurationProperties
public class ScannerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ScannerServiceApplication.class, args);
  }
}
