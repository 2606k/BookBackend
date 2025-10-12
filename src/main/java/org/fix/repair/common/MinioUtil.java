package org.fix.repair.common;

import org.fix.repair.config.MinioProperties;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
     * 上传MultipartFile文件
     *
     * @param file MultipartFile文件
     * @return 文件访问路径
     */
    public String uploadFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new RuntimeException("文件名不能为空");
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = generateUniqueFileName(fileExtension);

            // 上传文件
            return uploadFile(file.getInputStream(), fileName, file.getContentType());

        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到指定目录
     *
     * @param file     MultipartFile文件
     * @param folder   文件夹路径
     * @return 文件访问路径
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new RuntimeException("文件名不能为空");
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = folder + "/" + generateUniqueFileName(fileExtension);

            // 上传文件
            return uploadFile(file.getInputStream(), fileName, file.getContentType());

        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到bucket根目录
     *
     * @param file MultipartFile文件
     * @return 文件访问路径
     */
    public String uploadFileToRoot(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new RuntimeException("文件名不能为空");
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = generateUniqueFileName(fileExtension);

            // 上传文件到bucket根目录
            return uploadFile(file.getInputStream(), fileName, file.getContentType());

        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成唯一文件名
     *
     * @param fileExtension 文件扩展名
     * @return 唯一文件名
     */
    private String generateUniqueFileName(String fileExtension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return timestamp + "_" + uuid + fileExtension;
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

    /**
     * 从完整URL中提取对象名
     *
     * @param fileUrl 文件完整URL
     * @return 对象名
     */
    public String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        
        String bucketPrefix = minioProperties.getEndpointUrl() + "/" + minioProperties.getBucketName() + "/";
        if (fileUrl.startsWith(bucketPrefix)) {
            return fileUrl.substring(bucketPrefix.length());
        }
        
        // 如果URL不包含完整路径，直接返回
        return fileUrl;
    }

    /**
     * 检查文件是否存在
     *
     * @param objectName 文件路径
     * @return 是否存在
     */
    public boolean fileExists(String objectName) {
        try {
            MinioClient client = getClient();
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件信息
     *
     * @param objectName 文件路径
     * @return 文件信息
     */
    public StatObjectResponse getFileInfo(String objectName) {
        try {
            MinioClient client = getClient();
            return client.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取文件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证文件类型
     *
     * @param fileName 文件名
     * @param allowedTypes 允许的文件类型
     * @return 是否允许
     */
    public boolean validateFileType(String fileName, String[] allowedTypes) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        for (String allowedType : allowedTypes) {
            if (fileExtension.equals("." + allowedType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证图片文件类型
     *
     * @param fileName 文件名
     * @return 是否为图片
     */
    public boolean isImageFile(String fileName) {
        String[] imageTypes = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
        return validateFileType(fileName, imageTypes);
    }
}