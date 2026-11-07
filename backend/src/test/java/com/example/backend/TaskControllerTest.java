package com.example.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

}
