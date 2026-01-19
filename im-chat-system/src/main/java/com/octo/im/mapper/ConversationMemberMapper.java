package com.octo.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.im.entity.ConversationMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 会话成员Mapper
 */
@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {

    /**
     * 获取会话的所有成员ID
     */
    @Select("SELECT user_id FROM t_conversation_member WHERE conversation_id = #{conversationId}")
    List<Long> getMemberIds(@Param("conversationId") Long conversationId);

    /**
     * 获取除指定用户外的其他成员ID
     */
    @Select("SELECT user_id FROM t_conversation_member WHERE conversation_id = #{conversationId} " +
            "AND user_id != #{excludeUserId}")
    List<Long> getOtherMemberIds(@Param("conversationId") Long conversationId, 
                                  @Param("excludeUserId") Long excludeUserId);

    /**
     * 更新最后已读消息ID
     */
    @Update("UPDATE t_conversation_member SET last_read_msg_id = #{msgId}, last_read_time = NOW() " +
            "WHERE conversation_id = #{conversationId} AND user_id = #{userId}")
    int updateLastReadMsgId(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId,
                            @Param("msgId") Long msgId);

    /**
     * 获取会话成员信息
     */
    @Select("SELECT * FROM t_conversation_member WHERE conversation_id = #{conversationId} " +
            "AND user_id = #{userId}")
    ConversationMember getMember(@Param("conversationId") Long conversationId,
                                 @Param("userId") Long userId);
}

