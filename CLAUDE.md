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

## 守るしくみ

- `.claude/hooks/guard-git.sh`：main 上での `git commit`、main への `git push`、force push を止める
- GitHub 側：main は PR 経由でしか変更できない（承認は不要。自分でマージしてよい）
