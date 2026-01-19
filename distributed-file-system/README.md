# 分布式文件系统 (Distributed File System)

> 📁 大文件分片上传系统 - 面试亮点项目

## 📌 核心技术点

| 技术点 | 实现方案 | 说明 |
|-------|---------|------|
| 对象存储 | MinIO | S3兼容的分布式存储 |
| 分片上传 | 前端切片 + 并行上传 | 大文件切分为小块 |
| 断点续传 | 任务记录 + 分片状态 | 支持续传未完成任务 |
| 文件秒传 | MD5去重 | 相同文件直接返回 |
| CDN加速 | 预签名URL | 配合CDN分发 |

## 🎯 面试必问：大文件如何上传？

### 核心方案：分片上传 + 断点续传 + 秒传

```
┌─────────────────────────────────────────────────────────────┐
│                     大文件上传流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. 计算文件MD5                                              │
│        │                                                     │
│        ▼                                                     │
│  2. 初始化上传 ──────────────────────────────────────────┐  │
│        │                                                 │  │
│        ├── 秒传检测 ──> MD5已存在? ──> 直接返回URL       │  │
│        │                                                 │  │
│        ├── 断点续传 ──> 有未完成任务? ──> 返回已上传分片  │  │
│        │                                                 │  │
│        └── 创建任务 ──> 返回uploadId + 分片信息          │  │
│                                                          │  │
│  3. 分片上传（并行）                                        │
│        │                                                     │
│        ├── Chunk 1 ──────┐                                  │
│        ├── Chunk 2 ──────┼──> MinIO                         │
│        ├── Chunk 3 ──────┤                                  │
│        └── ...    ───────┘                                  │
│                                                              │
│  4. 合并分片                                                 │
│        │                                                     │
│        └── composeObject() ──> 生成完整文件                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 关键代码

```java
// 1. 秒传检测
public InitUploadResponse initUpload(InitUploadRequest request) {
    // 根据MD5查找已存在的文件
    FileInfo existingFile = fileInfoMapper.findByMd5(request.getFileMd5());
    if (existingFile != null) {
        // 秒传成功，直接返回URL
        return InitUploadResponse.instantSuccess(existingFile.getUrl());
    }
    
    // 检查断点续传...
    // 创建新任务...
}

// 2. 分片上传
public boolean uploadChunk(String uploadId, Integer chunkNumber, MultipartFile file) {
    // 幂等检查
    if (chunkRecordMapper.checkChunkUploaded(uploadId, chunkNumber) > 0) {
        return true; // 已上传，跳过
    }
    
    // 上传分片到MinIO
    String chunkObjectName = getChunkObjectName(objectName, chunkNumber);
    minioService.uploadChunk(chunkObjectName, file.getInputStream(), file.getSize());
    
    // 记录分片状态
    chunkRecordMapper.insert(record);
}

// 3. 合并分片
public String mergeChunks(String uploadId) {
    List<String> chunkNames = chunkRecordMapper.findByUploadId(uploadId)
            .stream().map(ChunkRecord::getStoragePath).toList();
    
    // MinIO合并API
    minioService.mergeChunks(objectName, chunkNames);
    
    // 清理分片
    minioService.deleteChunks(chunkNames);
}
```

## 📊 分片上传时序图

```
前端                   服务端                    MinIO
 │                       │                        │
 │── 1.计算文件MD5 ──────>│                        │
 │                       │                        │
 │── 2.初始化上传 ───────>│                        │
 │                       │── 检查秒传 ────────────>│
 │<── 返回uploadId ──────│                        │
 │                       │                        │
 │── 3.上传分片1 ────────>│                        │
 │                       │── PUT chunk1 ─────────>│
 │<── ACK ───────────────│                        │
 │                       │                        │
 │── 3.上传分片2 ────────>│                        │
 │     (并行)            │── PUT chunk2 ─────────>│
 │<── ACK ───────────────│                        │
 │                       │                        │
 │── 4.合并请求 ─────────>│                        │
 │                       │── composeObject ──────>│
 │                       │<── 合并完成 ───────────│
 │<── 返回文件URL ───────│                        │
```

## 🗄️ 数据库设计

```sql
-- 文件信息表
CREATE TABLE t_file_info (
    id BIGINT PRIMARY KEY,
    file_name VARCHAR(255),
    file_size BIGINT,
    file_md5 VARCHAR(32) UNIQUE,  -- 秒传检测
    storage_path VARCHAR(500),
    url VARCHAR(500),
    ...
);

-- 分片上传任务表
CREATE TABLE t_chunk_upload_task (
    id BIGINT PRIMARY KEY,
    upload_id VARCHAR(64) UNIQUE,
    file_md5 VARCHAR(32),
    total_chunks INT,
    uploaded_chunks INT,
    status TINYINT,
    expire_time DATETIME,
    ...
);

-- 分片记录表
CREATE TABLE t_chunk_record (
    id BIGINT PRIMARY KEY,
    upload_id VARCHAR(64),
    chunk_number INT,
    storage_path VARCHAR(500),
    status TINYINT,
    UNIQUE INDEX (upload_id, chunk_number)
);
```

## 📁 项目结构

```
distributed-file-system/
├── src/main/java/com/octo/file/
│   ├── FileSystemApplication.java    # 启动类
│   ├── common/
│   │   └── Result.java               # 统一响应
│   ├── config/
│   │   └── MinioConfig.java          # MinIO配置
│   ├── controller/
│   │   └── FileUploadController.java # 上传接口
│   ├── dto/
│   │   ├── InitUploadRequest.java    # 初始化请求
│   │   └── InitUploadResponse.java   # 初始化响应
│   ├── entity/
│   │   ├── FileInfo.java             # 文件信息
│   │   ├── ChunkUploadTask.java      # 上传任务
│   │   └── ChunkRecord.java          # 分片记录
│   ├── mapper/
│   │   ├── FileInfoMapper.java
│   │   ├── ChunkUploadTaskMapper.java
│   │   └── ChunkRecordMapper.java
│   └── service/
│       ├── FileUploadService.java    # 上传服务
│       └── MinioService.java         # MinIO服务
└── src/main/resources/
    ├── application.yml
    └── db/schema.sql
```

## 🚀 快速启动

### 1. 环境要求

- JDK 17+
- MySQL 8.0+
- MinIO (Docker启动)

### 2. 启动MinIO

```bash
docker run -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

### 3. 初始化数据库

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

## 📡 API接口

### 初始化上传

```bash
POST /api/file/init
Content-Type: application/json

{
  "fileName": "video.mp4",
  "fileSize": 1073741824,
  "fileMd5": "d41d8cd98f00b204e9800998ecf8427e"
}

# 响应 - 秒传成功
{
  "code": 200,
  "data": {
    "instantUpload": true,
    "fileUrl": "http://localhost:9000/files/2026/01/16/xxx.mp4"
  }
}

# 响应 - 需要上传
{
  "code": 200,
  "data": {
    "instantUpload": false,
    "uploadId": "abc123",
    "totalChunks": 200,
    "chunkSize": 5242880,
    "uploadedChunks": [1, 2, 3]  // 断点续传
  }
}
```

### 上传分片

```bash
POST /api/file/chunk?uploadId=abc123&chunkNumber=1
Content-Type: multipart/form-data

file: (binary)
```

### 合并分片

```bash
POST /api/file/merge?uploadId=abc123
```

## 🔍 面试常见追问

### 1. 为什么选择MD5做秒传？

- MD5碰撞概率极低（理论上2^64次才可能碰撞）
- 计算速度快
- 实际项目中可以结合文件大小双重校验

### 2. 分片大小如何确定？

**考虑因素**：
- 太小：请求次数多，HTTP开销大
- 太大：单个分片上传时间长，失败重传代价高

**推荐**：5MB ~ 10MB

### 3. 如何保证分片上传的幂等性？

```java
// 上传前检查分片是否已存在
if (chunkRecordMapper.checkChunkUploaded(uploadId, chunkNumber) > 0) {
    return true; // 已上传，直接返回成功
}
```

### 4. 如何处理上传中断？

1. **任务状态持久化**：记录uploadId和已上传分片
2. **分片状态检查**：续传时返回已上传分片列表
3. **定时清理**：过期任务和分片自动清理

### 5. CDN如何配合？

```java
// 生成预签名URL，配合CDN
public String getPresignedUrl(String objectName, int expireMinutes) {
    return minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .method(Method.GET)
            .expiry(expireMinutes, TimeUnit.MINUTES)
            .build());
}
```

## 📈 性能优化建议

1. **并行上传**：前端多线程上传分片
2. **分片预检**：上传前批量检查已上传分片
3. **压缩传输**：启用GZIP压缩
4. **就近上传**：多区域部署，选择最近节点

## 📝 License

MIT License

