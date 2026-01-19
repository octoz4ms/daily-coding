package com.octo.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.file.entity.ChunkRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分片记录Mapper
 */
@Mapper
public interface ChunkRecordMapper extends BaseMapper<ChunkRecord> {

    /**
     * 查询任务的所有分片记录
     */
    @Select("SELECT * FROM t_chunk_record WHERE upload_id = #{uploadId} ORDER BY chunk_number")
    List<ChunkRecord> findByUploadId(@Param("uploadId") String uploadId);

    /**
     * 查询已完成的分片序号列表
     */
    @Select("SELECT chunk_number FROM t_chunk_record WHERE upload_id = #{uploadId} AND status = 1")
    List<Integer> findUploadedChunkNumbers(@Param("uploadId") String uploadId);

    /**
     * 检查分片是否已上传
     */
    @Select("SELECT COUNT(1) FROM t_chunk_record WHERE upload_id = #{uploadId} " +
            "AND chunk_number = #{chunkNumber} AND status = 1")
    int checkChunkUploaded(@Param("uploadId") String uploadId, @Param("chunkNumber") Integer chunkNumber);

    /**
     * 删除任务的所有分片记录
     */
    @Delete("DELETE FROM t_chunk_record WHERE upload_id = #{uploadId}")
    int deleteByUploadId(@Param("uploadId") String uploadId);
}

