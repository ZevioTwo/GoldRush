-- 主要表结构（简化版）
-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    openid VARCHAR(100) UNIQUE,
    nickname VARCHAR(50),
    credit_score INT DEFAULT 100,
    is_vip BOOLEAN DEFAULT false
);

-- 契约表
CREATE TABLE contracts (
    id VARCHAR(32) PRIMARY KEY,
    initiator_id BIGINT,  -- 发起人
    receiver_id BIGINT,   -- 接收人
    game_type VARCHAR(20),
    deposit_amount DECIMAL(10,2),  -- 押金金额
    service_fee DECIMAL(10,2),     -- 服务费
    status ENUM('pending','active','completed','dispute','canceled'),
    create_time DATETIME
);

-- 支付订单表（关键）
CREATE TABLE payment_orders (
    order_id VARCHAR(32) PRIMARY KEY,
    contract_id VARCHAR(32),
    user_id BIGINT,
    type ENUM('freeze','unfreeze','deduct','refund'),
    amount DECIMAL(10,2),
    wx_transaction_id VARCHAR(50),
    status ENUM('pending','success','failed')
);

-- 仲裁表
CREATE TABLE disputes (
    id BIGINT PRIMARY KEY,
    contract_id VARCHAR(32),
    applicant_id BIGINT,
    evidence_url TEXT,
    result ENUM('pending','initiator_win','receiver_win','dismissed')
);