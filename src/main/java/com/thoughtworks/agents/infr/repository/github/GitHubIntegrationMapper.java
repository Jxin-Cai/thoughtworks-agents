package com.thoughtworks.agents.infr.repository.github;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GitHubIntegrationMapper extends BaseMapper<GitHubIntegrationPO> {
}
