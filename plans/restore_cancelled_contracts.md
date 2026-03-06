以下为一次性恢复所有已取消契约为可接取状态的SQL（请在生产执行前先备份）：

```sql
-- 一次性恢复所有已取消契约为可接取状态（PENDING且receiver_id为空）
UPDATE contracts
SET status = 'PENDING',
    receiver_id = NULL,
    cancel_time = NULL,
    update_time = NOW()
WHERE status = 'CANCELLED';
```
