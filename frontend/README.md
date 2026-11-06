# frontend（タスクボードの画面）

React 19 + Vite 8（JavaScript）で作った画面です。
バックエンドの `GET /api/tasks` を呼んで、タスクを「やること・進行中・完了」の3列で表示します。

アプリ全体の起動方法は [../README.md](../README.md)、技術とバージョンは [../docs/tech-stack.md](../docs/tech-stack.md) を見てください。

## コマンド（このフォルダで）
| コマンド | 内容 |
|---------|------|
| `npm install` | ライブラリを入れる（最初の1回と、`package.json` が変わったとき） |
| `npm run dev` | 開発用サーバーを 5173 番で起動する。保存するとすぐ画面に出る |
| `npm run lint` | 書き方のチェック（oxlint） |
| `npm run build` | 公開用のファイルを `dist/` に作る |

- バックエンド（8080番）が動いていないと、画面に「カードを読みこめませんでした：…」と出ます。
- 5173 番がふさがっていると、別の番号に逃げずにエラーで止まります（`vite.config.js` の `strictPort: true`）。そのときは `../.claude/hooks/free-port.sh 5173` で空けてから起動しなおします。

## ファイル
| ファイル | 内容 |
|---------|------|
| `vite.config.js` | ポート（5173）の固定と、`/api` をバックエンド（8080）に取り次ぐ設定 |
| `src/main.jsx` | 起動するところ |
| `src/App.jsx` | ボード画面。API を呼び、読みこみ中・エラー・表示を切りかえる |
| `src/Column.jsx` | 1列。見出し「やること（2）」とカード。0枚なら「カードはありません」 |
| `src/Card.jsx` | カード1枚（今はタイトルだけ） |
| `src/App.css` / `src/index.css` | 見た目 |

`node_modules/`（ライブラリ）と `dist/`（ビルド結果）は `npm install` / `npm run build` で作られるので、Git には入れません。
