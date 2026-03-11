package com.thoughtworks.agents.ohs.controller;

import com.thoughtworks.agents.application.conversation.ArchiveConversationCommand;
import com.thoughtworks.agents.application.conversation.ConversationApplicationService;
import com.thoughtworks.agents.application.conversation.ConversationDTO;
import com.thoughtworks.agents.application.conversation.CreateConversationCommand;
import com.thoughtworks.agents.application.conversation.SendMessageCommand;
import com.thoughtworks.agents.ohs.common.Result;
import com.thoughtworks.agents.ohs.dto.CreateConversationRequest;
import com.thoughtworks.agents.ohs.dto.SendMessageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationApplicationService conversationApplicationService;

    public ConversationController(ConversationApplicationService conversationApplicationService) {
        this.conversationApplicationService = conversationApplicationService;
    }

    @PostMapping
    public Result<ConversationDTO> createConversation(@Validated @RequestBody CreateConversationRequest request) {
        CreateConversationCommand command = CreateConversationCommand.builder()
                .title(request.getTitle())
                .repositoryFullName(request.getRepositoryFullName())
                .build();
        ConversationDTO conversationDTO = conversationApplicationService.createConversation(command);
        return Result.success(conversationDTO);
    }

    @PostMapping("/{conversationId}/messages")
    public Result<ConversationDTO> sendMessage(@PathVariable String conversationId,
                                               @Validated @RequestBody SendMessageRequest request) {
        SendMessageCommand command = SendMessageCommand.builder()
                .conversationId(conversationId)
                .content(request.getContent())
                .workingDirectory(request.getWorkingDirectory())
                .environmentVariables(request.getEnvironmentVariables())
                .build();
        ConversationDTO conversationDTO = conversationApplicationService.sendMessage(command);
        return Result.success(conversationDTO);
    }

    @GetMapping
    public Result<List<ConversationDTO>> listConversations(@RequestParam(required = false) String repositoryFullName) {
        if (repositoryFullName != null && !repositoryFullName.isBlank()) {
            return Result.success(conversationApplicationService.listConversationsByRepository(repositoryFullName));
        } else {
            return Result.success(conversationApplicationService.listConversations());
        }
    }

    @GetMapping("/{conversationId}")
    public Result<ConversationDTO> getConversation(@PathVariable String conversationId) {
        ConversationDTO conversationDTO = conversationApplicationService.getConversation(conversationId);
        return Result.success(conversationDTO);
    }

    @PutMapping("/{conversationId}/archive")
    public Result<Void> archiveConversation(@PathVariable String conversationId) {
        ArchiveConversationCommand command = ArchiveConversationCommand.builder()
                .conversationId(conversationId)
                .build();
        conversationApplicationService.archiveConversation(command);
        return Result.success();
    }
}
