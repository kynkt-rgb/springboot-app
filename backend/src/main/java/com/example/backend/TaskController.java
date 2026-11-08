package com.example.backend;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.web.server.ResponseStatusException;

/**
 * タスクの API。
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
	 *   position … 列の中の上から何番目に入れるか（1から数える）。その列の並び順を振りなおす
	 *   status だけ変わって position が無いときは、その列の一番下に入れる
	 * 成功すると 200 OK と、変えたあとのタスクを返す。無い番号なら 404。
	 */
	@PatchMapping("/{id}")
	// 何件もの並び順を書きかえるので、途中で失敗したら全部なかったことにする（トランザクション）
	@Transactional
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

		// 列の移動と並び替えは、ほかの項目のチェックがぜんぶ通ってから最後にやる
		// （ほかのカードの並び順も書きかえるので、途中でエラーになると困るため）
		String status = task.getStatus();
		if (body.containsKey("status")) {
			status = asText(body.get("status"), "状態");
			if (status == null || !STATUSES.contains(status)) {
				throw badRequest("status は todo / doing / done のどれかにしてください");
			}
		}
		if (body.containsKey("position")) {
			// 列の中の、上から何番目に入れるか（1から数える）
			placeInColumn(task, status, asPlace(body.get("position")));
		} else if (!status.equals(task.getStatus())) {
			// 場所の指定がなく、列だけ変わったときは、その列の一番下に入れる
			task.setPosition(nextPosition(status));
			task.setStatus(status);
		}

		return taskRepository.save(task);
	}

	/**
	 * タスクを、列（status）の上から place 番目に入れる。
	 * その列のカードを並べた順に 1, 2, 3… と振りなおして保存する。
	 * place が列のカードの数より大きいときは、一番下に入れる。
	 *
	 * 例：やること [A, B, C] の C を 1番目へ → [C, A, B]（C=1, A=2, B=3）
	 */
	private void placeInColumn(Task task, String status, long place) {
		// その列のカードを並び順で取り出し、動かすカード自身はいったん抜く
		List<Task> column = new ArrayList<>(taskRepository.findByStatusOrderByPositionAsc(status));
		column.removeIf(t -> t.getId().equals(task.getId()));

		int index = (int) Math.min(place - 1, column.size());
		column.add(index, task);
		task.setStatus(status);

		for (int i = 0; i < column.size(); i++) {
			column.get(i).setPosition((long) (i + 1));
		}
		taskRepository.saveAll(column);
	}

	/**
	 * タスクを 1件、データベースから完全に消す（物理削除。元には戻せない）。
	 * 成功すると 204 No Content（「消しました。返すものはありません」）。無い番号なら 404。
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		// 先に「あるかどうか」を確かめる。無ければ get() が 404 にする
		Task task = get(id);
		taskRepository.delete(task);
	}

	/** 優先度の高い順。この順番どおりに並べる（high が一番上） */
	private static final List<String> PRIORITY_ORDER = List.of("high", "medium", "low");

	/**
	 * 1つの列のカードを、優先度の高い順（高 → 中 → 低）に並べなおして保存する。
	 * 送られてきた例: { "status": "todo" }
	 * 優先度が同じカードどうしは、いまの順番のまま。
	 * 成功すると 200 OK と、並べなおしたその列のタスク（上から順）を返す。
	 */
	@PostMapping("/sort-by-priority")
	// 何件もの並び順を書きかえるので、途中で失敗したら全部なかったことにする（トランザクション）
	@Transactional
	public List<Task> sortByPriority(@RequestBody Map<String, Object> body) {
		String status = asText(body.get("status"), "状態");
		if (status == null || !STATUSES.contains(status)) {
			throw badRequest("status は todo / doing / done のどれかにしてください");
		}

		// いまの並び順で取り出してから、優先度で並べる。
		// Java の sort は「同じ値どうしの順番を変えない」並べ方なので、同じ優先度ならいまの順のまま
		List<Task> column = new ArrayList<>(taskRepository.findByStatusOrderByPositionAsc(status));
		column.sort(Comparator.comparingInt(t -> priorityRank(t.getPriority())));

		for (int i = 0; i < column.size(); i++) {
			column.get(i).setPosition((long) (i + 1));
		}
		taskRepository.saveAll(column);
		return column;
	}

	/** 優先度を「上から何番目か」の数字にする（high=0, medium=1, low=2。わからない値は medium あつかい） */
	private static int priorityRank(String priority) {
		int rank = PRIORITY_ORDER.indexOf(priority);
		return rank == -1 ? PRIORITY_ORDER.indexOf("medium") : rank;
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

	/** 送られてきた position が 1 以上の整数かを確かめる。ちがえば 400 */
	private static long asPlace(Object value) {
		if ((value instanceof Integer || value instanceof Long) && ((Number) value).longValue() >= 1) {
			return ((Number) value).longValue();
		}
		throw badRequest("position は 1 以上の整数にしてください");
	}

	/** 400 Bad Request（送ってきた値がおかしい）のエラーを作る */
	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}
