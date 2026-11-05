package com.example.backend;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

}
