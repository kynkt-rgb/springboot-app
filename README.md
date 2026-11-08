# springboot-app（タスクボード）

「やること」「進行中」「完了」の3列でタスクを表示する Web アプリです。
スクール（RaiseTech）の Spring Boot 回の課題として、[task-board](https://github.com/kynkt-rgb/task-board) を Java + Spring Boot と React で作りなおしています。

- 技術とバージョン：[docs/tech-stack.md](docs/tech-stack.md)
- 開発ルール（イシュー → ブランチ → PR、ポートのルール）：[CLAUDE.md](CLAUDE.md)

## できること
| 機能 | 状態 |
|------|------|
| タスクを3列のボードで表示する | できた |
| 状態（todo / doing / done）でタスクを探す API | できた（画面からはまだ使っていない） |
| タスクの追加・移動・並び替え・削除 | これから |

## 使っている技術
| 区分 | 技術 |
|------|------|
| バックエンド | Java 21 / Spring Boot 4.1.2 / Spring Data JPA / Gradle 9.8.2 |
| フロントエンド | React 19.3 / Vite 8.3（JavaScript） |
| DB | PostgreSQL 17（Docker） |

くわしいバージョンは [docs/tech-stack.md](docs/tech-stack.md) にあります。

## しくみ
```
ブラウザ ──> フロントエンド（Vite, 5173番）
                │  /api で始まる呼び出しは取り次ぐ
                ▼
             バックエンド（Spring Boot, 8080番）
                │
                ▼
             DB（PostgreSQL, Docker, 5433番）
```

## フォルダの中身
```
springboot-app/
├── backend/                     バックエンド（Spring Boot）
│   ├── build.gradle             使う部品とバージョン
│   └── src/
│       ├── main/java/com/example/backend/
│       │   ├── BackendApplication.java  起動するところ
│       │   ├── Task.java                タスク1件の形（tasks 表）
│       │   ├── TaskRepository.java      DB の読み書き
│       │   ├── TaskController.java      タスクの API（受け取って TaskService に渡す）
│       │   ├── TaskService.java         タスクの仕事のルール（入力チェック・並び替え）
│       │   ├── HelloController.java     動作確認用（/api/hello）
│       │   └── DbCheckController.java   DB 接続の確認用（/api/db-check）
│       ├── main/resources/application.properties  DB の接続先などの設定
│       └── test/                自動テスト
├── frontend/                    フロントエンド（React + Vite）
│   ├── vite.config.js           ポート（5173）と /api の取り次ぎ
│   ├── index.html
│   └── src/
│       ├── main.jsx             起動するところ
│       ├── App.jsx              ボード画面（API を呼んで3列に分ける）
│       ├── Column.jsx           1列
│       ├── Card.jsx             カード1枚
│       ├── api.js               バックエンドを呼ぶ関数（fetch はここだけ）
│       ├── App.css / index.css  見た目
├── db/seed.sql                  テストデータ（6件）
├── compose.yaml                 Docker の PostgreSQL の設定
├── docs/tech-stack.md           技術スタック
├── CLAUDE.md                    開発ルール
└── .claude/                     Claude Code の設定（フック・スキル）
```

## 手元で動かす（Mac）

### 最初の1回だけ
```sh
# Java 21・Node.js を入れる（Docker Desktop は別に入れておく）
brew install openjdk@21 node

# フロントエンドのライブラリを入れる
cd frontend
npm install
```
`~/.zshrc` に Java 21 の `JAVA_HOME` と `PATH` を設定しておきます。

### 起動する（3つのターミナルで、上から順に）
```sh
# 1. DB を起動する（springboot-app フォルダで）
docker compose up -d

# 2. バックエンドを起動する（backend フォルダで）→ 8080番
./gradlew bootRun

# 3. フロントエンドを起動する（frontend フォルダで）→ 5173番
npm run dev
```
ブラウザで **http://localhost:5173** を開きます。
表 `tasks` は、バックエンドを起動したときに自動で作られます。

### テストデータを入れる
```sh
# springboot-app フォルダで。中身を全部消して、6件を入れなおす
docker exec -i springboot-app-db psql -U app -d springboot_app < db/seed.sql
```

### 止める
- バックエンド・フロントエンド：それぞれのターミナルで `Ctrl + C`
- DB：`docker compose down`（データは消えずに残る）

### ポートがふさがっていたら
別のポートでは起動せず、使っているプロセスを止めてから起動しなおします。
```sh
.claude/hooks/free-port.sh 8080   # バックエンドのポートを空ける
.claude/hooks/free-port.sh 5173   # フロントエンドのポートを空ける
```

### 自動テスト（バックエンド）
```sh
cd backend
./gradlew test
```

### 書き方チェック
```sh
# フロントエンド（frontend フォルダで）
npm run lint        # 直せるものを自動で直すときは npm run lint:fix

# バックエンド（backend フォルダで）。テストと一緒に動く
./gradlew check     # 結果は build/reports/checkstyle/main.html・test.html
```
ルールは `frontend/.oxlintrc.json` と `backend/config/checkstyle/checkstyle.xml`。いま出ている指摘は [docs/quality-review.md](docs/quality-review.md) の D にあります。

## 設定（環境変数）
何も設定しなければ、Docker の DB（5433番）につながります。

| 名前 | 初期値 | 内容 |
|------|--------|------|
| `DB_URL` | `jdbc:postgresql://localhost:5433/springboot_app` | DB の接続先 |
| `DB_USERNAME` | `app` | DB のユーザー名 |
| `DB_PASSWORD` | `app` | DB のパスワード（練習用） |

## API
データの形は JSON です。

| メソッド | URL | 内容 | 成功 | 主なエラー |
|---------|-----|------|------|-----------|
| GET | `/api/tasks` | 全部のタスク（並び順 `position` の小さい順） | 200 | － |
| GET | `/api/tasks?status=doing` | 状態（`todo` / `doing` / `done`）で絞ったタスク | 200 | 400（ほかの値） |
| GET | `/api/tasks/{id}` | 番号でタスク1件 | 200 | 404（無い番号） |
| GET | `/api/hello` | 動作確認用のあいさつ | 200 | － |
| GET | `/api/db-check` | DB につながっているかの確認 | 200 | 500（つながらない） |

返ってくるタスクの例：
```json
{ "id": 1, "title": "買い物", "status": "todo", "position": 1,
  "createdAt": "2026-11-06T16:20:00+09:00", "updatedAt": "2026-11-06T16:20:00+09:00" }
```
