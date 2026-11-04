package com.example.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * データベース接続の確認用コントローラー。
 * http://localhost:8080/api/db-check にアクセスすると、
 * つながっている DB の名前と PostgreSQL のバージョンを返す。
 */
@RestController
public class DbCheckController {

	private final JdbcTemplate jdbcTemplate;

	public DbCheckController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/api/db-check")
	public String dbCheck() {
		String database = jdbcTemplate.queryForObject("select current_database()", String.class);
		String version = jdbcTemplate.queryForObject("show server_version", String.class);
		return "DB connected: " + database + " (PostgreSQL " + version + ")";
	}

}
