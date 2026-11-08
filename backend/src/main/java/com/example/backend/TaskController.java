package com.example.backend;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * PATCH /api/tasks/1           … 番号 1 のタスクの、送られてきた項目だけを変える
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

	/** priority（優先度）に使ってよい値 */
	private static final Set<String> PRIORITIES = Set.of("high", "medium", "low");

	/** タイトルの最大の文字数 */
	private static final int TITLE_MAX = 50;

	/** 説明の最大の文字数 */
	private static final int DESCRIPTION_MAX = 500;

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
		String title = checkTitle(request.title());

		// 「やること」列の一番下に入れる
		long position = nextPosition("todo");

		Task task = new Task();
		task.setTitle(title);
		task.setStatus("todo");
		task.setPosition(position);
		return taskRepository.save(task);
	}

	/**
	 * タスクの一部を変える（PATCH）。送られてきた項目だけを変え、送られてこなかった項目はそのまま。
	 * 送られてきた例: { "title": "牛乳を買う", "description": "2本", "priority": "high", "dueDate": "2026-11-10" }
	 *
	 * 「送られてこなかった」と「空（null）にしたい」を見分けるため、中身を Map（名前 → 値の表）で受け取る。
	 *   description / dueDate … null や "" なら空にする
	 *   title / priority / status … 空にはできない
	 *   status が変わったら（列を移動したら）、position をその列の一番下にする
	 * 成功すると 200 OK と、変えたあとのタスクを返す。無い番号なら 404。
	 */
	@PatchMapping("/{id}")
	public Task update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		Task task = get(id);

		if (body.containsKey("title")) {
			task.setTitle(checkTitle(asText(body.get("title"), "タイトル")));
		}
		if (body.containsKey("description")) {
			String description = asText(body.get("description"), "説明");
			description = description == null ? "" : description.strip();
			if (countChars(description) > DESCRIPTION_MAX) {
				throw badRequest("説明は" + DESCRIPTION_MAX + "文字以内にしてください");
			}
			task.setDescription(description.isEmpty() ? null : description);
		}
		if (body.containsKey("priority")) {
			String priority = asText(body.get("priority"), "優先度");
			if (priority == null || !PRIORITIES.contains(priority)) {
				throw badRequest("優先度は high / medium / low のどれかにしてください");
			}
			task.setPriority(priority);
		}
		if (body.containsKey("status")) {
			String status = asText(body.get("status"), "状態");
			if (status == null || !STATUSES.contains(status)) {
				throw badRequest("status は todo / doing / done のどれかにしてください");
			}
			// 同じ列に落としたときは何もしない（並び順もそのまま）
			if (!status.equals(task.getStatus())) {
				task.setPosition(nextPosition(status));
				task.setStatus(status);
			}
		}
		if (body.containsKey("dueDate")) {
			String dueDate = asText(body.get("dueDate"), "期限");
			if (dueDate == null || dueDate.isBlank()) {
				task.setDueDate(null);
			} else {
				try {
					// "2026-11-10" の形の文字を日付に変える。ありえない日付（2月30日など）はエラーになる
					task.setDueDate(LocalDate.parse(dueDate));
				} catch (DateTimeParseException e) {
					throw badRequest("期限は 2026-11-10 の形の日付にしてください");
				}
			}
		}

		return taskRepository.save(task);
	}

	// --- ここから下は、登録と変更で共通して使う小さな部品 ---

	/** 列の一番下の位置：いまの一番下の position + 1（列が空なら 1） */
	private long nextPosition(String status) {
		return taskRepository.findTopByStatusOrderByPositionDesc(status)
				.map(last -> last.getPosition() + 1)
				.orElse(1L);
	}

	/** タイトルの前後の空白を取りのぞき、1〜50文字かを確かめる */
	private static String checkTitle(String raw) {
		// strip は全角スペースも取りのぞける
		String title = raw == null ? "" : raw.strip();
		if (title.isEmpty()) {
			throw badRequest("タイトルを入力してください");
		}
		if (countChars(title) > TITLE_MAX) {
			throw badRequest("タイトルは" + TITLE_MAX + "文字以内にしてください");
		}
		return title;
	}

	/** 文字数を数える。length() だと絵文字が 2文字になるので、codePointCount で「見た目の1文字」を数える */
	private static int countChars(String text) {
		return text.codePointCount(0, text.length());
	}

	/** 送られてきた値が文字（または null）かを確かめる。数字などが来たら 400 */
	private static String asText(Object value, String name) {
		if (value == null || value instanceof String) {
			return (String) value;
		}
		throw badRequest(name + "は文字で送ってください");
	}

	/** 400 Bad Request（送ってきた値がおかしい）のエラーを作る */
	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}
