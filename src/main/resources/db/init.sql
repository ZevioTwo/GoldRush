-- 1. 插入默认VIP套餐
INSERT INTO `vip_packages` (
    `package_code`, `package_name`, `period_type`, `period_value`,
    `original_price`, `current_price`, `discount_rate`,
    `contract_limit`, `free_service_fee`, `special_card_style`, `priority_support`,
    `status`, `sort_order`
) VALUES
      ('VIP_MONTH', '月卡会员', 'MONTH', 1, 15.00, 9.90, 0.66, -1, 1, 1, 0, 'ACTIVE', 1),
      ('VIP_QUARTER', '季卡会员', 'QUARTER', 3, 45.00, 29.70, 0.66, -1, 1, 1, 1, 'ACTIVE', 2),
      ('VIP_YEAR', '年卡会员', 'YEAR', 12, 180.00, 99.00, 0.55, -1, 1, 1, 1, 'ACTIVE', 3);

-- 2. 插入系统配置
INSERT INTO `system_configs` (
    `config_key`, `config_value`, `config_type`, `config_group`, `description`
) VALUES
      -- 支付配置
      ('payment.service_fee_rate', '0.01', 'NUMBER', 'PAYMENT', '平台服务费率'),
      ('payment.deposit_freeze_amount', '20', 'NUMBER', 'PAYMENT', '默认押金冻结金额'),
      ('payment.max_deposit_amount', '200', 'NUMBER', 'PAYMENT', '最大押金金额'),
      ('payment.penalty_fee_rate', '0.01', 'NUMBER', 'PAYMENT', '违约金平台手续费率'),

      -- 信用配置
      ('credit.init_score', '100', 'NUMBER', 'CREDIT', '初始信用分'),
      ('credit.violation_deduct', '50', 'NUMBER', 'CREDIT', '违约扣分'),
      ('credit.complete_add', '10', 'NUMBER', 'CREDIT', '完成契约加分'),
      ('credit.blacklist_threshold', '30', 'NUMBER', 'CREDIT', '黑名单阈值'),

      -- 契约配置
      ('contract.dispute_timeout_hours', '24', 'NUMBER', 'CONTRACT', '争议举证超时时间(小时)'),
      ('contract.auto_complete_hours', '72', 'NUMBER', 'CONTRACT', '自动完成时间(小时)'),

      -- 运营配置
      ('operation.customer_service_qq', '123456789', 'STRING', 'OPERATION', '客服QQ'),
      ('operation.official_group_url', 'https://qq.com/group', 'STRING', 'OPERATION', '官方QQ群链接'),
      ('operation.mini_program_name', '摸金小契约', 'STRING', 'OPERATION', '小程序名称');