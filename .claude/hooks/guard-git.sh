#!/bin/bash
# main への直接 commit / push と force push を止めるフック（PreToolUse: Bash）
input=$(cat)
cmd=$(printf '%s' "$input" | /usr/bin/python3 -c 'import sys,json; print(json.load(sys.stdin).get("tool_input",{}).get("command",""))' 2>/dev/null)

block() {
  echo "ブロックしました: $1 先にイシューを作り、<種類>/<番号>-<名前> のブランチを切って作業してください（CLAUDE.md 参照）。" >&2
  exit 2
}

# git push
if printf '%s' "$cmd" | grep -Eq '(^|[;&|[:space:]])git[[:space:]]+push'; then
  if printf '%s' "$cmd" | grep -Eq -- '(--force|--force-with-lease|[[:space:]]-f([[:space:]]|$)|[[:space:]]\+[^[:space:]]+)'; then
    block "force push は禁止です。"
  fi
  if printf '%s' "$cmd" | grep -Eq '[[:space:]:]main([[:space:]]|$)'; then
    block "main への push は禁止です。"
  fi
  branch=$(git -C "${CLAUDE_PROJECT_DIR:-.}" branch --show-current 2>/dev/null)
  if [ "$branch" = "main" ]; then
    block "今のブランチが main です。"
  fi
fi

# git commit
if printf '%s' "$cmd" | grep -Eq '(^|[;&|[:space:]])git[[:space:]]+commit'; then
  branch=$(git -C "${CLAUDE_PROJECT_DIR:-.}" branch --show-current 2>/dev/null)
  if [ "$branch" = "main" ]; then
    block "main 上でのコミットは禁止です。"
  fi
fi

exit 0
