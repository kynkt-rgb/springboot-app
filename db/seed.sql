-- テストデータを入れる SQL
-- 使い方（springboot-app フォルダで）:
--   docker exec -i springboot-app-db psql -U app -d springboot_app < db/seed.sql
--
-- 何度流しても同じ状態になるように、いったん中身を全部消してから入れ直す。
-- RESTART IDENTITY で、タスクの番号（id）も 1 から振り直す。

TRUNCATE tasks RESTART IDENTITY;

INSERT INTO tasks (title, status, position) VALUES
  ('買い物',         'todo',  1),
  ('宿題',           'todo',  2),
  ('部屋のそうじ',   'doing', 1),
  ('本を返す',       'doing', 2),
  ('洗たく',         'done',  1),
  ('歯医者の予約',   'done',  2);
