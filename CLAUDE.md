# springboot-app の開発ルール

このリポジトリでは、次の流れを**必ず守る**こと。

## 作業の流れ

1. 作業の前に GitHub にイシューを作る（`gh issue create`）
   - ラベルは `feature` / `fix` / `docs` / `chore` のどれか
2. イシュー番号つきのブランチを main から切る
   - 名前: `<種類>/<イシュー番号>-<短い英語>` 例: `feature/3-task-api`
3. コミットメッセージにイシュー番号を書く 例: `Task API を追加 (#3)`
4. push して PR を作る。本文に `Closes #<番号>` を入れる（マージでイシューが自動で閉じる）
5. main への直接 commit / push はしない。force push もしない

## サーバーのポート（必ず守る）

| 動かすもの | ポート | 起動のしかた |
|-----------|-------|-------------|
| DB（Docker の PostgreSQL） | 5433 | `docker compose up -d`（springboot-app で） |
| バックエンド（Spring Boot） | 8080 | `./gradlew bootRun`（backend で） |
| フロントエンド（Vite + React） | 5173 | `npm run dev`（frontend で） |

- サーバーは**必ず上の決まったポートで**起動する。別のポートで一時的に起動しない（画面の取り次ぎ `/api` → 8080 などが合わなくなり、うまく動かないため）
- ポートがふさがっていたら、**そのポートを使っているプロセスを止めてから**、決まったポートで起動しなおす
  - 止め方: `.claude/hooks/free-port.sh 8080` または `.claude/hooks/free-port.sh 5173`
- `--port`・`--server.port`・`SERVER_PORT`・`PORT` などでポートを変えない。`vite.config.js` の `strictPort: true`（ふさがっていたら別の番号に逃げずに止まる）を外さない

## PR のマージ

- Claude は PR を作ってよいが、**マージはしない**。ユーザーが画面で動作確認してからマージする

## 守るしくみ

- `.claude/hooks/guard-git.sh`：main 上での `git commit`、main への `git push`、force push を止める
- `.claude/hooks/guard-port.sh`：サーバーを決まったポート（8080 / 5173）以外で起動するコマンドを止める
- `frontend/vite.config.js`：`port: 5173` と `strictPort: true` で、5173 番以外では動かない
- GitHub 側：main は PR 経由でしか変更できない（承認は不要。自分でマージしてよい）
