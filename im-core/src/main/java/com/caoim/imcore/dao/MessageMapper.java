package com.caoim.imcore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caoim.imcore.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
