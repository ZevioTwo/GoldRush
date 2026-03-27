-- Gold Rush Contract 初始化脚本（MySQL 8.x）
-- 包含DROP/CREATE与基础数据

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables (reverse dependency order)
DROP TABLE IF EXISTS message_items;
DROP TABLE IF EXISTS message_sessions;
DROP TABLE IF EXISTS credit_rank_items;
DROP TABLE IF EXISTS credit_rank_snapshots;
DROP TABLE IF EXISTS user_checkins;
DROP TABLE IF EXISTS checkin_rules;
DROP TABLE IF EXISTS mojin_ledger;
DROP TABLE IF EXISTS credit_packages;
DROP TABLE IF EXISTS contract_confirms;
DROP TABLE IF EXISTS credit_history;
DROP TABLE IF EXISTS disputes;
DROP TABLE IF EXISTS payment_orders;
DROP TABLE IF EXISTS contracts;
DROP TABLE IF EXISTS user_game_accounts;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS vip_packages;
DROP TABLE IF EXISTS system_configs;

-- 1. 用户表（users）
CREATE TABLE `users` (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                         `openid` varchar(100) NOT NULL COMMENT '微信openid，唯一标识',
                         `unionid` varchar(100) DEFAULT NULL COMMENT '微信unionid',
                         `nickname` varchar(100) DEFAULT '微信用户' COMMENT '昵称',
                         `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
                         `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                         `wechat_id` varchar(100) DEFAULT NULL COMMENT '微信号',

                         -- 信用体系
                         `credit_score` int(11) NOT NULL DEFAULT 100 COMMENT '信用分，初始100',
                         `mojin_balance` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '摸金值余额',
                         `mojin_locked` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '锁定摸金值',
                         `total_contracts` int(11) NOT NULL DEFAULT 0 COMMENT '总契约数',
                         `completed_contracts` int(11) NOT NULL DEFAULT 0 COMMENT '成功完成契约数',
                         `dispute_count` int(11) NOT NULL DEFAULT 0 COMMENT '发起争议次数',
                         `violation_count` int(11) NOT NULL DEFAULT 0 COMMENT '违约次数',

                         -- VIP系统
                         `is_vip` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否VIP会员',
                         `vip_type` varchar(20) DEFAULT NULL COMMENT '会员类型：MONTH/QUARTER/YEAR',
                         `vip_start_time` datetime DEFAULT NULL COMMENT 'VIP开始时间',
                         `vip_expire_time` datetime DEFAULT NULL COMMENT 'VIP到期时间',
                         `vip_contract_count` int(11) DEFAULT 0 COMMENT 'VIP期间契约数',

                         -- 安全与状态
                         `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常，FROZEN-冻结，BLACKLIST-黑名单',
                         `blacklist_reason` varchar(500) DEFAULT NULL COMMENT '拉黑原因',
                         `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                         `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',

                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_openid` (`openid`),
                         KEY `idx_credit_score` (`credit_score`),
                         KEY `idx_vip_expire` (`vip_expire_time`),
                         KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 用户游戏账号表（user_game_accounts）
CREATE TABLE `user_game_accounts` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                      `game_name` varchar(50) NOT NULL COMMENT '游戏名称',
                                      `game_uid` varchar(100) NOT NULL COMMENT '游戏账号ID',
                                      `game_nickname` varchar(100) NOT NULL COMMENT '游戏昵称',
                                      `remark` varchar(100) DEFAULT NULL COMMENT '备注',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户游戏账号表';

-- 3. 契约表（contracts）
CREATE TABLE `contracts` (
                             `id` varchar(32) NOT NULL COMMENT '契约ID，使用UUID生成',
                             `contract_no` varchar(50) NOT NULL COMMENT '契约编号，如：MJ-20240120-001',

                             -- 参与双方
                             `initiator_id` bigint(20) NOT NULL COMMENT '发起人ID',
                             `receiver_id` bigint(20) DEFAULT NULL COMMENT '接收人ID',

                             -- 契约条款
                             `deposit_amount` decimal(10,2) NOT NULL COMMENT '履约押金金额',
                             `service_fee_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '平台服务费',
                             `penalty_amount` decimal(10,2) DEFAULT NULL COMMENT '违约金金额',

                             -- 约定条件
                             `title` varchar(100) DEFAULT NULL COMMENT '契约标题',
                             `game_type` varchar(50) DEFAULT NULL COMMENT '游戏类型',
                             `guarantee_item` varchar(100) DEFAULT NULL COMMENT '保底物品',
                             `success_condition` varchar(500) DEFAULT NULL COMMENT '成功条件描述',
                             `failure_condition` varchar(500) DEFAULT NULL COMMENT '失败条件描述',
                             `min_credit` int(11) DEFAULT NULL COMMENT '最低信誉分要求',
                             `requirements` varchar(500) DEFAULT NULL COMMENT '契约要求',
                             `description` text COMMENT '详细描述',
                             `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图',

                             -- 状态流转
                             `status` varchar(20) NOT NULL COMMENT '状态：PENDING/PAID/ACTIVE/COMPLETED/DISPUTE/CANCELLED/VIOLATED',
                             `phase` varchar(20) DEFAULT NULL COMMENT '阶段：PREPARE/IN_GAME/SETTLEMENT',

                             -- 时间节点
                             `start_time` datetime DEFAULT NULL COMMENT '开始时间',
                             `end_time` datetime DEFAULT NULL COMMENT '结束时间',
                             `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
                             `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
                             `violate_time` datetime DEFAULT NULL COMMENT '违约时间',

                             -- 支付相关
                             `payment_status` varchar(20) DEFAULT NULL COMMENT '支付状态：UNPAID/PARTIAL/FULL',
                             `freeze_status` varchar(20) DEFAULT NULL COMMENT '冻结状态：NONE/INITIATOR_FROZEN/BOTH_FROZEN',
                             `refund_status` varchar(20) DEFAULT NULL COMMENT '退款状态：NONE/PARTIAL/FULL',

                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_contract_no` (`contract_no`),
                             KEY `idx_initiator` (`initiator_id`, `status`),
                             KEY `idx_receiver` (`receiver_id`, `status`),
                             KEY `idx_create_time` (`create_time`),
                             KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约表';

-- 4. 支付订单表（payment_orders）
CREATE TABLE `payment_orders` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `order_no` varchar(50) NOT NULL COMMENT '订单号',
                                  `contract_id` varchar(32) NOT NULL COMMENT '关联契约ID',
                                  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                  `order_type` varchar(20) NOT NULL COMMENT '订单类型',

                                  -- 金额信息
                                  `amount` decimal(10,2) NOT NULL COMMENT '订单金额',
                                  `actual_amount` decimal(10,2) DEFAULT NULL COMMENT '实际支付金额',
                                  `fee_rate` decimal(5,4) DEFAULT 0.006 COMMENT '费率',
                                  `fee_amount` decimal(10,2) DEFAULT 0.00 COMMENT '手续费',

                                  -- 支付信息
                                  `payment_method` varchar(20) DEFAULT 'WECHAT' COMMENT '支付方式',
                                  `pay_channel` varchar(20) DEFAULT NULL COMMENT '支付渠道：WECHAT/ALIPAY',
                                  `wx_prepay_id` varchar(100) DEFAULT NULL COMMENT '微信预支付ID',
                                  `wx_transaction_id` varchar(100) DEFAULT NULL COMMENT '微信支付流水号',
                                  `wx_out_trade_no` varchar(100) DEFAULT NULL COMMENT '微信商户订单号',

                                  -- 冻结/解冻信息
                                  `freeze_contract_id` varchar(100) DEFAULT NULL COMMENT '微信资金授权协议号',
                                  `freeze_transaction_id` varchar(100) DEFAULT NULL COMMENT '微信冻结流水号',
                                  `unfreeze_transaction_id` varchar(100) DEFAULT NULL COMMENT '微信解冻流水号',

                                  -- 状态
                                  `payment_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
                                  `refund_status` varchar(20) DEFAULT 'NONE' COMMENT '退款状态',
                                  `is_settled` tinyint(1) DEFAULT 0 COMMENT '是否已结算',
                                  `callback_status` varchar(20) DEFAULT 'PENDING' COMMENT '回调状态',
                                  `callback_count` int(11) DEFAULT 0 COMMENT '回调次数',
                                  `last_callback_time` datetime DEFAULT NULL COMMENT '最后回调时间',
                                  `notify_url` varchar(255) DEFAULT NULL COMMENT '异步通知URL',
                                  `business_data` text COMMENT '业务扩展数据(JSON)',

                                  -- 时间
                                  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
                                  `freeze_time` datetime DEFAULT NULL COMMENT '冻结时间',
                                  `unfreeze_time` datetime DEFAULT NULL COMMENT '解冻时间',
                                  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
                                  `expire_time` datetime DEFAULT NULL COMMENT '订单过期时间',

                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_order_no` (`order_no`),
                                  UNIQUE KEY `uk_wx_out_trade_no` (`wx_out_trade_no`),
                                  KEY `idx_contract_user` (`contract_id`, `user_id`),
                                  KEY `idx_user_status` (`user_id`, `payment_status`),
                                  KEY `idx_wx_transaction` (`wx_transaction_id`),
                                  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

-- 5. 仲裁表（disputes）
CREATE TABLE `disputes` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `dispute_no` varchar(50) NOT NULL COMMENT '仲裁编号',
                            `contract_id` varchar(32) NOT NULL COMMENT '关联契约ID',

                            -- 申请信息
                            `applicant_id` bigint(20) NOT NULL COMMENT '申请人ID',
                            `respondent_id` bigint(20) NOT NULL COMMENT '被申请人ID',
                            `applicant_role` varchar(20) NOT NULL COMMENT '申请人角色',
                            `dispute_type` varchar(20) NOT NULL COMMENT '争议类型',

                            -- 证据材料
                            `description` text NOT NULL COMMENT '争议描述',
                            `evidence_urls` text COMMENT '证据链接JSON数组',
                            `game_screenshot_urls` text COMMENT '游戏截图JSON数组',
                            `video_links` text COMMENT '录屏链接JSON数组',

                            -- 处理信息
                            `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
                            `result` varchar(20) DEFAULT NULL COMMENT '仲裁结果',
                            `result_reason` varchar(500) DEFAULT NULL COMMENT '仲裁理由',

                            -- 加急处理
                            `is_urgent` tinyint(1) DEFAULT 0 COMMENT '是否加急',
                            `urgent_fee_paid` tinyint(1) DEFAULT 0 COMMENT '是否已付加急费',
                            `urgent_fee_order_id` bigint(20) DEFAULT NULL COMMENT '加急费订单ID',

                            -- 处理人信息
                            `handler_id` bigint(20) DEFAULT NULL COMMENT '处理人ID',
                            `handle_time` datetime DEFAULT NULL COMMENT '处理时间',

                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_dispute_no` (`dispute_no`),
                            KEY `idx_contract` (`contract_id`),
                            KEY `idx_applicant` (`applicant_id`, `status`),
                            KEY `idx_respondent` (`respondent_id`, `status`),
                            KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仲裁表';

-- 6. 信用分历史表（credit_history）
CREATE TABLE `credit_history` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                  `change_type` varchar(20) NOT NULL COMMENT '变动类型',
                                  `change_amount` int(11) NOT NULL COMMENT '变动值',
                                  `before_score` int(11) NOT NULL COMMENT '变动前分数',
                                  `after_score` int(11) NOT NULL COMMENT '变动后分数',
                                  `related_id` varchar(50) DEFAULT NULL COMMENT '关联ID',
                                  `related_type` varchar(20) DEFAULT NULL COMMENT '关联类型',
                                  `description` varchar(500) NOT NULL COMMENT '变动描述',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_user` (`user_id`, `create_time`),
                                  KEY `idx_related` (`related_type`, `related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分历史表';

-- 7. 会员套餐表（vip_packages）
CREATE TABLE `vip_packages` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `package_code` varchar(50) NOT NULL COMMENT '套餐编码',
                                `package_name` varchar(100) NOT NULL COMMENT '套餐名称',
                                `period_type` varchar(20) NOT NULL COMMENT '周期类型',
                                `period_value` int(11) NOT NULL COMMENT '周期值',
                                `original_price` decimal(10,2) NOT NULL COMMENT '原价',
                                `current_price` decimal(10,2) NOT NULL COMMENT '现价',
                                `discount_rate` decimal(5,2) DEFAULT NULL COMMENT '折扣率',
                                `contract_limit` int(11) DEFAULT -1 COMMENT '每月契约次数限制，-1为无限',
                                `free_service_fee` tinyint(1) DEFAULT 1 COMMENT '免服务费',
                                `special_card_style` tinyint(1) DEFAULT 0 COMMENT '专属卡片样式',
                                `priority_support` tinyint(1) DEFAULT 0 COMMENT '优先客服支持',
                                `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
                                `sort_order` int(11) DEFAULT 0 COMMENT '排序',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_package_code` (`package_code`),
                                KEY `idx_status` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员套餐表';

-- 8. 系统配置表（system_configs）
CREATE TABLE `system_configs` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `config_key` varchar(100) NOT NULL COMMENT '配置键',
                                  `config_value` text COMMENT '配置值',
                                  `config_type` varchar(20) DEFAULT 'STRING' COMMENT '配置类型',
                                  `config_group` varchar(50) DEFAULT 'COMMON' COMMENT '配置分组',
                                  `description` varchar(500) DEFAULT NULL COMMENT '配置描述',
                                  `is_system` tinyint(1) DEFAULT 0 COMMENT '是否系统内置',
                                  `ext_data` text COMMENT '扩展字段',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_config_key` (`config_key`),
                                  KEY `idx_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 9. 契约确认表（contract_confirms）
CREATE TABLE `contract_confirms` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `contract_id` varchar(32) NOT NULL COMMENT '契约ID',
                                     `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                     `user_role` varchar(20) NOT NULL COMMENT '用户角色',
                                     `confirm_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '确认状态',
                                     `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
                                     `confirm_ip` varchar(50) DEFAULT NULL COMMENT '确认IP',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_contract_user` (`contract_id`, `user_id`),
                                     KEY `idx_user_status` (`user_id`, `confirm_status`),
                                     KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约确认表';

-- 10. 摸金值流水（mojin_ledger）
CREATE TABLE `mojin_ledger` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                `change_amount` decimal(12,2) NOT NULL COMMENT '变动值',
                                `before_balance` decimal(12,2) NOT NULL COMMENT '变动前余额',
                                `after_balance` decimal(12,2) NOT NULL COMMENT '变动后余额',
                                `change_type` varchar(50) NOT NULL COMMENT '变动类型',
                                `related_id` varchar(64) DEFAULT NULL COMMENT '关联ID',
                                `related_type` varchar(32) DEFAULT NULL COMMENT '关联类型',
                                `description` varchar(255) DEFAULT NULL COMMENT '描述',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摸金值流水';

-- 11. 签到规则（checkin_rules）
CREATE TABLE `checkin_rules` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `day_index` int(11) NOT NULL COMMENT '天数序号(1-7)',
                                 `reward_amount` decimal(12,2) NOT NULL COMMENT '奖励摸金值',
                                 `is_mystery` tinyint(1) DEFAULT 0 COMMENT '是否神秘奖励',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_day` (`day_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到规则';

-- 12. 用户签到记录（user_checkins）
CREATE TABLE `user_checkins` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                 `checkin_date` date NOT NULL COMMENT '签到日期',
                                 `reward_amount` decimal(12,2) NOT NULL COMMENT '奖励摸金值',
                                 `continuous_days` int(11) NOT NULL DEFAULT 1 COMMENT '连续天数',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_date` (`user_id`, `checkin_date`),
                                 KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到记录';

-- 13. 充值套餐（credit_packages）
CREATE TABLE `credit_packages` (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `package_code` varchar(50) NOT NULL COMMENT '套餐编码',
                                   `package_name` varchar(100) NOT NULL COMMENT '套餐名称',
                                   `points` int(11) NOT NULL COMMENT '信誉分',
                                   `price` decimal(10,2) NOT NULL COMMENT '售价',
                                   `bonus_points` int(11) NOT NULL DEFAULT 0 COMMENT '赠送分',
                                   `is_popular` tinyint(1) DEFAULT 0 COMMENT '是否热门',
                                   `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态',
                                   `sort_order` int(11) DEFAULT 0 COMMENT '排序',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_package_code` (`package_code`),
                                   KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值套餐';

-- 14. 站内消息会话（message_sessions）
CREATE TABLE `message_sessions` (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                   `peer_name` varchar(100) NOT NULL COMMENT '对话名称',
                                   `peer_avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
                                   `peer_tag` varchar(50) DEFAULT NULL COMMENT '标签',
                                   `last_message` varchar(500) DEFAULT NULL COMMENT '最后一条消息',
                                   `last_time` datetime DEFAULT NULL COMMENT '最后消息时间',
                                   `unread_count` int(11) NOT NULL DEFAULT 0 COMMENT '未读数',
                                   `highlight` tinyint(1) DEFAULT 0 COMMENT '高亮',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_time` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息会话';

-- 15. 站内消息明细（message_items）
CREATE TABLE `message_items` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `session_id` bigint(20) NOT NULL COMMENT '会话ID',
                                 `sender_id` bigint(20) DEFAULT NULL COMMENT '发送者ID',
                                 `content` text NOT NULL COMMENT '内容',
                                 `msg_type` varchar(20) DEFAULT 'TEXT' COMMENT '消息类型',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_session_time` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息明细';

-- 16. 信誉排行快照（credit_rank_snapshots）
CREATE TABLE `credit_rank_snapshots` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `snapshot_date` date NOT NULL COMMENT '榜单日期',
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信誉排行快照';

-- 17. 信誉排行明细（credit_rank_items）
CREATE TABLE `credit_rank_items` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `snapshot_id` bigint(20) NOT NULL COMMENT '快照ID',
                                     `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                     `score` int(11) NOT NULL COMMENT '信誉分',
                                     `rank_no` int(11) NOT NULL COMMENT '排名',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_snapshot_user` (`snapshot_id`, `user_id`),
                                     KEY `idx_snapshot_rank` (`snapshot_id`, `rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信誉排行明细';

SET FOREIGN_KEY_CHECKS = 1;

-- 基础数据
INSERT INTO `vip_packages` (`package_code`, `package_name`, `period_type`, `period_value`, `original_price`, `current_price`, `discount_rate`, `contract_limit`, `free_service_fee`, `special_card_style`, `priority_support`, `status`, `sort_order`) VALUES
('VIP_MONTH', '月度会员', 'MONTH', 1, 19.90, 9.90, 50.00, -1, 1, 1, 1, 'ACTIVE', 1),
('VIP_QUARTER', '季度会员', 'QUARTER', 3, 59.70, 29.70, 50.00, -1, 1, 1, 1, 'ACTIVE', 2),
('VIP_YEAR', '年度会员', 'YEAR', 12, 199.00, 99.00, 50.00, -1, 1, 1, 1, 'ACTIVE', 3);

INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_system`) VALUES
('credit.max_score', '1000', 'NUMBER', 'CREDIT', '信用分上限', 1),
('contract.max_active', '3', 'NUMBER', 'CONTRACT', '同时进行契约数量上限', 1),
('payment.service_fee_rate', '0.006', 'NUMBER', 'PAYMENT', '支付费率', 1),
('vip.free_service_fee', 'true', 'BOOLEAN', 'VIP', 'VIP免服务费', 1);

INSERT INTO `credit_packages` (`package_code`, `package_name`, `points`, `price`, `bonus_points`, `is_popular`, `status`, `sort_order`) VALUES
('CREDIT_100', '初级契约包', 100, 100.00, 5, 0, 'ACTIVE', 1),
('CREDIT_500', '专业老板包', 500, 488.00, 30, 1, 'ACTIVE', 2),
('CREDIT_1000', '顶奢打手包', 1000, 958.00, 80, 0, 'ACTIVE', 3),
('CREDIT_2000', '荣耀金牌包', 2000, 1888.00, 200, 0, 'ACTIVE', 4);

INSERT INTO `checkin_rules` (`day_index`, `reward_amount`, `is_mystery`) VALUES
(1, 2.00, 0),
(2, 4.00, 0),
(3, 6.00, 0),
(4, 8.00, 0),
(5, 10.00, 0),
(6, 12.00, 0),
(7, 0.00, 1);
