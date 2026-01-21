-- 1. 清理过期数据（30天前的失败订单）
DELETE FROM `payment_orders`
WHERE `payment_status` = 'FAILED'
  AND `create_time` < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 2. 清理已完成的契约（90天前）
UPDATE `contracts`
SET `status` = 'ARCHIVED'
WHERE `status` = 'COMPLETED'
  AND `complete_time` < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 3. 自动更新VIP过期状态（每天执行）
UPDATE `users`
SET `is_vip` = 0, `vip_type` = NULL
WHERE `is_vip` = 1
  AND `vip_expire_time` < NOW();

-- 4. 生成日报表视图
CREATE VIEW `daily_report` AS
SELECT
        DATE(create_time) as report_date,
        COUNT(*) as total_contracts,
        SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_contracts,
        SUM(CASE WHEN status = 'VIOLATED' THEN 1 ELSE 0 END) as violated_contracts,
        SUM(CASE WHEN status = 'DISPUTE' THEN 1 ELSE 0 END) as dispute_contracts,
        SUM(deposit_amount) as total_deposit,
        SUM(service_fee_amount) as total_service_fee
        FROM `contracts`
        GROUP BY DATE(create_time);