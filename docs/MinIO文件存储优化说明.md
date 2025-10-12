# MinIO文件存储优化说明

## 概述
本次优化将项目的文件存储从本地文件系统迁移到MinIO对象存储，提供更好的可扩展性、可靠性和性能。

## 主要变更

### 1. MinioUtil工具类优化
- **位置**: `src/main/java/org/fix/repair/common/MinioUtil.java`
- **新增功能**:
  - `uploadFile(MultipartFile file)` - 直接上传MultipartFile
  - `uploadFile(MultipartFile file, String folder)` - 上传到指定目录
  - `extractObjectName(String fileUrl)` - 从完整URL提取对象名
  - `fileExists(String objectName)` - 检查文件是否存在
  - `getFileInfo(String objectName)` - 获取文件信息
  - `validateFileType(String fileName, String[] allowedTypes)` - 验证文件类型
  - `isImageFile(String fileName)` - 验证是否为图片文件
  - `generateUniqueFileName(String fileExtension)` - 生成唯一文件名

### 2. AdminController文件管理优化
- **位置**: `src/main/java/org/fix/repair/controller/AdminController.java`
- **变更内容**:
  - 文件上传接口使用MinIO替代本地存储
  - 新增文件下载接口 `GET /admin/api/download`
  - 新增文件删除接口 `DELETE /admin/api/file`
  - 改进文件类型验证和错误处理

### 3. WeddingPhotoController优化
- **位置**: `src/main/java/org/fix/repair/controller/WeddingPhotoController.java`
- **变更内容**:
  - 使用MinIO存储婚纱照片
  - 优化文件上传逻辑
  - 删除照片时同时删除MinIO中的文件
  - 改进错误处理和日志记录

### 4. WebConfig配置更新
- **位置**: `src/main/java/org/fix/repair/config/WebConfig.java`
- **变更内容**:
  - 移除本地文件静态资源映射
  - 添加注释说明文件存储迁移

### 5. MinIO配置类
- **位置**: `src/main/java/org/fix/repair/config/MinioConfig.java`
- **功能**:
  - 应用启动时自动检查并创建MinIO存储桶
  - 确保存储桶存在性

## 配置文件说明

### application.yml配置
```yaml
gp:
  minio:
    endpointUrl: http://124.222.172.221:9000
    accessKey: 2AyrlhH3xJa8McdX4Gqh
    secreKey: BqXqVYiHt3std78ZJL76wA36cjduf3JiyHbuk2Pc
    bucketName: bookstore
```

## API接口说明

### 文件上传
- **接口**: `POST /admin/api/upload`
- **参数**: `file` (MultipartFile)
- **返回**: 
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "url": "http://124.222.172.221:9000/bookstore/20241201120000_abc123.jpg",
    "filename": "original.jpg"
  }
}
```

### 文件下载
- **接口**: `GET /admin/api/download?url={fileUrl}`
- **功能**: 从MinIO下载文件
- **返回**: 文件流

### 文件删除
- **接口**: `DELETE /admin/api/file?url={fileUrl}`
- **功能**: 从MinIO删除文件
- **返回**: 
```json
{
  "code": 200,
  "msg": "成功",
  "data": "文件删除成功"
}
```

### 婚纱照片上传
- **接口**: `POST /wedding/photo/upload`
- **参数**: 
  - `file` (MultipartFile) - 照片文件
  - `storyId` (Long, 可选) - 故事ID
- **返回**: 
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": "照片ID",
    "storyId": "故事ID",
    "url": "http://124.222.172.221:9000/bookstore/20241201120000_abc123.jpg",
    "remark": "原文件名"
  }
}
```

## 文件存储结构

```
bookstore/            # bucket根目录
├── 20241201120000_abc123.jpg    # 管理端上传的图片
├── 20241201120001_def456.jpg    # 婚纱照片
├── 20241201120002_ghi789.jpg    # 其他文件
└── ...
```

**注意**: 所有文件直接存储在bucket根目录，不再使用子文件夹分类。

## 文件命名规则
- 格式: `{timestamp}_{uuid}.{extension}`
- 示例: `20241201120000_abc123def456.jpg`
- 时间戳: `yyyyMMddHHmmss`
- UUID: 32位随机字符串

## 支持的文件类型
- 图片: jpg, jpeg, png, gif, bmp, webp
- 其他类型可通过 `validateFileType` 方法自定义

## 优势
1. **可扩展性**: MinIO支持分布式存储，易于扩展
2. **可靠性**: 数据冗余和备份机制
3. **性能**: 对象存储优化，访问速度快
4. **管理**: 统一的文件管理接口
5. **安全**: 支持访问控制和权限管理

## 注意事项
1. 确保MinIO服务正常运行
2. 检查网络连接和防火墙设置
3. 定期备份重要文件
4. 监控存储空间使用情况
5. 考虑设置文件生命周期策略

## 迁移建议
1. 备份现有本地文件
2. 逐步迁移历史文件到MinIO
3. 更新前端文件URL引用
4. 测试文件上传下载功能
5. 清理本地文件存储
