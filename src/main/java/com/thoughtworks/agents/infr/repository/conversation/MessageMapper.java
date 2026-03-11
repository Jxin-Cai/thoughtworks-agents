package com.thoughtworks.agents.infr.repository.conversation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<MessagePO> {

    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<MessagePO> selectByConversationId(@Param("conversationId") String conversationId);

    @Delete("DELETE FROM message WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);
}
