ALTER TABLE `payment_orders`
  ADD COLUMN `callback_status` varchar(20) DEFAULT 'PENDING' COMMENT '回调状态：PENDING-待回调，SUCCESS-成功，FAILED-失败',
  ADD COLUMN `callback_count` int(11) DEFAULT 0 COMMENT '回调次数',
  ADD COLUMN `last_callback_time` datetime DEFAULT NULL COMMENT '最后回调时间',
  ADD COLUMN `notify_url` varchar(255) DEFAULT NULL COMMENT '异步通知URL',
  ADD COLUMN `business_data` text COMMENT '业务扩展数据(JSON)';

