package com.example.backend;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * タスクの API。
 *
 * GET  /api/tasks              … 全部のタスク
 * GET  /api/tasks?status=todo  … 状態（todo / doing / done）で絞ったタスク
 * GET  /api/tasks/1            … 番号 1 のタスク 1件
 * POST /api/tasks              … タスクを 1件登録する（「やること」列の一番下に入る）
 *
 * 返す値（Task）は、Spring が自動で JSON に変えてくれる。
 *
 * produces の「charset=UTF-8」は文字化け対策。
 * 返事に「文字コードは UTF-8」と書き添えないと、Safari などが日本語を
 * 別の文字コード（Shift_JIS）だと思って読み、文字化けする。
 */
@RestController
@RequestMapping(path = "/api/tasks", produces = "application/json;charset=UTF-8")
public class TaskController {

	/** status に使ってよい値 */
	private static final Set<String> STATUSES = Set.of("todo", "doing", "done");

	/** タイトルの最大の文字数 */
	private static final int TITLE_MAX = 50;

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

	/**
	 * タスクを 1件登録する。
	 * 送られてきた例: { "title": "買い物" }
	 * 成功すると 201 Created（「作りました」）と、登録したタスクを返す。
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Task create(@RequestBody CreateTaskRequest request) {
		// 前後の空白を取りのぞく（strip は全角スペースも取りのぞける）
		String title = request.title() == null ? "" : request.title().strip();

		if (title.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "タイトルを入力してください");
		}
		// length() だと絵文字が 2文字と数えられてしまうので、codePointCount で「見た目の1文字」を数える
		if (title.codePointCount(0, title.length()) > TITLE_MAX) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"タイトルは" + TITLE_MAX + "文字以内にしてください");
		}

		// 「やること」列の一番下に入れる：いまの一番下の position + 1（列が空なら 1）
		long position = taskRepository.findTopByStatusOrderByPositionDesc("todo")
				.map(last -> last.getPosition() + 1)
				.orElse(1L);

		Task task = new Task();
		task.setTitle(title);
		task.setStatus("todo");
		task.setPosition(position);
		return taskRepository.save(task);
	}
}
