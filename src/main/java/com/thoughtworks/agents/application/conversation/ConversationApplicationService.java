package com.thoughtworks.agents.application.conversation;

import com.thoughtworks.agents.application.exception.BusinessException;
import com.thoughtworks.agents.domain.ccsession.model.CCSession;
import com.thoughtworks.agents.domain.ccsession.model.ProcessConfig;
import com.thoughtworks.agents.domain.ccsession.repository.CCSessionRepository;
import com.thoughtworks.agents.domain.conversation.model.Conversation;
import com.thoughtworks.agents.domain.conversation.model.ConversationId;
import com.thoughtworks.agents.domain.conversation.model.ConversationStatus;
import com.thoughtworks.agents.domain.conversation.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationApplicationService {

    private final ConversationRepository conversationRepository;
    private final CCSessionRepository ccSessionRepository;

    public ConversationApplicationService(ConversationRepository conversationRepository,
                                          CCSessionRepository ccSessionRepository) {
        this.conversationRepository = conversationRepository;
        this.ccSessionRepository = ccSessionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationDTO createConversation(CreateConversationCommand command) {
        Conversation conversation = Conversation.create(command.getTitle(), command.getRepositoryFullName());
        conversationRepository.save(conversation);
        return ConversationDTO.from(conversation);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConversationDTO sendMessage(SendMessageCommand command) {
        Conversation conversation = conversationRepository.findById(new ConversationId(command.getConversationId()))
                .orElseThrow(() -> new BusinessException("对话不存在: " + command.getConversationId()));

        if (conversation.getStatus() == ConversationStatus.CREATED) {
            ProcessConfig processConfig = new ProcessConfig(
                    "claude -p '" + command.getContent() + "'",
                    command.getWorkingDirectory(),
                    command.getEnvironmentVariables()
            );
            CCSession ccSession = CCSession.create(processConfig);
            ccSessionRepository.save(ccSession);
            conversation.activate(ccSession.getId());
        }

        conversation.addUserMessage(command.getContent());
        conversationRepository.save(conversation);
        return ConversationDTO.from(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationDTO> listConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        return conversations.stream().map(ConversationDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationDTO> listConversationsByRepository(String repositoryFullName) {
        List<Conversation> conversations = conversationRepository.findByRepositoryFullName(repositoryFullName);
        return conversations.stream().map(ConversationDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public ConversationDTO getConversation(String conversationId) {
        Conversation conversation = conversationRepository.findById(new ConversationId(conversationId))
                .orElseThrow(() -> new BusinessException("对话不存在: " + conversationId));
        return ConversationDTO.from(conversation);
    }

    @Transactional(rollbackFor = Exception.class)
    public void archiveConversation(ArchiveConversationCommand command) {
        Conversation conversation = conversationRepository.findById(new ConversationId(command.getConversationId()))
                .orElseThrow(() -> new BusinessException("对话不存在: " + command.getConversationId()));
        conversation.archive();
        conversationRepository.save(conversation);
    }
}
