package com.thoughtworks.agents.ohs.controller;

import com.thoughtworks.agents.application.devtask.AdvanceToWorkingCommand;
import com.thoughtworks.agents.application.devtask.CreateDevTaskCommand;
import com.thoughtworks.agents.application.devtask.DevTaskApplicationService;
import com.thoughtworks.agents.application.devtask.DevTaskDTO;
import com.thoughtworks.agents.application.devtask.ExecutePublishCommand;
import com.thoughtworks.agents.application.devtask.StartDevelopmentCommand;
import com.thoughtworks.agents.ohs.common.Result;
import com.thoughtworks.agents.ohs.dto.AdvanceToWorkingRequest;
import com.thoughtworks.agents.ohs.dto.CreateDevTaskRequest;
import com.thoughtworks.agents.ohs.dto.ExecutePublishRequest;
import com.thoughtworks.agents.ohs.dto.StartDevelopmentRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dev-tasks")
public class DevTaskController {

    private final DevTaskApplicationService devTaskApplicationService;

    public DevTaskController(DevTaskApplicationService devTaskApplicationService) {
        this.devTaskApplicationService = devTaskApplicationService;
    }

    @PostMapping
    public Result<DevTaskDTO> createDevTask(@Validated @RequestBody CreateDevTaskRequest request) {
        CreateDevTaskCommand command = CreateDevTaskCommand.builder()
                .conversationId(request.getConversationId())
                .repositoryFullName(request.getRepositoryFullName())
                .branchName(request.getBranchName())
                .requirement(request.getRequirement())
                .build();
        DevTaskDTO devTaskDTO = devTaskApplicationService.createDevTask(command);
        return Result.success(devTaskDTO);
    }

    @PostMapping("/{taskId}/start")
    public Result<DevTaskDTO> startDevelopment(@PathVariable String taskId,
                                               @Validated @RequestBody StartDevelopmentRequest request) {
        StartDevelopmentCommand command = StartDevelopmentCommand.builder()
                .taskId(taskId)
                .workingDirectory(request.getWorkingDirectory())
                .environmentVariables(request.getEnvironmentVariables())
                .build();
        DevTaskDTO devTaskDTO = devTaskApplicationService.startDevelopment(command);
        return Result.success(devTaskDTO);
    }

    @PostMapping("/{taskId}/advance")
    public Result<DevTaskDTO> advanceToWorking(@PathVariable String taskId,
                                               @Validated @RequestBody AdvanceToWorkingRequest request) {
        AdvanceToWorkingCommand command = AdvanceToWorkingCommand.builder()
                .taskId(taskId)
                .designOutput(request.getDesignOutput())
                .workingDirectory(request.getWorkingDirectory())
                .environmentVariables(request.getEnvironmentVariables())
                .build();
        DevTaskDTO devTaskDTO = devTaskApplicationService.advanceToWorking(command);
        return Result.success(devTaskDTO);
    }

    @PostMapping("/{taskId}/publish")
    public Result<DevTaskDTO> executePublish(@PathVariable String taskId,
                                             @Validated @RequestBody ExecutePublishRequest request) {
        ExecutePublishCommand command = ExecutePublishCommand.builder()
                .taskId(taskId)
                .baseBranch(request.getBaseBranch())
                .build();
        DevTaskDTO devTaskDTO = devTaskApplicationService.executePublish(command);
        return Result.success(devTaskDTO);
    }

    @GetMapping("/{taskId}")
    public Result<DevTaskDTO> getDevTask(@PathVariable String taskId) {
        DevTaskDTO devTaskDTO = devTaskApplicationService.getDevTask(taskId);
        return Result.success(devTaskDTO);
    }

    @GetMapping
    public Result<List<DevTaskDTO>> listDevTasks(@RequestParam String conversationId) {
        List<DevTaskDTO> devTaskDTOList = devTaskApplicationService.listDevTasksByConversation(conversationId);
        return Result.success(devTaskDTOList);
    }
}
