package org.fix.repair.controller;
import org.fix.repair.common.MinioUtil;
import org.fix.repair.common.R;
import org.fix.repair.entity.WeddingPhoto;
import org.fix.repair.service.WeddingPhotoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/wedding/photo")
public class WeddingPhotoController {
    private final WeddingPhotoService photoService;

    private final MinioUtil minioUtil;

    public WeddingPhotoController(WeddingPhotoService photoService, MinioUtil minioUtil) {
        this.photoService = photoService;
        this.minioUtil = minioUtil;
    }

    // 上传照片（简化：本地存储，实际建议用OSS）
    @PostMapping("/upload")
    public R<WeddingPhoto> uploadPhoto(
            @RequestParam MultipartFile file, 
            @RequestParam(required = false) Long storyId) throws IOException {
        if (file.isEmpty()) {
            return R.error("上传文件为空");
        }
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String remoteFilePath = minioUtil.uploadFile(inputStream, fileName, contentType);

        WeddingPhoto photo = new WeddingPhoto();
        photo.setStoryId(storyId);
        photo.setUrl(remoteFilePath);
        photo.setRemark(file.getOriginalFilename());
        photoService.save(photo);

        return R.ok(photo);
    }

    // 查询所有照片
    @GetMapping("/list")
    public R<List<WeddingPhoto>> listPhoto() {
        return R.ok(photoService.list());
    }

    // 按故事ID查询照片
    @GetMapping("/story/{storyId}")
    public R<List<WeddingPhoto>> listPhotoByStoryId(@PathVariable Long storyId) {
        return R.ok(photoService.lambdaQuery()
                .eq(WeddingPhoto::getStoryId, storyId)
                .list());
    }

    /**
     * 删除照片
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteStory(@PathVariable Long id) {
        return photoService.removeById(id)
                ? R.ok(null)
                : R.error("删除失败");
    }
}