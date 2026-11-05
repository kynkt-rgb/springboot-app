package com.example.backend;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * タスクを読み取る（検索する）API。
 *
 * GET /api/tasks              … 全部のタスク
 * GET /api/tasks?status=todo  … 状態（todo / doing / done）で絞ったタスク
 * GET /api/tasks/1            … 番号 1 のタスク 1件
 *
 * 返す値（Task）は、Spring が自動で JSON に変えてくれる。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	/** status に使ってよい値 */
	private static final Set<String> STATUSES = Set.of("todo", "doing", "done");

	private final TaskRepository taskRepository;

	public TaskController(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	/** 一覧。?status= が付いていれば、その状態だけに絞る */
	@GetMapping
	public List<Task> list(@RequestParam(required = false) String status) {
		if (status == null) {
			return taskRepository.findAllByOrderByPositionAsc();
		}
		if (!STATUSES.contains(status)) {
			// 400 Bad Request（送ってきた値がおかしい）
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"status は todo / doing / done のどれかにしてください");
		}
		return taskRepository.findByStatusOrderByPositionAsc(status);
	}

	/** 番号（id）で 1件取り出す。無ければ 404 Not Found */
	@GetMapping("/{id}")
	public Task get(@PathVariable Long id) {
		return taskRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"番号 " + id + " のタスクはありません"));
	}

}
