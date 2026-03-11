package com.thoughtworks.agents.infr.repository.devtask;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskPhaseMapper extends BaseMapper<TaskPhasePO> {

    @Select("SELECT * FROM task_phase WHERE dev_task_id = #{devTaskId} ORDER BY started_at ASC")
    List<TaskPhasePO> selectByDevTaskId(@Param("devTaskId") String devTaskId);

    @Delete("DELETE FROM task_phase WHERE dev_task_id = #{devTaskId}")
    int deleteByDevTaskId(@Param("devTaskId") String devTaskId);
}
