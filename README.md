核心API接口设计
   模块	接口	方法	功能
   用户模块	/api/user/login	POST	微信登录
   /api/user/profile	GET	获取用户信息
   /api/user/credit	GET	查询信用分
   契约模块	/api/contract/create	POST	创建契约
   /api/contract/list	GET	契约列表
   /api/contract/{id}	GET	契约详情
   /api/contract/confirm	POST	确认完成
   支付模块	/api/payment/prepay	POST	生成支付预订单
   /api/payment/notify	POST	微信支付回调
   /api/payment/freeze	POST	资金冻结
   /api/payment/unfreeze	POST	资金解冻
   /api/payment/deduct	POST	扣除违约金
   仲裁模块	/api/dispute/apply	POST	申请仲裁
   /api/dispute/submit	POST	提交证据
   /api/dispute/judge	POST	人工判责（后台）
   会员模块	/api/vip/subscribe	POST	开通会员
   /api/vip/benefits	GET	会员权益