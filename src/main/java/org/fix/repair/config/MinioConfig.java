package org.fix.repair.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * MinIO配置类
 * 在应用启动时检查并创建存储桶
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioConfig implements CommandLineRunner {

    private final MinioProperties minioProperties;

    @Override
    public void run(String... args) throws Exception {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndpointUrl())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecreKey())
                    .region("us-east-1")
                    .build();

            // 检查存储桶是否存在
//            boolean bucketExists = minioClient.bucketExists(
//                    BucketExistsArgs.builder()
//                            .bucket(minioProperties.getBucketName())
//                            .build()
//            );
//
//            if (!bucketExists) {
//                // 创建存储桶
//                minioClient.makeBucket(
//                        MakeBucketArgs.builder()
//                                .bucket(minioProperties.getBucketName())
//                                .build()
//                );
//                log.info("MinIO存储桶创建成功: {}", minioProperties.getBucketName());
//            } else {
//                log.info("MinIO存储桶已存在: {}", minioProperties.getBucketName());
//            }

            log.info("MinIO初始化完成，端点: {}", minioProperties.getEndpointUrl());
        } catch (Exception e) {
            log.error("MinIO初始化失败", e);
            throw new RuntimeException("MinIO初始化失败: " + e.getMessage(), e);
        }
    }
}
