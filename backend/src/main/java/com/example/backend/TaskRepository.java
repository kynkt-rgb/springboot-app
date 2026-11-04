package com.example.backend;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * tasks 表とやりとりする窓口（リポジトリ）。
 *
 * JpaRepository を受けつぐ（extends する）だけで、
 * 保存 save()、全件の検索 findAll()、番号での検索 findById()、削除 deleteById() などの
 * 基本の命令が、中身を書かなくても自動でそろう。
 *
 * {@code <Task, Long>} は「Task を扱う。番号（id）の型は Long」という意味。
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
}
