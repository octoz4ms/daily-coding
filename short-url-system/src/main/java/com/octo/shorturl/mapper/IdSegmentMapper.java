package com.octo.shorturl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.shorturl.entity.IdSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 号段Mapper
 */
@Mapper
public interface IdSegmentMapper extends BaseMapper<IdSegment> {

    /**
     * 根据业务标识查询
     */
    @Select("SELECT * FROM t_id_segment WHERE biz_tag = #{bizTag}")
    IdSegment findByBizTag(@Param("bizTag") String bizTag);

    /**
     * 获取并更新号段（乐观锁）
     */
    @Update("UPDATE t_id_segment SET max_id = max_id + step, version = version + 1 " +
            "WHERE biz_tag = #{bizTag} AND version = #{version}")
    int updateMaxId(@Param("bizTag") String bizTag, @Param("version") Integer version);
}

