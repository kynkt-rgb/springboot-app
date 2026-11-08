# 品質チェックの結果（2026-11-08）

> イシュー #20。見くらべたもの：このアプリのコード（main の f8f938c 時点）、[README](../README.md)、[tech-stack.md](tech-stack.md)、もとの設計書 [task-board/docs](https://github.com/kynkt-rgb/task-board/tree/main/docs)（01〜07）

## まとめ（先に読むところ）
- **動きはしっかりしている**。自動テスト 31件はすべて合格。入力チェックは画面とサーバーの両方にあり、絵文字の数え方までそろっている。
- **書き方の「型」から外れているところ**がいくつかある。いちばん大きかったのは「バックエンドの仕事が1つのファイル（TaskController）に全部入っている」こと（→ A-1）。
  - 大事さ「高」のうち **A-1 と B-1 は直した**（#20。動きは変えず、置き場所だけ整理）。A-2 は API の約束が変わるので次の段階でやる。
- **ドキュメントが実装に追いついていない**。README の「できること」「API」は、追加・編集・移動・削除がまだ書かれていない（→ C）。

大事さ：**高** ＝ 先に直すとよい ／ **中** ＝ 機会があれば ／ **低** ＝ 今のままでも困らない

直した項目は「状況」に ✅ をつけています。のこりは、1つずつイシューにして進めます。

---

## A. Spring Boot のふつうの書き方から外れているところ

| No | 大事さ | 場所 | いまの書き方 | ふつうの書き方 |
|----|------|------|------------|--------------|
| A-1 | 高 ✅ 直した | [TaskController.java](../backend/src/main/java/com/example/backend/TaskController.java) | 入力チェック・並び替えの計算・トランザクション（`@Transactional`）が、ぜんぶコントローラーにある | コントローラーは「受け取って返す」だけ。仕事のルールは `TaskService` クラスに分ける。トランザクションも Service に付ける。→ [TaskService.java](../backend/src/main/java/com/example/backend/TaskService.java) に移した。下の「同」「Controller」の行は、いまは TaskService のこと |
| A-2 | 高 | 同 `update`（121行目）・`sortByPriority`（223行目） | 中身を `Map<String, Object>` で受け取り、`asText` / `asPlace` で型を手で確かめている | 受け取り用の record（例 `UpdateTaskRequest`）を作り、`spring-boot-starter-validation` の `@Valid` `@NotBlank` `@Size(max = 50)` で確かめる。「送られてこない」と「空にしたい」の区別は、record の項目を `JsonNullable` などで表す |
| A-3 | 中 | [Task.java](../backend/src/main/java/com/example/backend/Task.java) 40・52行目 | status・priority が文字（String）。使ってよい値は Controller の `Set` で確かめる | `enum`（決まった値だけの型）にして `@Enumerated(EnumType.STRING)` を付ける。おかしい値は受け取る時点で弾ける |
| A-4 | 中 | エラーの返し方（Controller 全体） | `ResponseStatusException` をあちこちで投げる | `@RestControllerAdvice` で1か所にまとめる。形は Spring 標準の `ProblemDetail`（`spring.mvc.problemdetails.enabled=true`） |
| A-5 | 中 | Controller の返り値 | DB の形（エンティティ `Task`）をそのまま JSON で返している | 返す用の record（例 `TaskResponse`）を分ける。DB に列を足しても API の形が勝手に変わらない |
| A-6 | 中 | [application.properties](../backend/src/main/resources/application.properties) 14行目 | `ddl-auto=update`。表は Hibernate が自動で作り、表の定義ファイルがない | Flyway（表の変更を番号つきの SQL で残す道具）か `schema.sql`。DB 側のチェック（タイトル1〜50文字・status の値）も SQL で書ける |
| A-7 | 中 | [BackendApplicationTests.java](../backend/src/test/java/com/example/backend/BackendApplicationTests.java) | `@SpringBootTest` が本物の DB を使う。Docker の DB を止めると `./gradlew test` が失敗する | Testcontainers（テストのたびに使い捨ての DB を作る）。リポジトリのテストは `@DataJpaTest` |
| A-8 | 低 | application.properties 16行目 | `show-sql=true` がいつも ON | 開発用の設定ファイル（`application-dev.properties`）に分ける |
| A-9 | 低 | TaskController.java 44行目 | `produces = "application/json;charset=UTF-8"` | 文字化け対策の回避策で、理由もコメントにある。残してよいが、ふつうは書かない |
| A-10 | 低 | パッケージ | 全部 `com.example.backend` の1か所 | 小さいうちはこれでよい。増えたら `controller` / `service` / `repository` などに分ける |
| A-11 | 低 | HelloController / DbCheckController / HELP.md | 最初の確認用が残っている | 動作確認は Spring 標準の Actuator（`/actuator/health`）。もとの設計書の `/api/health` にも当たる |
| A-12 | 低 | [TaskControllerTest.java](../backend/src/test/java/com/example/backend/TaskControllerTest.java) 112・330・398行目 | `org.assertj...` `org.hamcrest...` を import せずに長い名前で書いている | 上で import して短く書く |
| A-13 | 低 | TaskController.java `nextPosition`（250行目） | 一番下の番号を読んでから +1 する | 同時に2件追加すると同じ番号になりうる。1人で使うので実害は小さい |

## B. React のふつうの書き方から外れているところ

| No | 大事さ | 場所 | いまの書き方 | ふつうの書き方 |
|----|------|------|------------|--------------|
| B-1 | 高 ✅ 直した | [App.jsx](../frontend/src/App.jsx)・[TaskForm.jsx](../frontend/src/TaskForm.jsx)・[TaskDetail.jsx](../frontend/src/TaskDetail.jsx) | `fetch` と「エラーの理由を取り出す」処理が5か所に同じ形でコピーされている | `src/api.js` に `fetchTasks()` `createTask()` などをまとめ、画面の部品からはそれを呼ぶ。→ [api.js](../frontend/src/api.js) を作った |
| B-2 | 中 | App.jsx 37行目 `useEffect` | 片づけ処理がない。開発中（StrictMode）は2回読みこむ | `AbortController` か `ignore` フラグで、いらなくなった読みこみを捨てる |
| B-3 | 中 | App.jsx `handleMove`（`before` の行） | 失敗したら `before`（動かす前の一覧）に戻す | 続けて2枚動かして1枚目が失敗すると、2枚目の移動まで消える。失敗したら一覧を読みなおすほうが安全 |
| B-4 | 中 | TaskForm.jsx 5行目・TaskDetail.jsx 6〜7・24行目 | `TITLE_MAX` `DESCRIPTION_MAX` `countChars` が2つのファイルに重複 | `priorities.js` と同じように共通のファイル（例 `rules.js`）にまとめる |
| B-5 | 中 | [Card.jsx](../frontend/src/Card.jsx) 36〜43行目 | 列の移動・並び替えはドラッグだけ | キーボードやスマホでは動かせない。ボタンかメニューなど、ほかの手段もあるのがふつう（→ C-2-11） |
| B-6 | 低 | [ConfirmDialog.jsx](../frontend/src/ConfirmDialog.jsx) 52行目 | 詳細モーダルの CSS クラス `task-detail` を借りている | 片方の見た目を変えると、もう片方も変わる。共通のクラス名（例 `modal`）にする |
| B-7 | 低 | [package.json](../frontend/package.json) | `@types/react` `@types/react-dom` が入っている | TypeScript を使わないのでいらない（雛形の残り） |
| B-8 | 低 | フロント全体 | 自動テストがない | Vitest ＋ Testing Library が React のふつうの組み合わせ |

---

## C. 実装とドキュメントのちがい

### C-1. このアプリの README・docs

| No | ドキュメント | 書いてあること | 実装 | 案 |
|----|------------|--------------|------|----|
| C-1-1 | README「できること」 | 追加・移動・並び替え・削除は「これから」。status での検索は「画面からはまだ使っていない」 | 追加・詳細と編集・ドラッグでの移動と並び替え・優先度順・削除まで、全部できている | README を直す |
| C-1-2 | README「フォルダの中身」 | App / Column / Card だけ | TaskForm.jsx・TaskDetail.jsx・ConfirmDialog.jsx・priorities.js・CreateTaskRequest.java もある | README を直す |
| C-1-3 | README「API」 | GET の3つと hello・db-check だけ | `POST /api/tasks`、`PATCH /api/tasks/{id}`、`DELETE /api/tasks/{id}`、`POST /api/tasks/sort-by-priority` もある | README を直す |
| C-1-4 | README「返ってくるタスクの例」 | id・title・status・position・日時だけ | `description`・`priority`・`dueDate` もある | README を直す |
| C-1-5 | README「自動テスト」 | `./gradlew test` だけ | Docker の DB が動いていないと失敗する（A-7） | README に一言足す（または A-7 を直す） |

### C-2. もとの設計書（task-board/docs）

springboot-app は task-board を作りなおしたもの。下の多くは「途中で自分で決めた変更」だが、設計書のほうは古いまま。

| No | 設計書 | 書いてあること | 実装 | 案 |
|----|-------|--------------|------|----|
| C-2-1 | 01 F-03 | 「←」「→」ボタンで、となりの列へ1つずつ動かす。サーバーも飛びこえる移動を受けつけない | ドラッグ&ドロップ。どの列へも動かせて、サーバーも受けつける | 設計を実装に合わせる |
| C-2-2 | 01 F-09 | 「↑」「↓」ボタン。「完了」の列は並び替えない（サーバーも受けつけない） | ドラッグで、「完了」も含め全部の列で並び替えられる | 設計を実装に合わせる |
| C-2-3 | （なし） | 「優先度順」ボタンは設計書にない | 列ごとに、優先度の高い順に並べて保存できる | 設計に足す |
| C-2-4 | 01「作らないもの」 | タイトル編集・期限・メモ・ドラッグ&ドロップは作らない | 全部作った（説明・優先度・期限を足すと決めた） | 設計を実装に合わせる |
| C-2-5 | 01 F-07 | ベーシック認証でログイン。ログインしないと画面も API も使えない | ログインはない | まだ作っていない。作るか、対象外にするか決める |
| C-2-6 | 01 F-05・04「データの流れ」 | 保存したら一覧を読みなおして画面を作りなおす。画面だけを先に書きかえない | 一覧は読みなおさない。移動は画面を先に変えて、失敗したら戻す | どちらにするか決める（B-3 とも関係） |
| C-2-7 | 01 F-08・03 ⑥-b | 通信中はボタンを押せなくする | 移動や削除の最中も、カードの「×」やドラッグができる | 実装を直すか、設計をゆるめる |
| C-2-8 | 04 ER図 | users・columns・cards の3つの表。cards に user_id・外部キー・インデックス、DB でもタイトルをチェック | tasks の表1つだけ。user_id・外部キー・DB のチェックはない。description・priority・due_date の列がある | 表の名前（cards → tasks）と新しい列を設計に反映。users / columns は作るか決める |
| C-2-9 | 04 テーブル定義 | 定義の実物は `db/schema.sql` | ファイルがない。表は Hibernate が自動で作る（A-6） | A-6 と一緒に決める |
| C-2-10 | 05 API | `/api/cards`、`/api/health`、`POST /api/cards/:id/reorder`（`up`/`down`）、エラーは `{"error": "…"}` | `/api/tasks`、health なし、並び替えは PATCH の `position`、エラーは Spring 標準の形（理由は `message`） | 設計を実装に合わせる |
| C-2-11 | 01 非機能・07 AT-13 / AT-19 | マウスなしで、Tab と Enter だけで全部の操作ができる | 列の移動と並び替えはキーボードでできない（B-5） | 実装を直すか、設計をゆるめる |
| C-2-12 | 01 利用環境・非機能 | スマホ（Safari）でも使う。768px 未満は縦にならべる | 縦にはならぶ（CSS あり）が、スマホ対応はしないと決めた。ドラッグはタッチでは動かない | 設計の対象ブラウザを直す |
| C-2-13 | 03 M-02 | 「（タイトル）」を削除しますか？ ／「OK」「キャンセル」 | 2行目に「削除すると元に戻せません。」、ボタンは「削除する」「キャンセル」 | 設計を実装に合わせる（実装のほうが親切） |
| C-2-14 | 03 M-04 | 失敗したら「保存できませんでした：（理由）」 | 移動・削除・優先度順は「「…」を移動できませんでした：」など別の文言 | 設計を実装に合わせる |
| C-2-15 | 07 AT-04 | 51文字目は入力できない | 入力欄に文字数の上限（`maxLength`）がなく、51文字以上も打てる。「追加」を押したときにエラーを出す | どちらにするか決める |
| C-2-16 | 01 非機能「テスト」・07 AT-16 | `npm test` がすべて通る | バックエンドは `./gradlew test`。フロントの自動テストはない（B-8） | 設計を実装に合わせる |
| C-2-17 | 01 公開・非機能 | Render と Neon で公開、HTTPS、`sslmode=require` | まだ手元だけ | まだの段階 |

**提案**：このアプリ用の要件定義（task-board とのちがいだけを書いたもの）を `docs/` に作ると、こういうずれがこれから出にくくなります（今回は作っていません）。

---

## D. 書き方チェックの道具（今回入れたもの）

| | フロントエンド | バックエンド |
|--|--------------|------------|
| 道具 | oxlint（もとから入っていたもの。ルールを増やした） | Checkstyle 14.4.0（Gradle に最初から入っている） |
| 動かし方 | `cd frontend && npm run lint`（直せるものは `npm run lint:fix`） | `cd backend && ./gradlew check`（テストと一緒に動く） |
| ルールの場所 | `frontend/.oxlintrc.json` | `backend/config/checkstyle/checkstyle.xml` |
| 結果の見方 | 画面にそのまま出る | 画面に出るほか、`backend/build/reports/checkstyle/main.html` と `test.html` |
| いまの結果 | エラー 0件・警告 4件 | 本体 0件・テスト 警告 3件 |
| 守らないとき | エラーは止まる。警告は止まらない | いまは警告だけで止まらない（直しおわったら error にする） |

**oxlint で足したもの**
- `jsx-a11y`：目の見えない人やキーボードの人にも使えるか
- `import`：import の書き方
- `react/exhaustive-deps`：`useEffect` に書き忘れた値がないか
- まちがいの可能性が高いもの（correctness）はエラー、あやしいもの（suspicious）は警告
- 外したもの：`react-in-jsx-scope`（React 17 から不要になった古いルール）、CSS の import への警告

**いま出ている警告**
| 道具 | 場所 | 中身 |
|------|------|------|
| oxlint | ConfirmDialog.jsx 50行目・TaskDetail.jsx 122行目（各2件） | `<dialog>` に `onClick`（外側をクリックで閉じる）があるが、キーボード用の処理がない。Esc キーで閉じられるので実害は小さい |
| Checkstyle | TaskControllerTest.java 192・398・399行目 | 1行が120文字をこえている |
