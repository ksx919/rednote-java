package com.rednote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rednote.entity.ConversationMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {
}
