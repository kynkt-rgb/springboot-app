---
name: start-servers
description: springboot-app の DB（5433）・バックエンド（8080）・フロントエンド（5173）を決まったポートで起動し、動いているか確かめる。「サーバーを起動して」「動作確認して」「画面を見られるようにして」「bootRun / npm run dev して」と言われたとき、またはアプリを動かして確認するときに使う。ポートがふさがっていたら別のポートに逃げず、使っているプロセスを止めてから決まったポートで起動する。
---

# サーバーを決まったポートで起動する

このアプリは、3つを**必ず下の決まったポートで**動かす。
別のポートで一時的に起動すると、画面からバックエンドへの取り次ぎ（`/api` → 8080）や確認手順が合わなくなり、うまく動かない。

| 動かすもの | ポート | フォルダ |
|-----------|-------|---------|
| DB（Docker の PostgreSQL） | 5433 | `springboot-app/` |
| バックエンド（Spring Boot） | 8080 | `springboot-app/backend/` |
| フロントエンド（Vite + React） | 5173 | `springboot-app/frontend/` |

## 守ること

- `--port`・`--server.port`・`-Dserver.port`・`SERVER_PORT`・`PORT` でポートを変えない
- `frontend/vite.config.js` の `port: 5173` / `strictPort: true` を外さない
- ポートがふさがっていたら、**そのポートを使っているプロセスを止めてから**決まったポートで起動しなおす
- 止めてよいのは 8080 と 5173 だけ。DB（5432 / 5433）は止めない
- ルールのもとは `CLAUDE.md` の「サーバーのポート」。フック `.claude/hooks/guard-port.sh` も別ポートでの起動を止める

## 手順

コマンドは `springboot-app/` を基準に、絶対パスで書く。

### 1. DB を起動する

docker コマンドは PATH に無いので、フルパスで呼ぶ。

```bash
D=/Applications/Docker.app/Contents/Resources/bin/docker
cd <springboot-app> && $D compose up -d
for i in $(seq 1 20); do $D exec springboot-app-db pg_isready -U app -d springboot_app >/dev/null 2>&1 && break; sleep 1; done
```

テストデータを入れなおすときだけ（中身が全部消えて6件に戻る）:

```bash
$D exec -i springboot-app-db psql -U app -d springboot_app < <springboot-app>/db/seed.sql
```

### 2. ポートが空いているか確かめる

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
```

何か出たら、そのプロセスを止める（止めたことはユーザーに伝える）:

```bash
<springboot-app>/.claude/hooks/free-port.sh 8080
<springboot-app>/.claude/hooks/free-port.sh 5173
```

### 3. バックエンドを起動する（8080）

Claude のシェルでは JAVA_HOME が無いので指定する。`run_in_background` で起動する。

```bash
cd <springboot-app>/backend && JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home" ./gradlew bootRun
```

出力に `Tomcat started on port 8080` と `Started BackendApplication` が出るまで待つ。
`Port 8080 was already in use` が出たら、手順2で 8080 を空けてから起動しなおす。

### 4. フロントエンドを起動する（5173）

初回や `package.json` が変わったときは先に `npm install`。`run_in_background` で起動する。

```bash
cd <springboot-app>/frontend && npm run dev
```

出力に `Local:   http://localhost:5173/` が出るまで待つ。
`Port 5173 is already in use` で止まったら、手順2で 5173 を空けてから起動しなおす。

### 5. 動いているか確かめる

```bash
curl -s http://localhost:5173/api/tasks   # 画面のサーバー経由でバックエンドに届くか
lsof -nP -iTCP:8080 -iTCP:5173 -sTCP:LISTEN
```

タスクの JSON が返り、java が 8080・node が 5173 で待ち受けていれば OK。
ユーザーには「http://localhost:5173 を開いてください」と伝える。
