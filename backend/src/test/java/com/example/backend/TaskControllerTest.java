package com.example.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TaskController の自動テスト。
 * 本物の DB は使わず、TaskRepository を「にせもの（モック）」に差しかえて、
 * API が正しい答えを返すかだけを確かめる。
 */
@WebMvcTest(TaskController.class)
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskRepository taskRepository;

	private static Task task(long id, String title, String status, long position) {
		Task t = new Task();
		t.setId(id);
		t.setTitle(title);
		t.setStatus(status);
		t.setPosition(position);
		return t;
	}

	@Test
	void 一覧を返す() throws Exception {
		given(taskRepository.findAllByOrderByPositionAsc()).willReturn(List.of(
				task(1, "買い物", "todo", 1),
				task(3, "部屋のそうじ", "doing", 1)));

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				// 文字化けしないように「UTF-8」の指定が付いていること
				.andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].title").value("買い物"));
	}

	@Test
	void 状態で絞った一覧を返す() throws Exception {
		given(taskRepository.findByStatusOrderByPositionAsc("doing")).willReturn(List.of(
				task(3, "部屋のそうじ", "doing", 1)));

		mockMvc.perform(get("/api/tasks").param("status", "doing"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("doing"));
	}

	@Test
	void おかしい状態なら400() throws Exception {
		mockMvc.perform(get("/api/tasks").param("status", "abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 番号で1件返す() throws Exception {
		given(taskRepository.findById(1L)).willReturn(Optional.of(task(1, "買い物", "todo", 1)));

		mockMvc.perform(get("/api/tasks/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("買い物"));
	}

	@Test
	void 無い番号なら404() throws Exception {
		given(taskRepository.findById(999L)).willReturn(Optional.empty());

		mockMvc.perform(get("/api/tasks/999"))
				.andExpect(status().isNotFound());
	}

	// --- 登録（POST /api/tasks） ---

	/** save されたタスクに番号 100 をつけて、そのまま返すようにする（本物の DB のかわり） */
	private void saveで番号をつける() {
		given(taskRepository.save(any(Task.class))).willAnswer(inv -> {
			Task t = inv.getArgument(0);
			t.setId(100L);
			return t;
		});
	}

	private org.springframework.test.web.servlet.ResultActions 登録する(String json) throws Exception {
		return mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	@Test
	void 登録するとやること列の一番下に入り201() throws Exception {
		// いまの「やること」の一番下は position 2
		given(taskRepository.findTopByStatusOrderByPositionDesc("todo"))
				.willReturn(Optional.of(task(2, "宿題", "todo", 2)));
		saveで番号をつける();

		登録する("{\"title\": \"  牛乳を買う　\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(100))
				// 前後の空白（全角スペースも）が取りのぞかれている
				.andExpect(jsonPath("$.title").value("牛乳を買う"))
				.andExpect(jsonPath("$.status").value("todo"))
				.andExpect(jsonPath("$.position").value(3));
	}

	@Test
	void やること列が空なら位置は1() throws Exception {
		given(taskRepository.findTopByStatusOrderByPositionDesc("todo")).willReturn(Optional.empty());
		saveで番号をつける();

		登録する("{\"title\": \"買い物\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.position").value(1));
	}

	@Test
	void 空白だけのタイトルなら400で保存しない() throws Exception {
		登録する("{\"title\": \"   \"}")
				.andExpect(status().isBadRequest());
		登録する("{}")
				.andExpect(status().isBadRequest());
		verify(taskRepository, never()).save(any(Task.class));
	}

	@Test
	void 絵文字は1文字と数えて50文字までOK() throws Exception {
		given(taskRepository.findTopByStatusOrderByPositionDesc("todo")).willReturn(Optional.empty());
		saveで番号をつける();

		登録する("{\"title\": \"" + "😀".repeat(50) + "\"}")
				.andExpect(status().isCreated());
	}

	@Test
	void 五十一文字なら400で保存しない() throws Exception {
		登録する("{\"title\": \"" + "あ".repeat(51) + "\"}")
				.andExpect(status().isBadRequest());
		verify(taskRepository, never()).save(any(Task.class));
	}

	// --- 変更（PATCH /api/tasks/{id}） ---

	/** 番号 1 のタスクがあることにして、save されたらそのまま返す */
	private Task 番号1のタスクを用意する() {
		Task t = task(1, "買い物", "todo", 1);
		t.setDescription("牛乳");
		t.setDueDate(LocalDate.of(2026, 11, 10));
		given(taskRepository.findById(1L)).willReturn(Optional.of(t));
		given(taskRepository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0));
		return t;
	}

	private org.springframework.test.web.servlet.ResultActions 変更する(long id, String json) throws Exception {
		return mockMvc.perform(patch("/api/tasks/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	@Test
	void 四つの項目を変えられる() throws Exception {
		番号1のタスクを用意する();

		変更する(1, """
				{"title": " 牛乳を買う ", "description": "  2本  ", "priority": "high", "dueDate": "2026-12-01"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("牛乳を買う"))
				.andExpect(jsonPath("$.description").value("2本"))
				.andExpect(jsonPath("$.priority").value("high"))
				.andExpect(jsonPath("$.dueDate").value("2026-12-01"))
				// 状態と並び順は変わらない
				.andExpect(jsonPath("$.status").value("todo"))
				.andExpect(jsonPath("$.position").value(1));
	}

	@Test
	void 送らなかった項目はそのまま() throws Exception {
		番号1のタスクを用意する();

		変更する(1, "{\"priority\": \"low\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("買い物"))
				.andExpect(jsonPath("$.description").value("牛乳"))
				.andExpect(jsonPath("$.dueDate").value("2026-11-10"))
				.andExpect(jsonPath("$.priority").value("low"));
	}

	@Test
	void 説明と期限は空にできる() throws Exception {
		番号1のタスクを用意する();

		変更する(1, "{\"description\": \"   \", \"dueDate\": null}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").doesNotExist())
				.andExpect(jsonPath("$.dueDate").doesNotExist());
	}

	@Test
	void おかしい値なら400で保存しない() throws Exception {
		番号1のタスクを用意する();

		変更する(1, "{\"title\": \"  \"}").andExpect(status().isBadRequest());
		変更する(1, "{\"title\": \"" + "あ".repeat(51) + "\"}").andExpect(status().isBadRequest());
		変更する(1, "{\"description\": \"" + "あ".repeat(501) + "\"}").andExpect(status().isBadRequest());
		変更する(1, "{\"priority\": \"urgent\"}").andExpect(status().isBadRequest());
		変更する(1, "{\"priority\": null}").andExpect(status().isBadRequest());
		変更する(1, "{\"dueDate\": \"2026-02-30\"}").andExpect(status().isBadRequest());
		変更する(1, "{\"title\": 123}").andExpect(status().isBadRequest());
		verify(taskRepository, never()).save(any(Task.class));
	}

	@Test
	void 説明は500文字までOK() throws Exception {
		番号1のタスクを用意する();

		変更する(1, "{\"description\": \"" + "あ".repeat(500) + "\"}")
				.andExpect(status().isOk());
	}

	@Test
	void 無い番号を変えようとしたら404() throws Exception {
		given(taskRepository.findById(999L)).willReturn(Optional.empty());

		変更する(999, "{\"title\": \"買い物\"}")
				.andExpect(status().isNotFound());
		verify(taskRepository, never()).save(any(Task.class));
	}

	@Test
	void 新しいタスクの優先度は中() throws Exception {
		given(taskRepository.findTopByStatusOrderByPositionDesc("todo")).willReturn(Optional.empty());
		saveで番号をつける();

		登録する("{\"title\": \"買い物\"}")
				.andExpect(jsonPath("$.priority").value("medium"));
	}

	// --- 列の移動（PATCH で status を変える） ---

	@Test
	void 列を移動するとその列の一番下に入る() throws Exception {
		番号1のタスクを用意する();
		// いまの「進行中」の一番下は position 4
		given(taskRepository.findTopByStatusOrderByPositionDesc("doing"))
				.willReturn(Optional.of(task(7, "本を返す", "doing", 4)));

		変更する(1, "{\"status\": \"doing\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("doing"))
				.andExpect(jsonPath("$.position").value(5))
				// ほかの項目は変わらない
				.andExpect(jsonPath("$.title").value("買い物"));
	}

	@Test
	void 空の列に移動すると位置は1() throws Exception {
		番号1のタスクを用意する();
		given(taskRepository.findTopByStatusOrderByPositionDesc("done")).willReturn(Optional.empty());

		変更する(1, "{\"status\": \"done\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("done"))
				.andExpect(jsonPath("$.position").value(1));
	}

	@Test
	void 同じ列に落としたら並び順はそのまま() throws Exception {
		番号1のタスクを用意する();

		変更する(1, "{\"status\": \"todo\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.position").value(1));
		verify(taskRepository, never()).findTopByStatusOrderByPositionDesc("todo");
	}

	@Test
	void おかしい状態に移動しようとしたら400で保存しない() throws Exception {
		番号1のタスクを用意する();

		変更する(1, "{\"status\": \"abc\"}").andExpect(status().isBadRequest());
		変更する(1, "{\"status\": null}").andExpect(status().isBadRequest());
		verify(taskRepository, never()).save(any(Task.class));
	}

	// --- 列の中での並び替え（PATCH で position を送る） ---

	@Test
	void 同じ列の中で一番上に動かすと並び順が振りなおされる() throws Exception {
		// やること [A(1), B(2), C(3)] の C を 1番目へ
		Task a = task(1, "A", "todo", 1);
		Task b = task(2, "B", "todo", 2);
		Task c = task(3, "C", "todo", 3);
		given(taskRepository.findById(3L)).willReturn(Optional.of(c));
		given(taskRepository.findByStatusOrderByPositionAsc("todo")).willReturn(List.of(a, b, c));
		given(taskRepository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0));

		変更する(3, "{\"position\": 1}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("todo"))
				.andExpect(jsonPath("$.position").value(1));
		// C, A, B の順になっている
		org.assertj.core.api.Assertions.assertThat(List.of(c.getPosition(), a.getPosition(), b.getPosition()))
				.containsExactly(1L, 2L, 3L);
		verify(taskRepository).saveAll(any());
	}

	@Test
	void ほかの列の決まった場所に入れられる() throws Exception {
		番号1のタスクを用意する();
		// 進行中 [X(1), Y(2)] の2番目に入れる → [X, 買い物, Y]
		Task x = task(5, "X", "doing", 1);
		Task y = task(6, "Y", "doing", 2);
		given(taskRepository.findByStatusOrderByPositionAsc("doing")).willReturn(List.of(x, y));

		変更する(1, "{\"status\": \"doing\", \"position\": 2}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("doing"))
				.andExpect(jsonPath("$.position").value(2));
		org.assertj.core.api.Assertions.assertThat(List.of(x.getPosition(), y.getPosition()))
				.containsExactly(1L, 3L);
	}

	@Test
	void 大きすぎるpositionなら一番下に入る() throws Exception {
		番号1のタスクを用意する();
		Task x = task(5, "X", "doing", 1);
		given(taskRepository.findByStatusOrderByPositionAsc("doing")).willReturn(List.of(x));

		変更する(1, "{\"status\": \"doing\", \"position\": 99}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.position").value(2));
	}

	@Test
	void おかしいpositionなら400で保存しない() throws Exception {
		番号1のタスクを用意する();

		変更する(1, "{\"position\": 0}").andExpect(status().isBadRequest());
		変更する(1, "{\"position\": -1}").andExpect(status().isBadRequest());
		変更する(1, "{\"position\": 1.5}").andExpect(status().isBadRequest());
		変更する(1, "{\"position\": \"1\"}").andExpect(status().isBadRequest());
		変更する(1, "{\"position\": null}").andExpect(status().isBadRequest());
		verify(taskRepository, never()).save(any(Task.class));
		verify(taskRepository, never()).saveAll(any());
	}

	// --- 優先度順に並べる（POST /api/tasks/sort-by-priority） ---

	private static Task taskWithPriority(long id, String title, long position, String priority) {
		Task t = task(id, title, "todo", position);
		t.setPriority(priority);
		return t;
	}

	@Test
	void 優先度の高い順に並べなおして保存する() throws Exception {
		// いまの並び：低A, 高B, 中C, 高D, 中E
		Task a = taskWithPriority(1, "A", 1, "low");
		Task b = taskWithPriority(2, "B", 2, "high");
		Task c = taskWithPriority(3, "C", 3, "medium");
		Task d = taskWithPriority(4, "D", 4, "high");
		Task e = taskWithPriority(5, "E", 5, "medium");
		given(taskRepository.findByStatusOrderByPositionAsc("todo")).willReturn(List.of(a, b, c, d, e));

		mockMvc.perform(post("/api/tasks/sort-by-priority")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"todo\"}"))
				.andExpect(status().isOk())
				// 高B, 高D, 中C, 中E, 低A（同じ優先度どうしは、いまの順のまま）
				.andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.contains("B", "D", "C", "E", "A")))
				.andExpect(jsonPath("$[*].position").value(org.hamcrest.Matchers.contains(1, 2, 3, 4, 5)));
		verify(taskRepository).saveAll(any());
	}

	@Test
	void 空の列でもエラーにならない() throws Exception {
		given(taskRepository.findByStatusOrderByPositionAsc("done")).willReturn(List.of());

		mockMvc.perform(post("/api/tasks/sort-by-priority")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"done\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void 優先度順でおかしい状態なら400で保存しない() throws Exception {
		mockMvc.perform(post("/api/tasks/sort-by-priority")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"abc\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/tasks/sort-by-priority")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
		verify(taskRepository, never()).saveAll(any());
	}

}
