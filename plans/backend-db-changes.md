# 后端字段与数据库变更说明（对齐new-front页面）

## 变更目标
- 覆盖新前端核心页面数据需求（合约大厅/详情/创建、个人中心、仲裁、充值、排行榜、消息）
- MySQL 全量初始化脚本已输出：[`src/main/resources/db/init.sql`](src/main/resources/db/init.sql:1)

## 数据库表新增/扩展
### users
- 新增：`mojin_balance`, `mojin_locked`（摸金值余额/锁定）
- 关联文件：[`src/main/java/net/coding/template/entity/po/User.java`](src/main/java/net/coding/template/entity/po/User.java:1)

### contracts
- 新增：`min_credit`, `requirements`, `description`, `cover_url`
- 关联文件：[`src/main/java/net/coding/template/entity/po/Contract.java`](src/main/java/net/coding/template/entity/po/Contract.java:1)

### payment_orders
- 新增：`pay_channel`（支付渠道）
- 关联文件：[`src/main/java/net/coding/template/entity/po/PaymentOrder.java`](src/main/java/net/coding/template/entity/po/PaymentOrder.java:1)

### user_game_accounts
- 新增：`game_name`, `game_uid`, `game_nickname`
- 关联文件：[`src/main/java/net/coding/template/entity/po/UserGameAccount.java`](src/main/java/net/coding/template/entity/po/UserGameAccount.java:1)

### 新增表
- `mojin_ledger` 摸金值流水
- `checkin_rules` 签到规则
- `user_checkins` 用户签到记录
- `credit_packages` 充值套餐
- `message_sessions` 站内消息会话
- `message_items` 站内消息明细
- `credit_rank_snapshots` 信誉排行快照
- `credit_rank_items` 信誉排行明细

## 后端接口与DTO更新
### 合约创建
- 入参扩展：`minCredit`, `requirements`, `description`, `coverUrl`
- 关联文件：[`src/main/java/net/coding/template/entity/request/ContractCreateRequest.java`](src/main/java/net/coding/template/entity/request/ContractCreateRequest.java:1)
- 逻辑更新：[`src/main/java/net/coding/template/service/ContractService.java`](src/main/java/net/coding/template/service/ContractService.java:73)

### 合约详情/列表
- DTO扩展：`minCredit`, `requirements`, `description`, `coverUrl`
- 关联文件：[`src/main/java/net/coding/template/entity/dto/ContractDetailDTO.java`](src/main/java/net/coding/template/entity/dto/ContractDetailDTO.java:1)
- 列表响应扩展：[`src/main/java/net/coding/template/entity/response/ContractListResponse.java`](src/main/java/net/coding/template/entity/response/ContractListResponse.java:1)

### 用户资料
- DTO扩展：`mojinBalance`, `mojinLocked`
- 关联文件：[`src/main/java/net/coding/template/entity/dto/UserProfileDTO.java`](src/main/java/net/coding/template/entity/dto/UserProfileDTO.java:1)
- 服务赋值：[`src/main/java/net/coding/template/service/UserService.java`](src/main/java/net/coding/template/service/UserService.java:101)

### 支付
- 入参扩展：`payMethod`
- 关联文件：[`src/main/java/net/coding/template/entity/request/PaymentRequest.java`](src/main/java/net/coding/template/entity/request/PaymentRequest.java:1)
- 支付渠道落库：[`src/main/java/net/coding/template/service/PaymentService.java`](src/main/java/net/coding/template/service/PaymentService.java:70)

### 游戏账号
- 入参扩展：`gameName`, `gameUid`, `gameNickname`
- 关联文件：[`src/main/java/net/coding/template/entity/request/UserGameAccountCreateRequest.java`](src/main/java/net/coding/template/entity/request/UserGameAccountCreateRequest.java:1)
- 更新逻辑：[`src/main/java/net/coding/template/service/UserGameAccountService.java`](src/main/java/net/coding/template/service/UserGameAccountService.java:28)
- 响应扩展：[`src/main/java/net/coding/template/entity/response/UserGameAccountResponse.java`](src/main/java/net/coding/template/entity/response/UserGameAccountResponse.java:1)

## 初始化脚本
- 完整DROP/CREATE/基础数据：[`src/main/resources/db/init.sql`](src/main/resources/db/init.sql:1)

## 说明
- 基础数据包含：VIP套餐、系统配置、充值套餐、签到规则
- 新表与新增字段已在 `init.sql` 中覆盖
