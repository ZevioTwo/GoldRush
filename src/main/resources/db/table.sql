-- 摸金小契约数据库设计 V2.0
-- 根据需求文档重新设计

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
                              `remark` varchar(100) DEFAULT NULL COMMENT '备注',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`),
                              KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户游戏账号表';

-- 3. 契约表（contracts）- 核心业务表
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
                             `guarantee_item` varchar(100) DEFAULT NULL COMMENT '保底物品，如：保底出红',
                             `success_condition` varchar(500) DEFAULT NULL COMMENT '成功条件描述',
                             `failure_condition` varchar(500) DEFAULT NULL COMMENT '失败条件描述',

    -- 状态流转
                             `status` varchar(20) NOT NULL COMMENT '状态：PENDING-待支付，PAID-已支付，ACTIVE-进行中，COMPLETED-已完成，DISPUTE-争议中，CANCELLED-已取消，VIOLATED-已违约',
                             `phase` varchar(20) DEFAULT NULL COMMENT '当前阶段：PREPARE-准备中，IN_GAME-游戏中，SETTLEMENT-结算中',

    -- 时间节点
                             `start_time` datetime DEFAULT NULL COMMENT '游戏开始时间',
                             `end_time` datetime DEFAULT NULL COMMENT '游戏结束时间',
                             `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
                             `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
                             `violate_time` datetime DEFAULT NULL COMMENT '违约时间',

    -- 支付相关
                             `payment_status` varchar(20) DEFAULT NULL COMMENT '支付状态：UNPAID-未支付，PARTIAL-部分支付，FULL-全支付',
                             `freeze_status` varchar(20) DEFAULT NULL COMMENT '冻结状态：NONE-未冻结，INITIATOR_FROZEN-发起人已冻结，BOTH_FROZEN-双方已冻结',
                             `refund_status` varchar(20) DEFAULT NULL COMMENT '退款状态：NONE-无退款，PARTIAL-部分退款，FULL-全额退款',

                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_contract_no` (`contract_no`),
                             KEY `idx_initiator` (`initiator_id`, `status`),
                             KEY `idx_receiver` (`receiver_id`, `status`),
                             KEY `idx_create_time` (`create_time`),
                             KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约表';

-- 3. 支付订单表（payment_orders）- 资金流转核心表
CREATE TABLE `payment_orders` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `order_no` varchar(50) NOT NULL COMMENT '订单号，如：PAY-20240120-001',
                                  `contract_id` varchar(32) NOT NULL COMMENT '关联契约ID',
                                  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                  `order_type` varchar(20) NOT NULL COMMENT '订单类型：SERVICE_FEE-服务费，DEPOSIT_FREEZE-押金冻结，DEPOSIT_DEDUCT-押金扣除，DEPOSIT_UNFREEZE-押金解冻，VIP_PAYMENT-VIP支付，ARBITRATION_FEE-仲裁费',

    -- 金额信息
                                  `amount` decimal(10,2) NOT NULL COMMENT '订单金额',
                                  `actual_amount` decimal(10,2) DEFAULT NULL COMMENT '实际支付/扣除金额',
                                  `fee_rate` decimal(5,4) DEFAULT 0.006 COMMENT '支付费率',
                                  `fee_amount` decimal(10,2) DEFAULT 0.00 COMMENT '手续费',

    -- 支付信息
                                  `payment_method` varchar(20) DEFAULT 'WECHAT' COMMENT '支付方式：WECHAT-微信支付',
                                  `wx_prepay_id` varchar(100) DEFAULT NULL COMMENT '微信预支付ID',
                                  `wx_transaction_id` varchar(100) DEFAULT NULL COMMENT '微信支付流水号',
                                  `wx_out_trade_no` varchar(100) DEFAULT NULL COMMENT '微信商户订单号',

    -- 冻结/解冻信息
                                  `freeze_contract_id` varchar(100) DEFAULT NULL COMMENT '微信资金授权协议号',
                                  `freeze_transaction_id` varchar(100) DEFAULT NULL COMMENT '微信冻结流水号',
                                  `unfreeze_transaction_id` varchar(100) DEFAULT NULL COMMENT '微信解冻流水号',

    -- 状态
                              `payment_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING-待支付，SUCCESS-成功，FAILED-失败，CLOSED-已关闭',
                              `refund_status` varchar(20) DEFAULT 'NONE' COMMENT '退款状态：NONE-无退款，PARTIAL-部分退款，FULL-全额退款',
                              `is_settled` tinyint(1) DEFAULT 0 COMMENT '是否已结算给商户',
                              `callback_status` varchar(20) DEFAULT 'PENDING' COMMENT '回调状态：PENDING-待回调，SUCCESS-成功，FAILED-失败',
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

-- 4. 仲裁表（disputes）- 争议处理
CREATE TABLE `disputes` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `dispute_no` varchar(50) NOT NULL COMMENT '仲裁编号，如：ARB-20240120-001',
                            `contract_id` varchar(32) NOT NULL COMMENT '关联契约ID',

    -- 申请信息
                            `applicant_id` bigint(20) NOT NULL COMMENT '申请人ID',
                            `respondent_id` bigint(20) NOT NULL COMMENT '被申请人ID',
                            `applicant_role` varchar(20) NOT NULL COMMENT '申请人角色：INITIATOR-发起人，RECEIVER-接收人',
                            `dispute_type` varchar(20) NOT NULL COMMENT '争议类型：VIOLATION-违约，FRAUD-欺诈，OTHER-其他',

    -- 证据材料
                            `description` text NOT NULL COMMENT '争议描述',
                            `evidence_urls` text COMMENT '证据链接，JSON数组',
                            `game_screenshot_urls` text COMMENT '游戏结算截图，JSON数组',
                            `video_links` text COMMENT '录屏链接，JSON数组',

    -- 处理信息
                            `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理，PROCESSING-处理中，RESOLVED-已解决，CLOSED-已关闭',
                            `result` varchar(20) DEFAULT NULL COMMENT '仲裁结果：APPLICANT_WIN-申请人胜诉，RESPONDENT_WIN-被申请人胜诉，DISMISSED-驳回',
                            `result_reason` varchar(500) DEFAULT NULL COMMENT '仲裁理由',

    -- 加急处理
                            `is_urgent` tinyint(1) DEFAULT 0 COMMENT '是否加急',
                            `urgent_fee_paid` tinyint(1) DEFAULT 0 COMMENT '是否已付加急费',
                            `urgent_fee_order_id` bigint(20) DEFAULT NULL COMMENT '加急费订单ID',

    -- 处理人信息
                            `handler_id` bigint(20) DEFAULT NULL COMMENT '处理人ID（后台管理员）',
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

-- 5. 信用分历史表（credit_history）- 用户信用记录
CREATE TABLE `credit_history` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                  `change_type` varchar(20) NOT NULL COMMENT '变动类型：INIT-初始，CONTRACT_COMPLETE-契约完成，VIOLATION-违约，DISPUTE_WIN-争议胜诉，DISPUTE_LOSE-争议败诉，MANUAL_ADJUST-手动调整',

    -- 变动信息
                                  `change_amount` int(11) NOT NULL COMMENT '变动值（正负）',
                                  `before_score` int(11) NOT NULL COMMENT '变动前分数',
                                  `after_score` int(11) NOT NULL COMMENT '变动后分数',

    -- 关联信息
                                  `related_id` varchar(50) DEFAULT NULL COMMENT '关联ID（契约ID/仲裁ID）',
                                  `related_type` varchar(20) DEFAULT NULL COMMENT '关联类型：CONTRACT-契约，DISPUTE-仲裁',
                                  `description` varchar(500) NOT NULL COMMENT '变动描述',

                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                  PRIMARY KEY (`id`),
                                  KEY `idx_user` (`user_id`, `create_time`),
                                  KEY `idx_related` (`related_type`, `related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分历史表';

-- 6. 会员套餐表（vip_packages）- VIP产品配置
CREATE TABLE `vip_packages` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `package_code` varchar(50) NOT NULL COMMENT '套餐编码',
                                `package_name` varchar(100) NOT NULL COMMENT '套餐名称',
                                `period_type` varchar(20) NOT NULL COMMENT '周期类型：MONTH-月，QUARTER-季，YEAR-年',
                                `period_value` int(11) NOT NULL COMMENT '周期值',

    -- 价格信息
                                `original_price` decimal(10,2) NOT NULL COMMENT '原价',
                                `current_price` decimal(10,2) NOT NULL COMMENT '现价',
                                `discount_rate` decimal(5,2) DEFAULT NULL COMMENT '折扣率',

    -- 权益配置
                                `contract_limit` int(11) DEFAULT -1 COMMENT '每月契约次数限制，-1为无限',
                                `free_service_fee` tinyint(1) DEFAULT 1 COMMENT '免服务费',
                                `special_card_style` tinyint(1) DEFAULT 0 COMMENT '专属卡片样式',
                                `priority_support` tinyint(1) DEFAULT 0 COMMENT '优先客服支持',

    -- 状态
                                `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-上架，INACTIVE-下架，DELETED-删除',
                                `sort_order` int(11) DEFAULT 0 COMMENT '排序',

                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_package_code` (`package_code`),
                                KEY `idx_status` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员套餐表';

-- 7. 系统配置表（system_configs）- 参数配置
CREATE TABLE `system_configs` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `config_key` varchar(100) NOT NULL COMMENT '配置键',
                                  `config_value` text COMMENT '配置值',
                                  `config_type` varchar(20) DEFAULT 'STRING' COMMENT '配置类型：STRING-字符串，NUMBER-数字，BOOLEAN-布尔，JSON-JSON对象',
                                  `config_group` varchar(50) DEFAULT 'COMMON' COMMENT '配置分组：PAYMENT-支付，CREDIT-信用，CONTRACT-契约，VIP-VIP',
                                  `description` varchar(500) DEFAULT NULL COMMENT '配置描述',
                                  `is_system` tinyint(1) DEFAULT 0 COMMENT '是否系统内置',
                                  `ext_data` text COMMENT '扩展字段（JSON等）',

                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_config_key` (`config_key`),
                                  KEY `idx_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';


-- 契约确认表（contract_confirms）- 记录双方确认状态
CREATE TABLE `contract_confirms` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `contract_id` varchar(32) NOT NULL COMMENT '契约ID',
                                     `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                     `user_role` varchar(20) NOT NULL COMMENT '用户角色：INITIATOR-发起人，RECEIVER-接收人',
                                     `confirm_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '确认状态：PENDING-待确认，CONFIRMED-已确认，REJECTED-已拒绝',
                                     `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
                                     `confirm_ip` varchar(50) DEFAULT NULL COMMENT '确认IP',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_contract_user` (`contract_id`, `user_id`),
                                     KEY `idx_user_status` (`user_id`, `confirm_status`),
                                     KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约确认表';