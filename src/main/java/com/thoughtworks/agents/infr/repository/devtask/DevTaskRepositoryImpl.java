package com.thoughtworks.agents.infr.repository.devtask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.conversation.model.ConversationId;
import com.thoughtworks.agents.domain.devtask.model.*;
import com.thoughtworks.agents.domain.devtask.repository.DevTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DevTaskRepositoryImpl implements DevTaskRepository {

    private final DevTaskMapper devTaskMapper;
    private final TaskPhaseMapper taskPhaseMapper;

    public DevTaskRepositoryImpl(DevTaskMapper devTaskMapper, TaskPhaseMapper taskPhaseMapper) {
        this.devTaskMapper = devTaskMapper;
        this.taskPhaseMapper = taskPhaseMapper;
    }

    @Override
    public void save(DevTask task) {
        DevTaskPO po = toDevTaskPO(task);
        DevTaskPO existing = devTaskMapper.selectById(po.getId());
        if (existing != null) {
            devTaskMapper.updateById(po);
        } else {
            devTaskMapper.insert(po);
        }

        String taskId = task.getId().getValue();
        taskPhaseMapper.deleteByDevTaskId(taskId);
        for (TaskPhase phase : task.getPhases()) {
            TaskPhasePO phasePO = toTaskPhasePO(phase, taskId);
            taskPhaseMapper.insert(phasePO);
        }
    }

    @Override
    public Optional<DevTask> findById(DevTaskId id) {
        DevTaskPO po = devTaskMapper.selectById(id.getValue());
        if (po == null) {
            return Optional.empty();
        }
        List<TaskPhasePO> phasePOs = taskPhaseMapper.selectByDevTaskId(id.getValue());
        return Optional.of(toDomain(po, phasePOs));
    }

    @Override
    public List<DevTask> findByConversationId(ConversationId conversationId) {
        LambdaQueryWrapper<DevTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DevTaskPO::getConversationId, conversationId.getValue())
                .orderByDesc(DevTaskPO::getCreatedAt);
        List<DevTaskPO> poList = devTaskMapper.selectList(wrapper);
        return poList.stream()
                .map(po -> {
                    List<TaskPhasePO> phasePOs = taskPhaseMapper.selectByDevTaskId(po.getId());
                    return toDomain(po, phasePOs);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<DevTask> findByRepositoryFullName(String repositoryFullName) {
        LambdaQueryWrapper<DevTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DevTaskPO::getRepositoryFullName, repositoryFullName)
                .orderByDesc(DevTaskPO::getCreatedAt);
        List<DevTaskPO> poList = devTaskMapper.selectList(wrapper);
        return poList.stream()
                .map(po -> {
                    List<TaskPhasePO> phasePOs = taskPhaseMapper.selectByDevTaskId(po.getId());
                    return toDomain(po, phasePOs);
                })
                .collect(Collectors.toList());
    }

    private DevTaskPO toDevTaskPO(DevTask task) {
        DevTaskPO po = new DevTaskPO();
        po.setId(task.getId().getValue());
        po.setConversationId(task.getConversationId().getValue());
        po.setRepositoryFullName(task.getRepositoryFullName());
        po.setBranchName(task.getBranchName());
        po.setRequirement(task.getRequirement());
        po.setStatus(task.getStatus().name());
        po.setCreatedAt(task.getCreatedAt());
        po.setUpdatedAt(task.getUpdatedAt());
        return po;
    }

    private TaskPhasePO toTaskPhasePO(TaskPhase phase, String devTaskId) {
        TaskPhasePO po = new TaskPhasePO();
        po.setId(phase.getId().getValue());
        po.setDevTaskId(devTaskId);
        po.setPhaseType(phase.getPhaseType().name());
        po.setCcSessionId(phase.getCcSessionId() != null ? phase.getCcSessionId().getValue() : null);
        po.setOutput(phase.getOutput());
        po.setStartedAt(phase.getStartedAt());
        po.setFinishedAt(phase.getFinishedAt());
        po.setFailureReason(phase.getFailureReason());
        return po;
    }

    private DevTask toDomain(DevTaskPO po, List<TaskPhasePO> phasePOs) {
        List<TaskPhase> phases = phasePOs.stream()
                .map(this::toTaskPhaseDomain)
                .collect(Collectors.toList());
        return DevTask.reconstitute(
                new DevTaskId(po.getId()),
                new ConversationId(po.getConversationId()),
                po.getRepositoryFullName(),
                po.getBranchName(),
                po.getRequirement(),
                DevTaskStatus.valueOf(po.getStatus()),
                phases,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    private TaskPhase toTaskPhaseDomain(TaskPhasePO po) {
        CCSessionId ccSessionId = po.getCcSessionId() != null ? new CCSessionId(po.getCcSessionId()) : null;
        return TaskPhase.reconstitute(
                new TaskPhaseId(po.getId()),
                PhaseType.valueOf(po.getPhaseType()),
                ccSessionId,
                po.getOutput(),
                po.getStartedAt(),
                po.getFinishedAt(),
                po.getFailureReason()
        );
    }
}
