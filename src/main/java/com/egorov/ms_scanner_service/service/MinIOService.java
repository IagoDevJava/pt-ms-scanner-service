package com.egorov.ms_scanner_service.service;

import com.egorov.ms_scanner_service.config.MinIOConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOService {

  private final MinioClient minioClient;
  private final MinIOConfig minIOConfig;

  /**
   * Загружает изображение в MinIO и возвращает публичный URL.
   *
   * @param imageBase64 изображение в формате base64 (без префикса data:image/...)
   * @param taskId      идентификатор задачи, используется для имени файла
   * @return URL загруженного изображения
   */
  public String uploadImage(String imageBase64, UUID taskId) {
    try {
      boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                                                                      .bucket(
                                                                          minIOConfig.getBucketName())
                                                                      .build());
      if (!bucketExists) {
        minioClient.makeBucket(MakeBucketArgs.builder()
                                             .bucket(minIOConfig.getBucketName())
                                             .build());
        log.info("Created bucket: {}", minIOConfig.getBucketName());
      }

      String objectName = taskId + ".jpg";

      byte[] imageBytes = java.util.Base64.getDecoder().decode(imageBase64);

      try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
        minioClient.putObject(PutObjectArgs.builder()
                                           .bucket(minIOConfig.getBucketName())
                                           .object(objectName)
                                           .stream(inputStream, imageBytes.length, -1)
                                           .contentType("image/jpeg")
                                           .build());
      }

      String imageUrl = String.format(
          "%s/%s/%s",
          minIOConfig.getUrl(),
          minIOConfig.getBucketName(),
          objectName
      );
      log.info("Image uploaded successfully: {}", imageUrl);
      return imageUrl;
    } catch (Exception e) {
      log.error("Failed to upload image to MinIO for taskId: {}", taskId, e);
      throw new RuntimeException("Failed to upload image to MinIO", e);
    }
  }
}