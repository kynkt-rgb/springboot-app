#!/bin/bash
# サーバーを決まったポート以外で起動するのを止めるフック（PreToolUse: Bash）
#   バックエンド（Spring Boot）… 8080 番
#   フロントエンド（Vite）     … 5173 番
# ポートがふさがっているときは、別の番号に逃げずに、
# そのポートを使っているプロセスを止めてから起動しなおす（CLAUDE.md 参照）。
input=$(cat)
cmd=$(printf '%s' "$input" | /usr/bin/python3 -c 'import sys,json; print(json.load(sys.stdin).get("tool_input",{}).get("command",""))' 2>/dev/null)

block() {
  echo "ブロックしました: $1 サーバーは決まったポート（バックエンド 8080 / フロントエンド 5173）でだけ起動します。ポートがふさがっていたら .claude/hooks/free-port.sh <番号> でそのプロセスを止めてから起動してください（CLAUDE.md 参照）。" >&2
  exit 2
}

# サーバーを起動するコマンドだけを調べる
if ! printf '%s' "$cmd" | grep -Eq '(bootRun|npm[[:space:]]+run[[:space:]]+(dev|preview)|npm[[:space:]]+(start|dev)|(^|[;&|[:space:]/])vite([[:space:]]|$)|npx[[:space:]]+vite|java[[:space:]].*-jar)'; then
  exit 0
fi

# Spring Boot のポート変更（--server.port=9090 / -Dserver.port=9090 / SERVER_PORT=9090）
if printf '%s' "$cmd" | grep -Eq 'server\.port[= ]+[0-9]+' && ! printf '%s' "$cmd" | grep -Eq 'server\.port[= ]+8080([^0-9]|$)'; then
  block "バックエンドのポートを 8080 以外にしようとしています。"
fi
if printf '%s' "$cmd" | grep -Eq 'SERVER_PORT=' && ! printf '%s' "$cmd" | grep -Eq 'SERVER_PORT=8080([^0-9]|$)'; then
  block "バックエンドのポートを 8080 以外にしようとしています。"
fi

# Vite のポート変更（--port 3000 / --port=3000）と、ポート固定の解除
if printf '%s' "$cmd" | grep -Eq -- '--port([= ]+)[0-9]+' && ! printf '%s' "$cmd" | grep -Eq -- '--port([= ]+)5173([^0-9]|$)'; then
  block "フロントエンドのポートを 5173 以外にしようとしています。"
fi
if printf '%s' "$cmd" | grep -Eq -- '--strictPort[= ]+false'; then
  block "ポートの固定（strictPort）を外そうとしています。"
fi

# PORT=3000 のような環境変数でのポート指定
if printf '%s' "$cmd" | grep -Eq '(^|[;&|[:space:]])PORT=[0-9]+' && ! printf '%s' "$cmd" | grep -Eq '(^|[;&|[:space:]])PORT=(8080|5173)([^0-9]|$)'; then
  block "PORT を 8080 / 5173 以外にしようとしています。"
fi

exit 0
