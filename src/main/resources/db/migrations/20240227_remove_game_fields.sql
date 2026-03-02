ALTER TABLE `users`
  DROP COLUMN `game_id`,
  DROP COLUMN `game_type`,
  DROP COLUMN `game_region`;

ALTER TABLE `users`
  DROP INDEX `uk_game_id`,
  DROP INDEX `idx_game_wechat`;

ALTER TABLE `user_game_accounts`
  DROP COLUMN `game_type`,
  DROP COLUMN `game_region`,
  DROP COLUMN `game_id`;

ALTER TABLE `user_game_accounts`
  DROP INDEX `uk_game_unique`;

ALTER TABLE `contracts`
  DROP COLUMN `initiator_game_id`,
  DROP COLUMN `receiver_game_id`,
  DROP COLUMN `game_type`,
  DROP COLUMN `game_region`;

ALTER TABLE `contracts`
  DROP INDEX `idx_game_info`;
