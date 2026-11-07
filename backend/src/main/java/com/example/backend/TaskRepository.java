package com.example.backend;

import java.util.List;
import java.util.Optional;

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

	/*
	 * ここから下は「メソッドの名前」だけ書けば、Spring が名前を読んで SQL を自動で作ってくれる。
	 *   findAll        … 全部さがす
	 *   ByStatus       … status 列が引数と同じものだけ（WHERE status = ?）
	 *   OrderByPositionAsc … position の小さい順に並べる（ORDER BY position ASC）
	 */

	/** 全部のタスクを、並び順（position）の小さい順で取り出す */
	List<Task> findAllByOrderByPositionAsc();

	/** 指定した状態（todo / doing / done）のタスクだけを、並び順で取り出す */
	List<Task> findByStatusOrderByPositionAsc(String status);

	/**
	 * 指定した状態のタスクのうち、並び順（position）がいちばん大きい1件を取り出す。
	 * 新しいタスクを「列の一番下」に入れるときに使う。
	 *   findTop … 1件だけ  /  OrderByPositionDesc … position の大きい順
	 * 列が空なら Optional.empty()（「中身なし」）が返る。
	 */
	Optional<Task> findTopByStatusOrderByPositionDesc(String status);
}
