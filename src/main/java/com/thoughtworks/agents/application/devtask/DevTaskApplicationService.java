package com.thoughtworks.agents.application.devtask;

import com.thoughtworks.agents.application.exception.BusinessException;
import com.thoughtworks.agents.domain.ccsession.model.CCSession;
import com.thoughtworks.agents.domain.ccsession.model.ProcessConfig;
import com.thoughtworks.agents.domain.ccsession.repository.CCSessionRepository;
import com.thoughtworks.agents.domain.conversation.model.ConversationId;
import com.thoughtworks.agents.domain.devtask.event.DevTaskEventPublisher;
import com.thoughtworks.agents.domain.devtask.event.DevTaskStatusChangedEvent;
import com.thoughtworks.agents.domain.devtask.model.DevTask;
import com.thoughtworks.agents.domain.devtask.model.DevTaskId;
import com.thoughtworks.agents.domain.devtask.model.DevTaskStatus;
import com.thoughtworks.agents.domain.devtask.repository.DevTaskRepository;
import com.thoughtworks.agents.domain.exception.GitHubApiException;
import com.thoughtworks.agents.domain.exception.GitHubMergeConflictException;
import com.thoughtworks.agents.domain.github.acl.GitHubAclService;
import com.thoughtworks.agents.domain.github.model.GitHubIntegration;
import com.thoughtworks.agents.domain.github.model.OAuthToken;
import com.thoughtworks.agents.domain.github.repository.GitHubIntegrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DevTaskApplicationService {

    private final DevTaskRepository devTaskRepository;
    private final CCSessionRepository ccSessionRepository;
    private final DevTaskEventPublisher devTaskEventPublisher;
    private final GitHubIntegrationRepository gitHubIntegrationRepository;
    private final GitHubAclService gitHubAclService;

    public DevTaskApplicationService(DevTaskRepository devTaskRepository,
                                     CCSessionRepository ccSessionRepository,
                                     DevTaskEventPublisher devTaskEventPublisher,
                                     GitHubIntegrationRepository gitHubIntegrationRepository,
                                     GitHubAclService gitHubAclService) {
        this.devTaskRepository = devTaskRepository;
        this.ccSessionRepository = ccSessionRepository;
        this.devTaskEventPublisher = devTaskEventPublisher;
        this.gitHubIntegrationRepository = gitHubIntegrationRepository;
        this.gitHubAclService = gitHubAclService;
    }

    @Transactional(rollbackFor = Exception.class)
    public DevTaskDTO createDevTask(CreateDevTaskCommand command) {
        DevTask task = DevTask.create(
                new ConversationId(command.getConversationId()),
                command.getRepositoryFullName(),
                command.getBranchName(),
                command.getRequirement()
        );
        devTaskRepository.save(task);
        return DevTaskDTO.from(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public DevTaskDTO startDevelopment(StartDevelopmentCommand command) {
        DevTask task = devTaskRepository.findById(new DevTaskId(command.getTaskId()))
                .orElseThrow(() -> new BusinessException("开发任务不存在: " + command.getTaskId()));

        DevTaskStatus previousStatus = task.getStatus();

        ProcessConfig processConfig = new ProcessConfig(
                "claude -p '请基于以下需求生成设计方案: " + task.getRequirement() + "'",
                command.getWorkingDirectory(),
                command.getEnvironmentVariables()
        );
        CCSession thinkingSession = CCSession.create(processConfig);
        ccSessionRepository.save(thinkingSession);

        task.startThinking(thinkingSession.getId());
        devTaskRepository.save(task);

        devTaskEventPublisher.publishStatusChanged(new DevTaskStatusChangedEvent(
                task.getId(), previousStatus, task.getStatus(), LocalDateTime.now()
        ));

        return DevTaskDTO.from(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public DevTaskDTO advanceToWorking(AdvanceToWorkingCommand command) {
        DevTask task = devTaskRepository.findById(new DevTaskId(command.getTaskId()))
                .orElseThrow(() -> new BusinessException("开发任务不存在: " + command.getTaskId()));

        DevTaskStatus previousStatus = task.getStatus();

        task.completeThinking(command.getDesignOutput());

        ProcessConfig processConfig = new ProcessConfig(
                "claude -p '请基于以下设计方案开始编码实现: " + command.getDesignOutput() + "'",
                command.getWorkingDirectory(),
                command.getEnvironmentVariables()
        );
        CCSession workingSession = CCSession.create(processConfig);
        ccSessionRepository.save(workingSession);

        task.startWorking(workingSession.getId());
        devTaskRepository.save(task);

        devTaskEventPublisher.publishStatusChanged(new DevTaskStatusChangedEvent(
                task.getId(), previousStatus, task.getStatus(), LocalDateTime.now()
        ));

        return DevTaskDTO.from(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public DevTaskDTO executePublish(ExecutePublishCommand command) {
        DevTask task = devTaskRepository.findById(new DevTaskId(command.getTaskId()))
                .orElseThrow(() -> new BusinessException("开发任务不存在: " + command.getTaskId()));

        DevTaskStatus previousStatus = task.getStatus();

        task.startPublishing();
        devTaskRepository.save(task);

        GitHubIntegration integration = gitHubIntegrationRepository.find()
                .orElseThrow(() -> new BusinessException("请先完成 GitHub OAuth 认证"));
        OAuthToken token = integration.getRequiredToken();

        try {
            gitHubAclService.mergeBranch(token, task.getRepositoryFullName(),
                    task.getBranchName(), command.getBaseBranch());
        } catch (GitHubMergeConflictException e) {
            task.fail("分支合并冲突");
            devTaskRepository.save(task);
            throw e;
        } catch (GitHubApiException e) {
            task.fail("GitHub API 调用失败");
            devTaskRepository.save(task);
            throw e;
        }

        task.completePublishing();
        devTaskRepository.save(task);

        devTaskEventPublisher.publishStatusChanged(new DevTaskStatusChangedEvent(
                task.getId(), previousStatus, task.getStatus(), LocalDateTime.now()
        ));

        return DevTaskDTO.from(task);
    }

    @Transactional(readOnly = true)
    public DevTaskDTO getDevTask(String taskId) {
        DevTask task = devTaskRepository.findById(new DevTaskId(taskId))
                .orElseThrow(() -> new BusinessException("开发任务不存在: " + taskId));
        return DevTaskDTO.from(task);
    }

    @Transactional(readOnly = true)
    public List<DevTaskDTO> listDevTasksByConversation(String conversationId) {
        List<DevTask> tasks = devTaskRepository.findByConversationId(new ConversationId(conversationId));
        return tasks.stream().map(DevTaskDTO::from).toList();
    }
}
