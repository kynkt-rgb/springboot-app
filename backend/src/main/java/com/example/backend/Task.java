package com.example.backend;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * タスク1件を表すクラス（エンティティ＝表の設計図）。
 * このクラスを見て、Spring Boot がデータベースに「tasks 表」を作る。
 * 1つのフィールドが、表の1つの列になる。
 *
 * もとにしたのは task-board の ER図（docs/04_data-design.md）の CARDS 表。
 * user_id（だれのタスクか）は、あとの段階で足す。
 */
@Entity
@Table(name = "tasks")
public class Task {

	/** タスクの番号。1, 2, 3… とデータベースが自動でつける（主キー） */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(columnDefinition = "bigint")
	private Long id;

	/** タスクの名前。空にはできない（1〜50文字のチェックはあとの段階で足す） */
	@Column(nullable = false, columnDefinition = "text")
	private String title;

	/** 状態（todo / doing / done）。書かなければ 'todo' が入る */
	@Column(nullable = false, columnDefinition = "text default 'todo'")
	private String status = "todo";

	/** 列の中での並び順。小さいほど上に表示する */
	@Column(nullable = false, columnDefinition = "bigint")
	private Long position;

	/** 作った日時。保存したときに自動で入る */
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false,
			columnDefinition = "timestamptz default now()")
	private OffsetDateTime createdAt;

	/** 最後に変えた日時。保存・更新のたびに自動で入る */
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false,
			columnDefinition = "timestamptz default now()")
	private OffsetDateTime updatedAt;

	/** JPA（データベースとのやりとりをする仕組み）が使う、引数なしのコンストラクタ */
	public Task() {
	}

	// --- getter / setter（値を取り出す・入れるためのメソッド） ---

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getPosition() {
		return position;
	}

	public void setPosition(Long position) {
		this.position = position;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
