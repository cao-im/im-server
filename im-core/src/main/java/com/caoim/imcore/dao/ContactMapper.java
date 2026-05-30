package com.caoim.imcore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caoim.imcore.entity.Contact;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
}
