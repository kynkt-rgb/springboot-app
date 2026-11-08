# 技術スタック

> 起動方法は [README](../README.md)、開発ルールは [CLAUDE.md](../CLAUDE.md)

バージョンは 2026-11-06 時点で、実際に入っているもの・動かして確かめたものです。

## 使う技術

### バックエンド（`backend/`）
| 区分 | 技術 | バージョン | 決めているところ |
|------|------|-----------|----------------|
| 言語 | Java（OpenJDK、Homebrew の `openjdk@21`） | 21（21.0.13） | `build.gradle` の `toolchain` |
| フレームワーク | Spring Boot | 4.1.2 | `build.gradle` の `plugins` |
| ↳ 中身の Spring Framework | Spring Framework | 7.0.10 | Spring Boot が自動で決める |
| ↳ Web サーバー | Tomcat（組みこみ） | 11.0.27 | Spring Boot が自動で決める |
| ↳ JSON 変換 | Jackson | 3.1.7 | Spring Boot が自動で決める |
| DB とのやりとり | Spring Data JPA / Hibernate | Hibernate 7.4.13 | Spring Boot が自動で決める |
| DB への接続 | PostgreSQL JDBC Driver | 42.7.14 | Spring Boot が自動で決める |
| ビルド | Gradle（Gradle Wrapper） | 9.8.2 | `gradle/wrapper/gradle-wrapper.properties` |
| ライブラリのバージョン管理 | io.spring.dependency-management | 1.1.7 | `build.gradle` の `plugins` |
| テスト | JUnit / Mockito（MockMvc） | JUnit 6.0.3 / Mockito 5.23.0 | Spring Boot が自動で決める |
| 書き方チェック | Checkstyle（Gradle の checkstyle プラグイン） | 14.4.0 | `build.gradle` の `checkstyle`、ルールは `config/checkstyle/checkstyle.xml` |

### フロントエンド（`frontend/`）
| 区分 | 技術 | バージョン | 決めているところ |
|------|------|-----------|----------------|
| 言語 | JavaScript（JSX） | － | TypeScript は使わない |
| 画面のしくみ | React / React DOM | 19.3.1 | `package.json`（実際の版は `package-lock.json`） |
| 開発サーバー・ビルド | Vite | 8.3.6 | 同上 |
| Vite の React 対応 | @vitejs/plugin-react | 6.2.1 | 同上 |
| 書き方チェック | oxlint（react・jsx-a11y・import のルールつき） | 1.91.0 | 同上。ルールは `.oxlintrc.json` |
| 実行環境 | Node.js / npm（Homebrew） | 26.13.0 / 11.20.0 | Mac に入っているもの |

### DB・道具
| 区分 | 技術 | バージョン | 決めているところ |
|------|------|-----------|----------------|
| DB | PostgreSQL（Docker の公式イメージ） | 17（17.11） | `compose.yaml` の `image: postgres:17` |
| DB を動かす箱 | Docker Desktop / Docker Compose | 29.9.2 / 5.6.1 | Mac に入っているもの |
| バージョン管理 | Git / GitHub（`gh` コマンド） | Git 2.50.1 / gh 2.103.3 | Mac に入っているもの |

`postgres:17` は「17 系の最新」を意味するので、イメージを取りなおすと 17.12 などに上がることがあります（17 系の中なので動きは変わりません）。

## ポート
| 動かすもの | ポート |
|-----------|-------|
| DB | 5433（Mac の 5432 は Homebrew の PostgreSQL が使っているため） |
| バックエンド | 8080 |
| フロントエンド | 5173（`/api` は 8080 に取り次ぐ） |

ポートは決まった番号以外では動かしません（[CLAUDE.md](../CLAUDE.md) の「サーバーのポート」）。

## 選んだ理由
| 選んだもの | 理由 |
|-----------|------|
| Java + Spring Boot | 講座の Spring Boot 回の課題で指定された |
| Gradle | 講義に合わせた |
| Spring Boot 4 系 | start.spring.io で 3 系が選べなくなっていた |
| React（Next.js は使わない） | 講座で指定された。画面が1つなので Next.js はいらない |
| Vite | React の雛形をすぐ作れて、保存するとすぐ画面に出る |
| JavaScript（TypeScript は使わない） | 書くことが少なく、はじめてでも読みやすい |
| PostgreSQL を Docker で動かす | Mac に入れた PostgreSQL と分けて、`docker compose` だけで同じ DB を用意できる |
| Vite の proxy で `/api` を 8080 に取り次ぐ | バックエンドに CORS の設定を足さなくて済む |
