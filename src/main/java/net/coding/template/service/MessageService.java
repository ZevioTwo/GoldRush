package net.coding.template.service;

import net.coding.template.entity.po.Contract;
import net.coding.template.entity.po.MessageItem;
import net.coding.template.entity.po.MessageSession;
import net.coding.template.entity.po.User;
import net.coding.template.entity.request.MessageSendRequest;
import net.coding.template.entity.response.MessageOpenSessionResponse;
import net.coding.template.entity.response.MessageSessionDetailResponse;
import net.coding.template.entity.response.MessageSessionListResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.ContractMapper;
import net.coding.template.mapper.MessageItemMapper;
import net.coding.template.mapper.MessageSessionMapper;
import net.coding.template.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MessageService {

    private static final String SESSION_TYPE_PRIVATE = "PRIVATE";
    private static final String SESSION_TYPE_SYSTEM = "SYSTEM";
    private static final String BIZ_TYPE_CONTRACT = "CONTRACT";
    private static final String BIZ_TYPE_SYSTEM = "SYSTEM";
    private static final String MSG_TYPE_TEXT = "TEXT";
    private static final String MSG_TYPE_SYSTEM = "SYSTEM";
    private static final String SYSTEM_SESSION_KEY = "SYSTEM";
    private static final String SYSTEM_SESSION_NAME = "系统通知";
    private static final String SYSTEM_SESSION_TAG = "系统";
    private static final String SYSTEM_SESSION_AVATAR = "/images/message_active.png";
    private static final int MESSAGE_FETCH_LIMIT = 100;

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private MessageSessionMapper messageSessionMapper;

    @Resource
    private MessageItemMapper messageItemMapper;

    public MessageSessionListResponse getSessions(String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        List<MessageSessionListResponse.MessageSessionItem> sessions =
                messageSessionMapper.selectUserSessions(currentUser.getId());
        if (sessions == null) {
            sessions = Collections.emptyList();
        }

        int unreadTotal = 0;
        for (MessageSessionListResponse.MessageSessionItem session : sessions) {
            session.setCanReply(SESSION_TYPE_PRIVATE.equals(session.getSessionType()));
            unreadTotal += session.getUnreadCount() == null ? 0 : session.getUnreadCount();
        }

        MessageSessionListResponse response = new MessageSessionListResponse();
        response.setUnreadTotal(unreadTotal);
        response.setSessions(sessions);
        return response;
    }

    @Transactional
    public MessageSessionDetailResponse getSessionDetail(Long sessionId, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        MessageSession session = getOwnedSession(sessionId, currentUser.getId());
        List<MessageItem> items = messageItemMapper.selectLatestBySessionId(session.getId(), MESSAGE_FETCH_LIMIT);
        if (items == null) {
            items = Collections.emptyList();
        } else {
            items = new ArrayList<>(items);
            Collections.reverse(items);
        }

        if (session.getUnreadCount() != null && session.getUnreadCount() > 0) {
            clearUnread(session.getId(), currentUser.getId());
        }

        List<MessageSessionDetailResponse.MessageItemDTO> mappedItems = new ArrayList<>();
        for (MessageItem item : items) {
            MessageSessionDetailResponse.MessageItemDTO dto = new MessageSessionDetailResponse.MessageItemDTO();
            dto.setId(item.getId());
            dto.setSenderId(item.getSenderId());
            dto.setContent(item.getContent());
            dto.setMsgType(item.getMsgType());
            dto.setCreateTime(item.getCreateTime());
            dto.setSelf(item.getSenderId() != null && item.getSenderId().equals(currentUser.getId()));
            mappedItems.add(dto);
        }

        MessageSessionDetailResponse response = new MessageSessionDetailResponse();
        response.setSessionId(session.getId());
        response.setSessionType(session.getSessionType());
        response.setPeerName(session.getPeerName());
        response.setPeerAvatar(session.getPeerAvatar());
        response.setPeerTag(session.getPeerTag());
        response.setBizType(session.getBizType());
        response.setBizId(session.getBizId());
        response.setCanReply(SESSION_TYPE_PRIVATE.equals(session.getSessionType()));
        response.setItems(mappedItems);
        return response;
    }

    @Transactional
    public void markSessionRead(Long sessionId, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }
        getOwnedSession(sessionId, currentUser.getId());
        clearUnread(sessionId, currentUser.getId());
    }

    @Transactional
    public MessageOpenSessionResponse openContractSession(String contractId, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Contract contract = getContractForMessage(contractId, currentUser.getId());
        Long peerUserId = getPeerUserId(contract, currentUser.getId());
        if (peerUserId == null) {
            throw new BusinessException(400, "当前契约暂无可联系的对方");
        }

        User peerUser = userMapper.selectById(peerUserId);
        if (peerUser == null) {
            throw new BusinessException(404, "对方用户不存在");
        }

        MessageSession currentSession = ensureContractSession(currentUser, peerUser, contract);
        MessageSession peerSession = ensureContractSession(peerUser, currentUser, contract);
        ensureContractIntroMessage(currentSession, contract);
        ensureContractIntroMessage(peerSession, contract);

        MessageOpenSessionResponse response = new MessageOpenSessionResponse();
        response.setSessionId(currentSession.getId());
        response.setSessionType(currentSession.getSessionType());
        response.setPeerName(currentSession.getPeerName());
        return response;
    }

    @Transactional
    public void sendMessage(Long sessionId, MessageSendRequest request, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        MessageSession currentSession = getOwnedSession(sessionId, currentUser.getId());
        if (!SESSION_TYPE_PRIVATE.equals(currentSession.getSessionType())) {
            throw new BusinessException(400, "系统通知不支持回复");
        }

        String content = normalizeContent(request == null ? null : request.getContent());
        Contract contract = getContractForMessage(currentSession.getBizId(), currentUser.getId());
        Long peerUserId = getPeerUserId(contract, currentUser.getId());
        if (peerUserId == null) {
            throw new BusinessException(400, "当前契约暂无可聊天对象");
        }

        User peerUser = userMapper.selectById(peerUserId);
        if (peerUser == null) {
            throw new BusinessException(404, "对方用户不存在");
        }

        MessageSession selfSession = ensureContractSession(currentUser, peerUser, contract);
        MessageSession peerSession = ensureContractSession(peerUser, currentUser, contract);

        appendMessage(selfSession, currentUser.getId(), content, MSG_TYPE_TEXT, false);
        appendMessage(peerSession, currentUser.getId(), content, MSG_TYPE_TEXT, true);
    }

    @Transactional
    public void notifyContractAccepted(String contractId, Long operatorUserId) {
        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null || contract.getInitiatorId() == null || contract.getReceiverId() == null) {
            return;
        }

        User initiator = userMapper.selectById(contract.getInitiatorId());
        User receiver = userMapper.selectById(contract.getReceiverId());
        if (initiator == null || receiver == null) {
            return;
        }

        MessageSession initiatorSession = ensureContractSession(initiator, receiver, contract);
        MessageSession receiverSession = ensureContractSession(receiver, initiator, contract);
        ensureContractIntroMessage(initiatorSession, contract);
        ensureContractIntroMessage(receiverSession, contract);

        pushSystemMessage(
                initiator.getId(),
                String.format("契约《%s》已被 %s 接取，双方现在可以通过站内消息沟通履约细节。", safeNickname(contract.getTitle(), contract.getContractNo()), safeNickname(receiver.getNickname(), "接单玩家")),
                BIZ_TYPE_CONTRACT,
                contract.getId()
        );

        if (operatorUserId == null || !operatorUserId.equals(receiver.getId())) {
            pushSystemMessage(
                    receiver.getId(),
                    String.format("你已成功接取契约《%s》，现在可以联系发起人沟通细节。", safeNickname(contract.getTitle(), contract.getContractNo())),
                    BIZ_TYPE_CONTRACT,
                    contract.getId()
            );
        }
    }

    @Transactional
    public void notifyContractStarted(String contractId) {
        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null || contract.getInitiatorId() == null || contract.getReceiverId() == null) {
            return;
        }

        String content = String.format("契约《%s》已签订并进入进行中，请保持沟通并按约履约。", safeNickname(contract.getTitle(), contract.getContractNo()));
        pushSystemMessage(contract.getInitiatorId(), content, BIZ_TYPE_CONTRACT, contract.getId());
        pushSystemMessage(contract.getReceiverId(), content, BIZ_TYPE_CONTRACT, contract.getId());
    }

    @Transactional
    public void notifyContractWaitingOtherConfirm(String contractId, Long operatorUserId) {
        if (operatorUserId == null) {
            return;
        }

        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null || contract.getInitiatorId() == null || contract.getReceiverId() == null) {
            return;
        }

        Long targetUserId = operatorUserId.equals(contract.getInitiatorId())
                ? contract.getReceiverId()
                : contract.getInitiatorId();
        if (targetUserId == null) {
            return;
        }

        pushSystemMessage(
                targetUserId,
                String.format("契约《%s》对方已确认完成，等待你确认后即可完结。", safeNickname(contract.getTitle(), contract.getContractNo())),
                BIZ_TYPE_CONTRACT,
                contract.getId()
        );
    }

    @Transactional
    public void notifyContractCompleted(String contractId) {
        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null || contract.getInitiatorId() == null || contract.getReceiverId() == null) {
            return;
        }

        String content = String.format("契约《%s》已完成，相关状态和结算结果已同步更新。", safeNickname(contract.getTitle(), contract.getContractNo()));
        pushSystemMessage(contract.getInitiatorId(), content, BIZ_TYPE_CONTRACT, contract.getId());
        pushSystemMessage(contract.getReceiverId(), content, BIZ_TYPE_CONTRACT, contract.getId());
    }

    private MessageSession getOwnedSession(Long sessionId, Long userId) {
        if (sessionId == null) {
            throw new BusinessException(400, "会话ID不能为空");
        }
        MessageSession session = messageSessionMapper.selectOwnedSession(sessionId, userId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }
        return session;
    }

    private Contract getContractForMessage(String contractId, Long currentUserId) {
        if (!StringUtils.hasText(contractId)) {
            throw new BusinessException(400, "契约ID不能为空");
        }
        Contract contract = contractMapper.selectContractDetail(contractId);
        if (contract == null) {
            throw new BusinessException(404, "契约不存在");
        }
        boolean isInitiator = contract.getInitiatorId() != null && contract.getInitiatorId().equals(currentUserId);
        boolean isReceiver = contract.getReceiverId() != null && contract.getReceiverId().equals(currentUserId);
        if (!isInitiator && !isReceiver) {
            throw new BusinessException(403, "无权访问此契约消息");
        }
        return contract;
    }

    private Long getPeerUserId(Contract contract, Long currentUserId) {
        if (contract == null || currentUserId == null) {
            return null;
        }
        if (contract.getInitiatorId() != null && contract.getInitiatorId().equals(currentUserId)) {
            return contract.getReceiverId();
        }
        if (contract.getReceiverId() != null && contract.getReceiverId().equals(currentUserId)) {
            return contract.getInitiatorId();
        }
        return null;
    }

    private MessageSession ensureContractSession(User owner, User peer, Contract contract) {
        if (owner == null || peer == null || contract == null) {
            throw new BusinessException(400, "创建契约会话所需参数不完整");
        }

        String sessionKey = buildContractSessionKey(contract.getId());
        MessageSession existing = messageSessionMapper.selectByUserAndSessionKey(owner.getId(), sessionKey);
        if (existing != null) {
            return existing;
        }

        MessageSession session = new MessageSession();
        session.setUserId(owner.getId());
        session.setSessionKey(sessionKey);
        session.setSessionType(SESSION_TYPE_PRIVATE);
        session.setPeerUserId(peer.getId());
        session.setPeerName(safeNickname(peer.getNickname(), "对方用户"));
        session.setPeerAvatar(peer.getAvatarUrl());
        session.setPeerTag(StringUtils.hasText(contract.getContractNo()) ? contract.getContractNo() : "契约私聊");
        session.setBizType(BIZ_TYPE_CONTRACT);
        session.setBizId(contract.getId());
        session.setLastMessage(null);
        session.setLastTime(null);
        session.setUnreadCount(0);
        session.setHighlight(Boolean.FALSE);
        messageSessionMapper.insert(session);
        return session;
    }

    private MessageSession ensureSystemSession(Long userId) {
        MessageSession existing = messageSessionMapper.selectByUserAndSessionKey(userId, SYSTEM_SESSION_KEY);
        if (existing != null) {
            return existing;
        }

        MessageSession session = new MessageSession();
        session.setUserId(userId);
        session.setSessionKey(SYSTEM_SESSION_KEY);
        session.setSessionType(SESSION_TYPE_SYSTEM);
        session.setPeerUserId(null);
        session.setPeerName(SYSTEM_SESSION_NAME);
        session.setPeerAvatar(SYSTEM_SESSION_AVATAR);
        session.setPeerTag(SYSTEM_SESSION_TAG);
        session.setBizType(BIZ_TYPE_SYSTEM);
        session.setBizId(BIZ_TYPE_SYSTEM);
        session.setLastMessage(null);
        session.setLastTime(null);
        session.setUnreadCount(0);
        session.setHighlight(Boolean.TRUE);
        messageSessionMapper.insert(session);
        return session;
    }

    private void ensureContractIntroMessage(MessageSession session, Contract contract) {
        if (session == null || contract == null) {
            return;
        }
        if (StringUtils.hasText(session.getLastMessage()) || session.getLastTime() != null) {
            return;
        }

        String content = String.format("已建立契约《%s》的沟通通道，可在这里协商履约细节。", safeNickname(contract.getTitle(), contract.getContractNo()));
        appendMessage(session, null, content, MSG_TYPE_SYSTEM, false);
    }

    private void pushSystemMessage(Long userId, String content, String bizType, String bizId) {
        if (userId == null || !StringUtils.hasText(content)) {
            return;
        }
        MessageSession session = ensureSystemSession(userId);
        appendMessage(session, null, trimToLength(content, 1000), MSG_TYPE_SYSTEM, true);
    }

    private void appendMessage(MessageSession session, Long senderId, String content, String msgType, boolean unread) {
        if (session == null || !StringUtils.hasText(content)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        MessageItem item = new MessageItem();
        item.setSessionId(session.getId());
        item.setSenderId(senderId);
        item.setContent(trimToLength(content, 1000));
        item.setMsgType(StringUtils.hasText(msgType) ? msgType : MSG_TYPE_TEXT);
        item.setIsRead(!unread);
        item.setCreateTime(now);
        messageItemMapper.insert(item);

        messageSessionMapper.updateSessionSnapshot(session.getId(), trimToLength(content, 500), now);
        if (unread) {
            messageSessionMapper.incrementUnread(session.getId());
        }
    }

    private void clearUnread(Long sessionId, Long userId) {
        messageSessionMapper.clearUnread(sessionId, userId);
        messageItemMapper.markSessionItemsRead(sessionId);
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.length() > 1000) {
            throw new BusinessException(400, "消息内容不能超过1000字");
        }
        return normalized;
    }

    private String buildContractSessionKey(String contractId) {
        return "CONTRACT:" + contractId;
    }

    private String safeNickname(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
