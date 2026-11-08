---
name: quality-check
description: springboot-app の品質チェックをする。書き方チェック（oxlint・Checkstyle）と自動テストを動かし、React / Spring Boot のふつうの書き方から外れているところ、実装とドキュメントのちがい、エラーを隠すような直し方がないかを調べて docs/quality-review.md にまとめる。「品質チェックして」「コードをレビューして」「書き方をチェックして」「ドキュメントと実装がずれていないか見て」と言われたとき、または PR をマージする前の確認で使う。
---

# 品質チェック

このアプリ（springboot-app）の品質を、毎回同じ手順で確かめる。
**調べて報告するだけ。コードは直さない。** 直すかどうか・どれから直すかはユーザーが決める。

## 守ること

- 作業の流れは `CLAUDE.md` のとおり（イシュー → ブランチ → PR、main に直接コミットしない、マージしない）
  - ユーザーが「同じブランチで」と言ったときは、そのブランチで続ける
- 結果はやさしい日本語で書く。表を使い、各項目に大事さ（高・中・低）をつける
- コマンドの結果は、うまくいかなかったものも含めて**そのまま**報告する。失敗をかくさない
- 確認のために画面やサーバーを動かすときは、スキル `start-servers` の手順で決まったポートで起動する。
  ユーザーのデータを並べかえたり消したりしない（確認用のタスクを作り、最後に消す）

## 手順

### 1. 何と見くらべるかを読む

- このアプリのドキュメント：`README.md`・`docs/tech-stack.md`・`CLAUDE.md`・前回の `docs/quality-review.md`
- もとの設計書（要件定義）：`../task-board/docs/01_requirements.md` 〜 `07_acceptance-criteria.md`
- コード：`backend/src/`（main と test）、`frontend/src/`、`backend/build.gradle`、`frontend/package.json`、
  `frontend/.oxlintrc.json`、`backend/config/checkstyle/`、`backend/src/main/resources/application.properties`
- 前回からの変わり方：`git log --oneline main..HEAD`、`git diff main...HEAD --stat`

### 2. 書き方チェックとテストを動かす

```bash
# フロントエンド（springboot-app/frontend で）
npm run lint
npm run build

# バックエンド（springboot-app/backend で）。Claude のシェルでは JAVA_HOME を指定する
# Docker の DB（5433）が動いていないとテストが失敗する
JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home" ./gradlew check --rerun-tasks
```

- oxlint の「エラー・警告の件数」、Checkstyle の「本体・テストの警告の件数」、テストの「件数・失敗の数」を書きとめる
- テストの件数は `build/test-results/test/*.xml` の `tests=` を足して数える。**前回より減っていたら必ず報告する**

### 3. エラーを隠すような直し方がないかを確かめる（大事）

チェックやテストを「通す」ために、問題を直さずに見えなくしていないかを調べる。
前回の PR（`git diff main...HEAD`）と、いまのコード全体の両方を見る。

| No | 確かめること | 調べ方の例 |
|----|------------|-----------|
| H-1 | チェックのルールを外したり、エラーを警告に下げたりしていないか | `git diff main...HEAD -- frontend/.oxlintrc.json backend/config/checkstyle/ backend/build.gradle`。`"off"`・`"warn"`・`severity`・`ignoreFailures`・`maxWarnings`・`suppress` が増えていないか |
| H-2 | コードの中でチェックを止めていないか | `grep -rn "oxlint-disable\|eslint-disable\|@SuppressWarnings\|CHECKSTYLE:OFF\|@ts-ignore" frontend/src backend/src` |
| H-3 | テストを消したり、止めたりしていないか | テストの件数が減っていないか。`grep -rn "@Disabled\|\.skip(\|\.only(\|xit(\|assumeTrue" backend/src/test frontend/src` |
| H-4 | テストの答え（期待する値）を、まちがった動きに合わせて書きかえていないか | `git diff main...HEAD -- backend/src/test` で `andExpect` や `assert` の値が変わった行を読み、仕様（設計書・README）に合っているか確かめる |
| H-5 | エラーを受け取って、何もせずに捨てていないか | Java：空の `catch {}`、`catch` してログも出さず `return null` など。JS：`.catch(() => {})`、`catch` の中で何も表示しない。`grep -rn "catch" frontend/src backend/src/main` で全部を読む |
| H-6 | エラーを「成功」あつかいにしていないか | 例：失敗のときに空の一覧や初期値を返して、画面にエラーを出さない。わざと成功あつかいにしている所（例：`api.js` の削除の 404）は、理由のコメントがあるかを見る |
| H-7 | 入力チェックをゆるめていないか | 文字数の上限・使ってよい値（status / priority）・必須のチェックが、画面とサーバーの両方に残っているか。前回とくらべて消えていないか |
| H-8 | 実行のときにチェックを飛ばしていないか | `-x test`・`-x checkstyleMain`・`--no-verify`・`--max-warnings` を、手順やスクリプト（`package.json`・`build.gradle`・`.claude/`・README）に足していないか |

- 見つけたものは「隠している」と決めつけず、**理由が書いてあるか**を確かめる。理由があり、
  `docs/quality-review.md` に記録されているものは「記録あり」、理由が無いものは大事さ「高」で報告する
- いまの設定ですでに記録されている例：oxlint の `react-in-jsx-scope` を off、CSS の import を許す、
  `<dialog>` の2つの jsx-a11y ルールを warn（どれも quality-review.md の D に理由あり）。
  Checkstyle が warning だけでビルドを止めないこと（直しおわったら error にする予定）

### 4. ふつうの書き方から外れているところを調べる

- **Spring Boot**：Controller が薄いか（仕事は Service）、受け取り用 record ＋ Bean Validation、enum、
  エラーを1か所で返す（`@RestControllerAdvice` / `ProblemDetail`）、エンティティをそのまま返していないか、
  表の定義（Flyway / schema.sql）、テストが本物の DB に頼っていないか、設定の分け方（profile）
- **React**：fetch は `api.js` だけか、`useEffect` の片づけ、state の持ち方、同じ定数・関数の重複、
  キーボード・スクリーンリーダーで使えるか、自動テストがあるか
- 前回の `docs/quality-review.md` の A・B の項目それぞれについて「直った／まだ／悪くなった」を確かめ、
  新しく見つけたものは番号を続けて足す

### 5. 実装とドキュメントのちがいを調べる

- README の「できること」「フォルダの中身」「API」「JSON の例」「動かし方」が、いまのコードと合っているか
- `docs/tech-stack.md` のバージョンが `package-lock.json`・`build.gradle`・`gradle-wrapper.properties` と合っているか
- もとの設計書（01〜07）の機能（F-xx）・メッセージ（M-xx）・受け入れ基準（AT-xx）・API・ER図と、実装のちがい
- 前回の C の項目それぞれについて「直った／まだ」を確かめる

### 6. docs/quality-review.md を書きかえる

- 先頭に日付と、見くらべたコミット（`git log -1 --format=%h`）を書く
- 形は前回と同じ：まとめ → A（Spring Boot）→ B（React）→ C（ドキュメントとのちがい）→ D（書き方チェックの結果）
  → **E（エラーを隠す直し方の確認：H-1〜H-8 の結果）**
- 直った項目は消さずに「✅ 直した（#イシュー番号）」と書く
- 行番号は、書いたあとにもう一度コードを見て合っているか確かめる

### 7. ユーザーに報告して止める

- まとめは要点2〜3個にしぼり、用語にはたとえを添える
- 書き方チェックとテストの件数（前回とくらべて増えた・減った）を表で見せる
- H-1〜H-8 で「理由が無いもの」があれば、いちばん先に伝える
- 直すのは、ユーザーが選んでから。コミット・push・PR も、ユーザーが中身を確かめてから
