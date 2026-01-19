package com.octo.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.im.entity.OfflineMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 离线消息Mapper
 */
@Mapper
public interface OfflineMessageMapper extends BaseMapper<OfflineMessage> {

    /**
     * 获取用户的离线消息
     */
    @Select("SELECT * FROM t_offline_message WHERE user_id = #{userId} ORDER BY create_time ASC")
    List<OfflineMessage> getByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的离线消息
     */
    @Delete("DELETE FROM t_offline_message WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除指定消息的离线记录
     */
    @Delete("DELETE FROM t_offline_message WHERE msg_id = #{msgId} AND user_id = #{userId}")
    int deleteByMsgIdAndUserId(@Param("msgId") String msgId, @Param("userId") Long userId);
}

