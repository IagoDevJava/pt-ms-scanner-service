package com.egorov.ms_scanner_service.config;

import com.egorov.ms_scanner_service.model.ScanResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CacheConfig {

  @Bean
  public Cache<UUID, ScanResult> scanResultCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(10000)
        .recordStats()
        .removalListener(
            (key, value, cause) -> log.debug(
                "Removed {} from cache due to {}", key, cause)
        )
        .build();
  }
}