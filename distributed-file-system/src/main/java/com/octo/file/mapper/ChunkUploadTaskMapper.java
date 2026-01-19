package com.octo.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.file.entity.ChunkUploadTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 分片上传任务Mapper
 */
@Mapper
public interface ChunkUploadTaskMapper extends BaseMapper<ChunkUploadTask> {

    /**
     * 根据uploadId查询任务
     */
    @Select("SELECT * FROM t_chunk_upload_task WHERE upload_id = #{uploadId}")
    ChunkUploadTask findByUploadId(@Param("uploadId") String uploadId);

    /**
     * 根据文件MD5查询未完成的任务（断点续传）
     */
    @Select("SELECT * FROM t_chunk_upload_task WHERE file_md5 = #{md5} AND status = 0 " +
            "AND uploader_id = #{uploaderId} LIMIT 1")
    ChunkUploadTask findUnfinishedByMd5(@Param("md5") String md5, @Param("uploaderId") Long uploaderId);

    /**
     * 更新已上传分片数
     */
    @Update("UPDATE t_chunk_upload_task SET uploaded_chunks = uploaded_chunks + 1 " +
            "WHERE upload_id = #{uploadId}")
    int incrementUploadedChunks(@Param("uploadId") String uploadId);

    /**
     * 更新任务状态
     */
    @Update("UPDATE t_chunk_upload_task SET status = #{status} WHERE upload_id = #{uploadId}")
    int updateStatus(@Param("uploadId") String uploadId, @Param("status") Integer status);
}

