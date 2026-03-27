-- Gold Rush Contract 初始化 DDL（MySQL 8.x）
-- 根据当前业务代码与历史脚本重整

SET NAMES utf8mb4;

-- 1. 用户表（users）
CREATE TABLE IF NOT EXISTS `users` (
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

-- 2. 会员套餐表（vip_packages）
CREATE TABLE IF NOT EXISTS `vip_packages` (
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

-- 3. 系统配置表（system_configs）
CREATE TABLE IF NOT EXISTS `system_configs` (
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

-- 3.1 系统默认配置初始化（与 SystemConfigService 默认值保持一致）
INSERT INTO `system_configs`
  (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `is_system`)
VALUES
  -- 通用配置
  ('app.name', '摸金小契约', 'STRING', 'COMMON', '应用名称', 1),
  ('app.version', '1.0.0', 'STRING', 'COMMON', '应用版本', 1),
  ('app.description', '游戏组队履约保障工具', 'STRING', 'COMMON', '应用描述', 1),
  ('app.copyright', '© 2024 摸金科技', 'STRING', 'COMMON', '版权信息', 1),

  -- 支付配置
  ('payment.service_fee_rate', '0.01', 'NUMBER', 'PAYMENT', '平台服务费率', 1),
  ('payment.default_deposit', '20', 'NUMBER', 'PAYMENT', '默认押金金额', 1),
  ('payment.max_deposit', '200', 'NUMBER', 'PAYMENT', '最大押金金额', 1),
  ('payment.min_deposit', '10', 'NUMBER', 'PAYMENT', '最小押金金额', 1),
  ('payment.penalty_fee_rate', '0.01', 'NUMBER', 'PAYMENT', '违约金平台手续费率', 1),
  ('payment.order_expire_minutes', '30', 'NUMBER', 'PAYMENT', '订单过期时间(分钟)', 1),
  ('payment.auto_refund_hours', '168', 'NUMBER', 'PAYMENT', '自动退款时间(小时)', 1),

  -- 信用配置
  ('credit.init_score', '100', 'NUMBER', 'CREDIT', '初始信用分', 1),
  ('credit.max_score', '100', 'NUMBER', 'CREDIT', '最高信用分', 1),
  ('credit.min_score', '0', 'NUMBER', 'CREDIT', '最低信用分', 1),
  ('credit.violation_deduct', '50', 'NUMBER', 'CREDIT', '违约扣分', 1),
  ('credit.complete_add', '10', 'NUMBER', 'CREDIT', '完成契约加分', 1),
  ('credit.dispute_win_add', '5', 'NUMBER', 'CREDIT', '争议胜诉加分', 1),
  ('credit.dispute_lose_deduct', '20', 'NUMBER', 'CREDIT', '争议败诉扣分', 1),
  ('credit.blacklist_threshold', '30', 'NUMBER', 'CREDIT', '黑名单阈值', 1),
  ('credit.recover_days', '7', 'NUMBER', 'CREDIT', '信用分恢复周期(天)', 1),
  ('credit.recover_points', '5', 'NUMBER', 'CREDIT', '每次恢复点数', 1),

  -- 契约配置
  ('contract.dispute_timeout_hours', '24', 'NUMBER', 'CONTRACT', '争议举证超时时间(小时)', 1),
  ('contract.auto_complete_hours', '72', 'NUMBER', 'CONTRACT', '自动完成时间(小时)', 1),
  ('contract.auto_cancel_minutes', '30', 'NUMBER', 'CONTRACT', '自动取消时间(分钟)', 1),
  ('contract.max_active_contracts', '3', 'NUMBER', 'CONTRACT', '最大同时进行契约数', 1),
  ('contract.min_guarantee_length', '5', 'NUMBER', 'CONTRACT', '保底物品最小长度', 1),
  ('contract.max_guarantee_length', '100', 'NUMBER', 'CONTRACT', '保底物品最大长度', 1),

  -- VIP配置
  ('vip.month_price', '9.9', 'NUMBER', 'VIP', '月卡价格', 1),
  ('vip.quarter_price', '29.7', 'NUMBER', 'VIP', '季卡价格', 1),
  ('vip.year_price', '99.0', 'NUMBER', 'VIP', '年卡价格', 1),
  ('vip.trial_days', '3', 'NUMBER', 'VIP', '试用天数', 1),
  ('vip.max_contract_limit', '-1', 'NUMBER', 'VIP', '最大契约限制(-1为无限)', 1),
  ('vip.free_service_fee', 'true', 'BOOLEAN', 'VIP', '免服务费', 1),
  ('vip.special_card_style', 'true', 'BOOLEAN', 'VIP', '专属卡片样式', 1),
  ('vip.priority_support', 'true', 'BOOLEAN', 'VIP', '优先客服支持', 1),

  -- 安全配置
  ('security.token_expire_hours', '168', 'NUMBER', 'SECURITY', 'Token过期时间(小时)', 1),
  ('security.max_login_attempts', '5', 'NUMBER', 'SECURITY', '最大登录尝试次数', 1),
  ('security.login_lock_minutes', '30', 'NUMBER', 'SECURITY', '登录锁定时间(分钟)', 1),
  ('security.password_min_length', '6', 'NUMBER', 'SECURITY', '密码最小长度', 1),
  ('security.enable_captcha', 'true', 'BOOLEAN', 'SECURITY', '启用验证码', 1),
  ('security.require_realname', 'false', 'BOOLEAN', 'SECURITY', '要求实名认证', 1),

  -- 运营配置
  ('operation.customer_service_qq', '123456789', 'STRING', 'OPERATION', '客服QQ', 1),
  ('operation.customer_service_phone', '400-123-4567', 'STRING', 'OPERATION', '客服电话', 1),
  ('operation.official_group_url', 'https://qq.com/group', 'STRING', 'OPERATION', '官方QQ群链接', 1),
  ('operation.mini_program_name', '摸金小契约', 'STRING', 'OPERATION', '小程序名称', 1),
  ('operation.company_name', '摸金科技有限公司', 'STRING', 'OPERATION', '公司名称', 1),
  ('operation.icp_number', '京ICP备12345678号', 'STRING', 'OPERATION', 'ICP备案号', 1),
  ('operation.privacy_policy_url', 'https://goldrush.com/privacy', 'STRING', 'OPERATION', '隐私政策链接', 1),
  ('operation.user_agreement_url', 'https://goldrush.com/agreement', 'STRING', 'OPERATION', '用户协议链接', 1),

  -- 通知配置
  ('notification.enable_sms', 'false', 'BOOLEAN', 'NOTIFICATION', '启用短信通知', 1),
  ('notification.enable_email', 'false', 'BOOLEAN', 'NOTIFICATION', '启用邮件通知', 1),
  ('notification.enable_wechat', 'true', 'BOOLEAN', 'NOTIFICATION', '启用微信通知', 1),
  ('notification.template_contract_created', '您有一个新的契约待处理', 'STRING', 'NOTIFICATION', '契约创建通知模板', 1),
  ('notification.template_contract_completed', '您的契约已完成', 'STRING', 'NOTIFICATION', '契约完成通知模板', 1),
  ('notification.template_payment_success', '支付成功通知', 'STRING', 'NOTIFICATION', '支付成功通知模板', 1),
  ('notification.template_dispute_created', '您有一个新的争议待处理', 'STRING', 'NOTIFICATION', '争议创建通知模板', 1),

  -- 第三方配置
  ('third_party.wechat_app_id', 'wx1234567890abcdef', 'STRING', 'THIRD_PARTY', '微信AppId', 1),
  ('third_party.wechat_app_secret', 'your_app_secret', 'STRING', 'THIRD_PARTY', '微信AppSecret', 1),
  ('third_party.wechat_mch_id', '1234567890', 'STRING', 'THIRD_PARTY', '微信商户号', 1),
  ('third_party.qiniu_access_key', 'your_qiniu_access_key', 'STRING', 'THIRD_PARTY', '七牛AccessKey', 1),
  ('third_party.qiniu_secret_key', 'your_qiniu_secret_key', 'STRING', 'THIRD_PARTY', '七牛SecretKey', 1),
  ('third_party.qiniu_bucket', 'goldrush', 'STRING', 'THIRD_PARTY', '七牛存储桶', 1),
  ('third_party.qiniu_domain', 'https://cdn.goldrush.com', 'STRING', 'THIRD_PARTY', '七牛域名', 1),
  ('third_party.aliyun_sms_access_key', 'your_aliyun_access_key', 'STRING', 'THIRD_PARTY', '阿里云短信AccessKey', 1),
  ('third_party.aliyun_sms_secret_key', 'your_aliyun_secret_key', 'STRING', 'THIRD_PARTY', '阿里云短信SecretKey', 1);

-- 4. 契约表（contracts）
CREATE TABLE IF NOT EXISTS `contracts` (
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
  KEY `idx_status_time` (`status`, `create_time`),
  CONSTRAINT `fk_contract_initiator` FOREIGN KEY (`initiator_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_contract_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约表';

-- 5. 用户游戏账号表（user_game_accounts）
CREATE TABLE IF NOT EXISTS `user_game_accounts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `game_name` varchar(50) NOT NULL COMMENT '游戏名称',
  `game_uid` varchar(100) NOT NULL COMMENT '游戏账号ID',
  `game_nickname` varchar(100) NOT NULL COMMENT '游戏昵称',
  `remark` varchar(100) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `fk_game_account_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户游戏账号表';

-- 6. 契约确认表（contract_confirms）
CREATE TABLE IF NOT EXISTS `contract_confirms` (
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
  KEY `idx_contract` (`contract_id`),
  CONSTRAINT `fk_confirm_contract` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_confirm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='契约确认表';

-- 7. 支付订单表（payment_orders）
CREATE TABLE IF NOT EXISTS `payment_orders` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单号',
  `contract_id` varchar(32) NOT NULL COMMENT '关联契约ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `order_type` varchar(20) NOT NULL COMMENT '订单类型：SERVICE_FEE/DEPOSIT_FREEZE/DEPOSIT_DEDUCT/DEPOSIT_UNFREEZE/VIP_PAYMENT/ARBITRATION_FEE',

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

  -- 回调与业务扩展
  `callback_status` varchar(20) DEFAULT 'PENDING' COMMENT '回调状态：PENDING/SUCCESS/FAILED',
  `callback_count` int(11) DEFAULT 0 COMMENT '回调次数',
  `last_callback_time` datetime DEFAULT NULL COMMENT '最后回调时间',
  `notify_url` varchar(255) DEFAULT NULL COMMENT '异步通知URL',
  `business_data` text COMMENT '业务扩展数据(JSON)',

  -- 状态
  `payment_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
  `refund_status` varchar(20) DEFAULT 'NONE' COMMENT '退款状态',
  `is_settled` tinyint(1) DEFAULT 0 COMMENT '是否已结算',

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
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_payment_contract` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

-- 8. 仲裁表（disputes）
CREATE TABLE IF NOT EXISTS `disputes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dispute_no` varchar(50) NOT NULL COMMENT '仲裁编号',
  `contract_id` varchar(32) NOT NULL COMMENT '关联契约ID',

  -- 申请信息
  `applicant_id` bigint(20) NOT NULL COMMENT '申请人ID',
  `respondent_id` bigint(20) NOT NULL COMMENT '被申请人ID',
  `applicant_role` varchar(20) NOT NULL COMMENT '申请人角色：INITIATOR/RECEIVER',
  `dispute_type` varchar(20) NOT NULL COMMENT '争议类型：VIOLATION/FRAUD/OTHER',

  -- 证据材料
  `description` text NOT NULL COMMENT '争议描述',
  `evidence_urls` text COMMENT '证据链接JSON数组',
  `game_screenshot_urls` text COMMENT '游戏截图JSON数组',
  `video_links` text COMMENT '录屏链接JSON数组',

  -- 处理信息
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/RESOLVED/CLOSED',
  `result` varchar(20) DEFAULT NULL COMMENT '仲裁结果：APPLICANT_WIN/RESPONDENT_WIN/DISMISSED',
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
  KEY `idx_status_time` (`status`, `create_time`),
  CONSTRAINT `fk_dispute_contract` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_dispute_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_dispute_respondent` FOREIGN KEY (`respondent_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_dispute_urgent_order` FOREIGN KEY (`urgent_fee_order_id`) REFERENCES `payment_orders` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仲裁表';

-- 9. 信用分历史表（credit_history）
CREATE TABLE IF NOT EXISTS `credit_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `change_type` varchar(20) NOT NULL COMMENT '变动类型：INIT/CONTRACT_COMPLETE/VIOLATION/DISPUTE_WIN/DISPUTE_LOSE/MANUAL_ADJUST/RECHARGE',

  -- 变动信息
  `change_amount` int(11) NOT NULL COMMENT '变动值（正负）',
  `before_score` int(11) NOT NULL COMMENT '变动前分数',
  `after_score` int(11) NOT NULL COMMENT '变动后分数',

  -- 关联信息
  `related_id` varchar(50) DEFAULT NULL COMMENT '关联ID（契约ID/仲裁ID）',
  `related_type` varchar(20) DEFAULT NULL COMMENT '关联类型：CONTRACT/DISPUTE',
  `description` varchar(500) NOT NULL COMMENT '变动描述',

  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `create_time`),
  KEY `idx_related` (`related_type`, `related_id`),
  CONSTRAINT `fk_credit_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分历史表';

-- 10. 摸金值流水（mojin_ledger）
CREATE TABLE IF NOT EXISTS `mojin_ledger` (
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
  KEY `idx_user_time` (`user_id`, `create_time`),
  CONSTRAINT `fk_mojin_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摸金值流水';

-- 11. 签到规则（checkin_rules）
CREATE TABLE IF NOT EXISTS `checkin_rules` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `day_index` int(11) NOT NULL COMMENT '天数序号(1-7)',
  `reward_amount` decimal(12,2) NOT NULL COMMENT '奖励摸金值',
  `is_mystery` tinyint(1) DEFAULT 0 COMMENT '是否神秘奖励',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_day` (`day_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到规则';

-- 12. 用户签到记录（user_checkins）
CREATE TABLE IF NOT EXISTS `user_checkins` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `checkin_date` date NOT NULL COMMENT '签到日期',
  `reward_amount` decimal(12,2) NOT NULL COMMENT '奖励摸金值',
  `continuous_days` int(11) NOT NULL DEFAULT 1 COMMENT '连续天数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `checkin_date`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到记录';

-- 13. 充值套餐（credit_packages）
CREATE TABLE IF NOT EXISTS `credit_packages` (
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
CREATE TABLE IF NOT EXISTS `message_sessions` (
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
  KEY `idx_user_time` (`user_id`, `update_time`),
  CONSTRAINT `fk_msg_session_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息会话';

-- 15. 站内消息明细（message_items）
CREATE TABLE IF NOT EXISTS `message_items` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint(20) NOT NULL COMMENT '会话ID',
  `sender_id` bigint(20) DEFAULT NULL COMMENT '发送者ID',
  `content` text NOT NULL COMMENT '内容',
  `msg_type` varchar(20) DEFAULT 'TEXT' COMMENT '消息类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_time` (`session_id`, `create_time`),
  CONSTRAINT `fk_msg_item_session` FOREIGN KEY (`session_id`) REFERENCES `message_sessions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_msg_item_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息明细';

-- 16. 信誉排行快照（credit_rank_snapshots）
CREATE TABLE IF NOT EXISTS `credit_rank_snapshots` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `snapshot_date` date NOT NULL COMMENT '榜单日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信誉排行快照';

-- 17. 信誉排行明细（credit_rank_items）
CREATE TABLE IF NOT EXISTS `credit_rank_items` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `snapshot_id` bigint(20) NOT NULL COMMENT '快照ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `score` int(11) NOT NULL COMMENT '信誉分',
  `rank_no` int(11) NOT NULL COMMENT '排名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_user` (`snapshot_id`, `user_id`),
  KEY `idx_snapshot_rank` (`snapshot_id`, `rank_no`),
  CONSTRAINT `fk_rank_snapshot` FOREIGN KEY (`snapshot_id`) REFERENCES `credit_rank_snapshots` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rank_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信誉排行明细';
