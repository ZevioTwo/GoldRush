ALTER TABLE `message_sessions`
  ADD COLUMN `session_key` varchar(100) NOT NULL DEFAULT '' COMMENT '会话唯一键' AFTER `user_id`,
  ADD COLUMN `session_type` varchar(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '会话类型：PRIVATE/SYSTEM' AFTER `session_key`,
  ADD COLUMN `peer_user_id` bigint(20) DEFAULT NULL COMMENT '对方用户ID' AFTER `session_type`,
  ADD COLUMN `biz_type` varchar(20) DEFAULT NULL COMMENT '业务类型：CONTRACT/SYSTEM' AFTER `peer_tag`,
  ADD COLUMN `biz_id` varchar(64) DEFAULT NULL COMMENT '业务ID，如contractId' AFTER `biz_type`;

ALTER TABLE `message_items`
  ADD COLUMN `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读' AFTER `msg_type`;

ALTER TABLE `message_sessions`
  ADD KEY `idx_user_session_key` (`user_id`, `session_key`),
  ADD KEY `idx_user_biz` (`user_id`, `biz_type`, `biz_id`);
