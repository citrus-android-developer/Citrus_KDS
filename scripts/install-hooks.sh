#!/usr/bin/env bash
# install-hooks.sh — 一鍵把整套圖譜 hook 系統裝起來
#
# 用法 (在 repo root 跑):
#   scripts/install-hooks.sh                  # 預設 copy 模式 (穩,update 要重跑)
#   scripts/install-hooks.sh --symlink        # symlink 模式 (你改 repo 對方馬上拿到, 但路徑硬綁)
#   scripts/install-hooks.sh --force          # 覆寫既有 hook (預設 skip)
#   scripts/install-hooks.sh --uninstall      # 移除 (回復原狀)
#
# 裝的東西:
#   1. git core.hooksPath → scripts/hooks/  (git 跑該目錄所有 native hook,一次到位:)
#        - pre-commit  : L2 硬擋(污染指紋 + code 無圖譜)
#        - post-commit : L2 觀測面(bypass 留痕)
#        - pre-push    : 第二·五道(push 前同步跑完整 lumos doctor,壞圖譜出不了本機)
#   2. Claude hooks 複製/symlink 到 ~/.claude/hooks/  (L1 Stop + L3 PostToolUse)
#   3. ~/.claude/settings.json 加 hook 註冊  (用 merge-claude-settings.py 安全合併)
#
# 依賴:
#   - git ≥ 2.9 (core.hooksPath)
#   - python3 (跑 merge script)
#   - Claude Code (L1/L3 才會 fire)
#   - Max 訂閱 + obsidian app + obsidian-cli (L3 + scripts/* 才能用)

set -u

MODE="copy"
FORCE=0
UNINSTALL=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --symlink)   MODE="symlink"; shift ;;
    --copy)      MODE="copy"; shift ;;
    --force)     FORCE=1; shift ;;
    --uninstall) UNINSTALL=1; shift ;;
    -h|--help)
      sed -n '2,/^$/p' "$0" | sed 's/^# //;s/^#//'
      exit 0 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

if [[ -t 1 ]]; then
  GREEN=$'\033[32m'; YELLOW=$'\033[33m'; DIM=$'\033[2m'; BOLD=$'\033[1m'; RESET=$'\033[0m'
else
  GREEN=''; YELLOW=''; DIM=''; BOLD=''; RESET=''
fi

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "ERROR: 不在 git repo 裡,要在專案 repo root 跑" >&2
  exit 2
}
cd "$REPO_ROOT"

HOOKS_REPO_DIR="$REPO_ROOT/scripts/hooks"
HOOKS_CLAUDE_SRC="$HOOKS_REPO_DIR/claude"
CLAUDE_HOOKS_DEST="$HOME/.claude/hooks"
CLAUDE_HOOK_FILES=("check-graph-sync.py" "verification-rot-check.py")

# ─── UNINSTALL ────────────────────────────────────────────────────────
if [[ $UNINSTALL -eq 1 ]]; then
  echo "${BOLD}Uninstalling...${RESET}"
  echo "  ${YELLOW}!${RESET} git core.hooksPath 回復預設"
  git config --unset core.hooksPath 2>/dev/null || true

  for hf in "${CLAUDE_HOOK_FILES[@]}"; do
    target="$CLAUDE_HOOKS_DEST/$hf"
    if [[ -L "$target" || -f "$target" ]]; then
      rm -f "$target"
      echo "  ${YELLOW}!${RESET} 刪掉 $target"
    fi
  done

  echo ""
  echo "  ${DIM}注意:settings.json 內 hook 註冊條目沒自動刪 (避免誤砍其他人改過的)${RESET}"
  echo "  ${DIM}      手動編輯 ~/.claude/settings.json 把相關 entry 移掉${RESET}"
  exit 0
fi

# ─── INSTALL ──────────────────────────────────────────────────────────
echo "${BOLD}install-hooks.sh${RESET} — mode=${BOLD}$MODE${RESET}"
echo ""

# Step 1: git native hooks (pre-commit + post-commit + pre-push 一次到位)
echo "${BOLD}[1/3]${RESET} git core.hooksPath → scripts/hooks/  (pre-commit + post-commit + pre-push)"
current="$(git config core.hooksPath 2>/dev/null || true)"
if [[ "$current" == "scripts/hooks" ]]; then
  echo "  ${GREEN}✓${RESET} 已設定"
else
  git config core.hooksPath scripts/hooks
  echo "  ${GREEN}✓${RESET} 設定完成 (從 '$current' → 'scripts/hooks')"
fi
for h in pre-commit post-commit pre-push; do
  if [[ -x "$HOOKS_REPO_DIR/$h" ]]; then
    echo "    ${GREEN}✓${RESET} $h"
  else
    echo "    ${YELLOW}!${RESET} $h 缺或不可執行(chmod +x scripts/hooks/$h)"
  fi
done
echo ""

# Step 2: Claude hooks
echo "${BOLD}[2/3]${RESET} Claude hooks 裝到 ~/.claude/hooks/"
mkdir -p "$CLAUDE_HOOKS_DEST"

for hf in "${CLAUDE_HOOK_FILES[@]}"; do
  src="$HOOKS_CLAUDE_SRC/$hf"
  dst="$CLAUDE_HOOKS_DEST/$hf"

  if [[ ! -f "$src" ]]; then
    echo "  ${YELLOW}!${RESET} source 缺: $src"
    continue
  fi

  if [[ -e "$dst" || -L "$dst" ]]; then
    if [[ $FORCE -eq 0 ]]; then
      # 看是否已對齊
      if [[ -L "$dst" && "$MODE" == "symlink" ]]; then
        actual="$(readlink "$dst")"
        if [[ "$actual" == "$src" ]]; then
          echo "  ${GREEN}✓${RESET} [link] $hf already correct symlink"
          continue
        fi
      fi
      if [[ -f "$dst" && ! -L "$dst" && "$MODE" == "copy" ]]; then
        if cmp -s "$src" "$dst"; then
          echo "  ${GREEN}✓${RESET} [copy] $hf already matches"
          continue
        fi
      fi
      echo "  ${YELLOW}!${RESET} $dst 已存在但跟 source 不同,加 --force 覆寫"
      continue
    fi
    rm -f "$dst"
  fi

  if [[ "$MODE" == "symlink" ]]; then
    ln -s "$src" "$dst"
    echo "  ${GREEN}✓${RESET} [link] $dst → $src"
  else
    cp "$src" "$dst"
    chmod +x "$dst"
    echo "  ${GREEN}✓${RESET} [copy] $dst"
  fi
done
echo ""

# Step 3: settings.json merge
echo "${BOLD}[3/3]${RESET} 註冊 hook 到 ~/.claude/settings.json"
if command -v python3 >/dev/null 2>&1; then
  python3 "$REPO_ROOT/scripts/merge-claude-settings.py"
else
  echo "  ${YELLOW}!${RESET} python3 不在 PATH,跳過 settings 自動 merge"
  echo "  ${DIM}手動編 ~/.claude/settings.json 加入 hooks 區段 (見 scripts/merge-claude-settings.py 內 HOOK_ENTRIES 常數)${RESET}"
fi
echo ""

# ─── 驗證 ──────────────────────────────────────────────────────────────
echo "${BOLD}─────────────────────────────────${RESET}"
echo "${GREEN}${BOLD}Install complete${RESET}"
echo ""
echo "${BOLD}驗證指令:${RESET}"
echo "  ${DIM}# git native hooks (pre-commit/post-commit/pre-push)${RESET}"
echo "  git config core.hooksPath        # 應顯示 scripts/hooks"
echo "  python3 scripts/lumos doctor  # pre-push 會跑這個,先手動確認 0 issues"
echo ""
echo "  ${DIM}# L1/L3 (Claude hooks)${RESET}"
echo "  ls -la ~/.claude/hooks/          # 應有 2 個 .py"
echo "  jq '.hooks' ~/.claude/settings.json"
echo ""
echo "${BOLD}下一步:${RESET}"
echo "  1. ${DIM}重啟 Claude Code session${RESET} (L1/L3 設定要 session start 才會載入)"
echo "  2. ${DIM}試做一個 commit${RESET} (改 .cs 但不更新 graph) — L2 應該擋"
echo "  3. ${DIM}讀 lumos-project-notes skill${RESET} 了解圖譜 schema 慣例"
