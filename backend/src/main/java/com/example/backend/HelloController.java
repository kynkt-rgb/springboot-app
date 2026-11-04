package com.example.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 動作確認用のコントローラー。
 * http://localhost:8080/api/hello にアクセスすると文字を返す。
 */
@RestController
public class HelloController {

	@GetMapping("/api/hello")
	public String hello() {
		return "こんにちは！はじめての Spring Boot!";
	}

}
