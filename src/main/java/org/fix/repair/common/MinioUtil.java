package org.fix.repair.common;



import org.fix.repair.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
@Component
public class MinioUtil {

    private final MinioProperties minioProperties;


    private MinioClient getClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpointUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecreKey())
                .build();
    }

    /**
     * 上传文件
     *
     * @param inputStream 文件流
     * @param fileName    文件名
     * @param contentType 文件类型
     * @return 文件访问路径
     */
    public String uploadFile(InputStream inputStream, String fileName, String contentType) {
        try {
            MinioClient client = getClient();

            // 上传文件到指定存储桶
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .stream(inputStream, -1, 10485760) // unknown size, 10MB part size
                            .contentType(contentType)
                            .build()
            );

            // 返回文件完整访问路径
            return minioProperties.getEndpointUrl() + "/" + minioProperties.getBucketName() + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     *
     * @param objectName 文件路径
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) {
        try {
            MinioClient client = getClient();

            // 下载文件
            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     *
     * @param objectName 文件路径
     */
    public void deleteFile(String objectName) {
        try {
            MinioClient client = getClient();

            // 删除文件
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }


}