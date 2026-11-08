package com.example.backend;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * タスクの API（窓口）。
 *
 * GET  /api/tasks              … 全部のタスク
 * GET  /api/tasks?status=todo  … 状態（todo / doing / done）で絞ったタスク
 * GET  /api/tasks/1            … 番号 1 のタスク 1件
 * POST /api/tasks              … タスクを 1件登録する（「やること」列の一番下に入る）
 * PATCH /api/tasks/1           … 番号 1 のタスクの、送られてきた項目だけを変える
 *                                （列の移動・列の中での並び替えもこれでやる）
 * DELETE /api/tasks/1          … 番号 1 のタスクをデータベースから完全に消す（物理削除）
 * POST /api/tasks/sort-by-priority … 1つの列のカードを、優先度の高い順（高 → 中 → 低）に並べなおす
 *
 * ここでは「受け取って、TaskService に渡して、返ってきたものを返す」だけをする。
 * 入力チェックや並び替えなどの仕事の中身は TaskService にある。
 * 返す値（Task）は、Spring が自動で JSON に変えてくれる。
 *
 * produces の「charset=UTF-8」は文字化け対策。
 * 返事に「文字コードは UTF-8」と書き添えないと、Safari などが日本語を
 * 別の文字コード（Shift_JIS）だと思って読み、文字化けする。
 */
@RestController
@RequestMapping(path = "/api/tasks", produces = "application/json;charset=UTF-8")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	/** 一覧。?status= が付いていれば、その状態だけに絞る（おかしい値なら 400） */
	@GetMapping
	public List<Task> list(@RequestParam(required = false) String status) {
		return taskService.list(status);
	}

	/** 番号（id）で 1件取り出す。無ければ 404 Not Found */
	@GetMapping("/{id}")
	public Task get(@PathVariable Long id) {
		return taskService.get(id);
	}

	/**
	 * タスクを 1件登録する。
	 * 送られてきた例: { "title": "買い物" }
	 * 成功すると 201 Created（「作りました」）と、登録したタスクを返す。
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Task create(@RequestBody CreateTaskRequest request) {
		return taskService.create(request.title());
	}

	/**
	 * タスクの一部を変える（PATCH）。送られてきた項目だけを変える。
	 * 成功すると 200 OK と、変えたあとのタスクを返す。無い番号なら 404。
	 */
	@PatchMapping("/{id}")
	public Task update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		return taskService.update(id, body);
	}

	/**
	 * タスクを 1件、データベースから完全に消す（物理削除。元には戻せない）。
	 * 成功すると 204 No Content（「消しました。返すものはありません」）。無い番号なら 404。
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		taskService.delete(id);
	}

	/**
	 * 1つの列のカードを、優先度の高い順（高 → 中 → 低）に並べなおして保存する。
	 * 送られてきた例: { "status": "todo" }
	 * 成功すると 200 OK と、並べなおしたその列のタスク（上から順）を返す。
	 */
	@PostMapping("/sort-by-priority")
	public List<Task> sortByPriority(@RequestBody Map<String, Object> body) {
		return taskService.sortByPriority(body);
	}
}
