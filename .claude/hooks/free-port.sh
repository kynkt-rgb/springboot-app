#!/bin/bash
# アプリのポート（8080 / 5173）を使っているプロセスを止める。
# 使い方（springboot-app フォルダで）:
#   .claude/hooks/free-port.sh 8080
#   .claude/hooks/free-port.sh 5173
# DB（5432 / 5433）などを止めてしまわないよう、この2つの番号だけを受けつける。
port="$1"
case "$port" in
  8080|5173) ;;
  *) echo "使えるのは 8080 か 5173 だけです（指定: '$port'）" >&2; exit 1 ;;
esac

pids=$(lsof -nP -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null)
if [ -z "$pids" ]; then
  echo "$port 番を使っているプロセスはありません"
  exit 0
fi

echo "$port 番を使っているプロセスを止めます:"
lsof -nP -iTCP:"$port" -sTCP:LISTEN
kill $pids
# 止まるまで最大10秒まつ。止まらなければ強制終了する
for _ in $(seq 1 20); do
  sleep 0.5
  lsof -nP -t -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1 || { echo "$port 番が空きました"; exit 0; }
done
kill -9 $pids 2>/dev/null
sleep 0.5
if lsof -nP -t -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "$port 番がまだ使われています" >&2
  exit 1
fi
echo "$port 番が空きました（強制終了）"
