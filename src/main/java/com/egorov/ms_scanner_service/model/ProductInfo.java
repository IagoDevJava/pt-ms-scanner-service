package com.egorov.ms_scanner_service.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record ProductInfo(
    Long id,
    Long categoryId,
    String name,
    String description,
    String barcode,
    Double calories,
    Double proteins,
    Double fats,
    Double carbs,
    Integer glycemicIndex,
    Boolean isVegan,
    String storageCondition,
    BigDecimal minTemperature,
    BigDecimal maxTemperature,
    Integer maxHumidity,
    Integer shelfLifeDays,
    Boolean requiresDarkness,
    String externalId,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {

}