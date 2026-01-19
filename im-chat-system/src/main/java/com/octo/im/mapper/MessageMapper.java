package com.octo.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.im.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 获取会话消息列表（分页）
     */
    @Select("SELECT * FROM t_message WHERE conversation_id = #{conversationId} " +
            "AND id < #{lastMsgId} ORDER BY id DESC LIMIT #{limit}")
    List<Message> getMessagesByConversation(@Param("conversationId") Long conversationId,
                                            @Param("lastMsgId") Long lastMsgId,
                                            @Param("limit") int limit);

    /**
     * 获取指定时间之后的消息（同步）
     */
    @Select("SELECT * FROM t_message WHERE conversation_id IN " +
            "(SELECT conversation_id FROM t_conversation_member WHERE user_id = #{userId}) " +
            "AND send_time > #{since} ORDER BY send_time ASC LIMIT 100")
    List<Message> getMessagesSince(@Param("userId") Long userId, 
                                   @Param("since") LocalDateTime since);

    /**
     * 更新消息状态
     */
    @Update("UPDATE t_message SET status = #{status} WHERE msg_id = #{msgId}")
    int updateStatus(@Param("msgId") String msgId, @Param("status") Integer status);
}

