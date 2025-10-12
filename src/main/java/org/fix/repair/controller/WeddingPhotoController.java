package org.fix.repair.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.MinioUtil;
import org.fix.repair.common.R;
import org.fix.repair.entity.WeddingPhoto;
import org.fix.repair.service.WeddingPhotoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/wedding/photo")
@RequiredArgsConstructor
public class WeddingPhotoController {
    private final WeddingPhotoService photoService;
    private final MinioUtil minioUtil;

    /**
     * 上传照片
     */
    @PostMapping("/upload")
    public R<WeddingPhoto> uploadPhoto(
            @RequestParam MultipartFile file, 
            @RequestParam(required = false) Long storyId) {
        try {
            if (file.isEmpty()) {
                return R.error("上传文件为空");
            }

            // 验证文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return R.error("文件名不能为空");
            }

            if (!minioUtil.isImageFile(originalFilename)) {
                return R.error("只支持图片文件格式 (jpg, jpeg, png, gif, bmp, webp)");
            }

            // 上传文件到MinIO bucket根目录
            String fileUrl = minioUtil.uploadFileToRoot(file);

            // 保存照片记录
            WeddingPhoto photo = new WeddingPhoto();
            photo.setStoryId(storyId);
            photo.setUrl(fileUrl);
            photo.setRemark(originalFilename);
            photoService.save(photo);

            log.info("婚纱照片上传成功: {} -> {}", originalFilename, fileUrl);
            return R.ok(photo);
        } catch (Exception e) {
            log.error("婚纱照片上传失败", e);
            return R.error("照片上传失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有照片
     */
    @GetMapping("/list")
    public R<List<WeddingPhoto>> listPhoto() {
        try {
            List<WeddingPhoto> photos = photoService.list();
            log.info("查询所有婚纱照片，数量: {}", photos.size());
            return R.ok(photos);
        } catch (Exception e) {
            log.error("查询婚纱照片失败", e);
            return R.error("查询照片失败: " + e.getMessage());
        }
    }

    /**
     * 按故事ID查询照片
     */
    @GetMapping("/story/{storyId}")
    public R<List<WeddingPhoto>> listPhotoByStoryId(@PathVariable Long storyId) {
        try {
            List<WeddingPhoto> photos = photoService.lambdaQuery()
                    .eq(WeddingPhoto::getStoryId, storyId)
                    .list();
            log.info("查询故事{}的照片，数量: {}", storyId, photos.size());
            return R.ok(photos);
        } catch (Exception e) {
            log.error("查询故事照片失败", e);
            return R.error("查询照片失败: " + e.getMessage());
        }
    }

    /**
     * 删除照片
     */
    @DeleteMapping("/{id}")
    public R<String> deletePhoto(@PathVariable Long id) {
        try {
            // 先查询照片信息
            WeddingPhoto photo = photoService.getById(id);
            if (photo == null) {
                return R.error("照片不存在");
            }

            // 从MinIO删除文件
            if (photo.getUrl() != null && !photo.getUrl().isEmpty()) {
                try {
                    String objectName = minioUtil.extractObjectName(photo.getUrl());
                    if (objectName != null && minioUtil.fileExists(objectName)) {
                        minioUtil.deleteFile(objectName);
                        log.info("从MinIO删除文件: {}", objectName);
                    }
                } catch (Exception e) {
                    log.warn("从MinIO删除文件失败: {}", e.getMessage());
                    // 继续删除数据库记录，不因为文件删除失败而阻止
                }
            }

            // 删除数据库记录
            boolean success = photoService.removeById(id);
            if (success) {
                log.info("删除婚纱照片成功，ID: {}", id);
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除婚纱照片失败", e);
            return R.error("删除失败: " + e.getMessage());
        }
    }
}