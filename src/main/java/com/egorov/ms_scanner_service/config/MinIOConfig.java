package com.egorov.ms_scanner_service.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinIOConfig {

  private String url;
  private String accessKey;
  private String secretKey;
  private String bucketName;

  @Bean
  public MinioClient minioClient() {
    return MinioClient.builder()
                      .endpoint(url)
                      .credentials(accessKey, secretKey)
                      .build();
  }
}