package com.example.backend;

/**
 * タスクを新しく登録するときに、画面から送られてくる中身。
 * 例: { "title": "買い物" }
 *
 * record は「値を入れておくだけの箱」を短く書く書き方。
 * getter などは自動でできる（title() で取り出せる）。
 */
public record CreateTaskRequest(String title) {
}
