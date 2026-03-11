package com.thoughtworks.agents.infr.repository.ccsession;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CCSessionMapper extends BaseMapper<CCSessionPO> {

    @Select("SELECT * FROM cc_session WHERE status IN ('CREATED', 'RUNNING') ORDER BY created_at DESC")
    List<CCSessionPO> selectActiveSessions();
}
