CREATE TABLE IF NOT EXISTS `bounty_posts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bounty_no` varchar(50) NOT NULL COMMENT '悬赏编号',
  `creator_user_id` bigint(20) NOT NULL COMMENT '发起人用户ID',
  `creator_credit_score` int(11) NOT NULL DEFAULT 0 COMMENT '发起人信誉分快照',
  `target_role_name` varchar(100) NOT NULL COMMENT '被悬赏角色名称',
  `title` varchar(100) NOT NULL COMMENT '悬赏标题',
  `reward_mojin` decimal(12,2) NOT NULL COMMENT '每位摸金校尉奖励摸金币',
  `total_reward_mojin` decimal(12,2) NOT NULL COMMENT '本次悬赏总计扣除摸金币',
  `game_type` varchar(50) NOT NULL COMMENT '游戏类型',
  `description` varchar(1000) NOT NULL COMMENT '悬赏描述',
  `recruit_target_count` int(11) NOT NULL COMMENT '目标招募摸金校尉数量',
  `recruit_current_count` int(11) NOT NULL DEFAULT 0 COMMENT '当前已接取数量',
  `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/FULL/CLOSED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bounty_no` (`bounty_no`),
  KEY `idx_status_credit_time` (`status`, `creator_credit_score`, `create_time`),
  KEY `idx_creator_time` (`creator_user_id`, `create_time`),
  KEY `idx_game_type` (`game_type`),
  CONSTRAINT `fk_bounty_creator` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏发布表';

CREATE TABLE IF NOT EXISTS `bounty_claims` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bounty_id` bigint(20) NOT NULL COMMENT '悬赏ID',
  `hunter_user_id` bigint(20) NOT NULL COMMENT '接取悬赏的摸金校尉用户ID',
  `status` varchar(20) NOT NULL DEFAULT 'CLAIMED' COMMENT '状态：CLAIMED/CANCELLED/COMPLETED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bounty_hunter` (`bounty_id`, `hunter_user_id`),
  KEY `idx_hunter_status` (`hunter_user_id`, `status`, `create_time`),
  CONSTRAINT `fk_bounty_claim_post` FOREIGN KEY (`bounty_id`) REFERENCES `bounty_posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_bounty_claim_user` FOREIGN KEY (`hunter_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏接取记录表';
