package com.thoughtworks.agents.infr.repository.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thoughtworks.agents.domain.ccsession.model.CCSessionId;
import com.thoughtworks.agents.domain.conversation.model.*;
import com.thoughtworks.agents.domain.conversation.repository.ConversationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public ConversationRepositoryImpl(ConversationMapper conversationMapper, MessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public void save(Conversation conversation) {
        ConversationPO po = toConversationPO(conversation);
        ConversationPO existing = conversationMapper.selectById(po.getId());
        if (existing != null) {
            conversationMapper.updateById(po);
        } else {
            conversationMapper.insert(po);
        }

        String conversationId = conversation.getId().getValue();
        messageMapper.deleteByConversationId(conversationId);
        for (Message message : conversation.getMessages()) {
            MessagePO messagePO = toMessagePO(message, conversationId);
            messageMapper.insert(messagePO);
        }
    }

    @Override
    public Optional<Conversation> findById(ConversationId id) {
        ConversationPO po = conversationMapper.selectById(id.getValue());
        if (po == null) {
            return Optional.empty();
        }
        List<MessagePO> messagePOs = messageMapper.selectByConversationId(id.getValue());
        return Optional.of(toDomain(po, messagePOs));
    }

    @Override
    public List<Conversation> findByRepositoryFullName(String repositoryFullName) {
        LambdaQueryWrapper<ConversationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationPO::getRepositoryFullName, repositoryFullName)
                .orderByDesc(ConversationPO::getCreatedAt);
        List<ConversationPO> poList = conversationMapper.selectList(wrapper);
        return poList.stream()
                .map(po -> {
                    List<MessagePO> messagePOs = messageMapper.selectByConversationId(po.getId());
                    return toDomain(po, messagePOs);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> findAll() {
        LambdaQueryWrapper<ConversationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ConversationPO::getUpdatedAt);
        List<ConversationPO> poList = conversationMapper.selectList(wrapper);
        return poList.stream()
                .map(po -> {
                    List<MessagePO> messagePOs = messageMapper.selectByConversationId(po.getId());
                    return toDomain(po, messagePOs);
                })
                .collect(Collectors.toList());
    }

    private ConversationPO toConversationPO(Conversation conversation) {
        ConversationPO po = new ConversationPO();
        po.setId(conversation.getId().getValue());
        po.setTitle(conversation.getTitle());
        po.setRepositoryFullName(conversation.getRepositoryFullName());
        po.setCcSessionId(conversation.getCcSessionId() != null ? conversation.getCcSessionId().getValue() : null);
        po.setStatus(conversation.getStatus().name());
        po.setCreatedAt(conversation.getCreatedAt());
        po.setUpdatedAt(conversation.getUpdatedAt());
        return po;
    }

    private MessagePO toMessagePO(Message message, String conversationId) {
        MessagePO po = new MessagePO();
        po.setId(message.getId().getValue());
        po.setConversationId(conversationId);
        po.setRole(message.getRole().name());
        po.setContent(message.getContent());
        po.setCreatedAt(message.getCreatedAt());
        return po;
    }

    private Conversation toDomain(ConversationPO po, List<MessagePO> messagePOs) {
        CCSessionId ccSessionId = po.getCcSessionId() != null ? new CCSessionId(po.getCcSessionId()) : null;
        List<Message> messages = messagePOs.stream()
                .map(this::toMessageDomain)
                .collect(Collectors.toList());
        return Conversation.reconstitute(
                new ConversationId(po.getId()),
                po.getTitle(),
                po.getRepositoryFullName(),
                ccSessionId,
                ConversationStatus.valueOf(po.getStatus()),
                messages,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    private Message toMessageDomain(MessagePO po) {
        return new Message(
                new MessageId(po.getId()),
                MessageRole.valueOf(po.getRole()),
                po.getContent(),
                po.getCreatedAt()
        );
    }
}
