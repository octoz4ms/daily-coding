package com.octo.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.file.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件信息Mapper
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 根据MD5查询文件（秒传检测）
     */
    @Select("SELECT * FROM t_file_info WHERE file_md5 = #{md5} AND upload_status = 1 LIMIT 1")
    FileInfo findByMd5(@Param("md5") String md5);
}

