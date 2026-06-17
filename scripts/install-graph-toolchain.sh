#!/usr/bin/env bash
# install-graph-toolchain.sh — 把「圖譜即合約」整套工具鏈裝進另一個專案
#
# 用法（在「來源」repo＝本專案 root 跑）：
#   scripts/install-graph-toolchain.sh --target <專案路徑> [--slug <知識庫名>] [選項]
#
# 範例：
#   scripts/install-graph-toolchain.sh --target ~/backend/OtherApp --slug otherapp
#   scripts/install-graph-toolchain.sh --target ~/backend/OtherApp --dry-run
#
# 選項：
#   --slug <name>   知識庫資料夾名 → docs/<name>-knowledge/（給了才 scaffold；沒給跳過 scaffold）
#   --dry-run       只印會做什麼，不真的動檔
#   --no-hooks      跳過在目標 repo 跑 install-hooks.sh
#   --no-scaffold   跳過建 docs/<slug>-knowledge 骨架
#   -h|--help       說明
#
# 兩層冪等（再跑一次的語意）：
#   ▸ 工具鏈（lumos / skills / hooks 腳本）= 覆寫＝更新到最新（重跑就是升級）
#   ▸ 圖譜資料（docs/<slug>-knowledge 骨架 + 你的筆記）= 已存在就整個跳過，絕不覆寫（保護資料）

set -u

if [[ -t 1 ]]; then
  G=$'\033[32m'; Y=$'\033[33m'; B=$'\033[1m'; D=$'\033[2m'; R=$'\033[0m'
else
  G=''; Y=''; B=''; D=''; R=''
fi

TARGET=""; SLUG=""; DRY=0; DO_HOOKS=1; DO_SCAFFOLD=1
while [[ $# -gt 0 ]]; do
  case "$1" in
    --target) TARGET="${2:-}"; shift 2 ;;
    --slug)   SLUG="${2:-}"; shift 2 ;;
    --dry-run) DRY=1; shift ;;
    --no-hooks) DO_HOOKS=0; shift ;;
    --no-scaffold) DO_SCAFFOLD=0; shift ;;
    -h|--help) sed -n '2,/^$/p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

# 來源 = 本腳本所在 repo（scripts/ 的上一層）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$(cd "$SCRIPT_DIR/.." && pwd)"

[[ -z "$TARGET" ]] && { echo "ERROR: 需 --target <專案路徑>" >&2; exit 2; }
[[ -d "$TARGET" ]] || { echo "ERROR: target 不存在或非目錄: $TARGET" >&2; exit 2; }
TARGET="$(cd "$TARGET" && pwd)"
[[ "$TARGET" == "$SRC" ]] && { echo "ERROR: target 不能是來源 repo 自己" >&2; exit 2; }

echo "${B}install-graph-toolchain${R}  ${D}(dry-run=$DRY)${R}"
echo "  來源: $SRC"
echo "  目標: $TARGET"
[[ -d "$TARGET/.git" ]] || echo "  ${Y}!${R} 目標不是 git repo → hooks 步驟會失效（git hook 需 git）；建議先 git init"
echo

# ── 工具鏈檔（覆寫＝更新）──────────────────────────────────────────
TOOLCHAIN=(
  "scripts/lumos"
  "scripts/test_lumos.py"
  "scripts/install-hooks.sh"
  "scripts/merge-claude-settings.py"
  "scripts/graph-rename.sh"
  "scripts/fetch-notesmd.sh"
  "scripts/templates/graph-discipline.md"
)
copy_file() {  # $1 = rel path；覆寫更新，回報 created/updated
  local rel="$1" s="$SRC/$1" d="$TARGET/$1"
  [[ -e "$s" ]] || { echo "  ${Y}skip${R} 來源缺 $rel"; return; }
  local verb="created"; [[ -e "$d" ]] && verb="updated"
  if [[ "$DRY" == 1 ]]; then echo "  ${D}[dry]${R} $verb $rel"; return; fi
  mkdir -p "$(dirname "$d")"
  cp -f "$s" "$d"
  [[ -x "$s" ]] && chmod +x "$d"
  echo "  ${G}✓${R} $verb $rel"
}

copy_dir() {   # $1 = rel dir；clean replace（rm -rf + cp -R），回報 created/updated
  local rel="$1" s="$SRC/$1" d="$TARGET/$1"
  [[ -d "$s" ]] || { echo "  ${Y}skip${R} 來源缺 $rel"; return; }
  local verb="created"; [[ -d "$d" ]] && verb="updated"
  if [[ "$DRY" == 1 ]]; then echo "  ${D}[dry]${R} $verb $rel/"; return; fi
  rm -rf "$d"; mkdir -p "$(dirname "$d")"; cp -R "$s" "$d"
  echo "  ${G}✓${R} $verb $rel/"
}

echo "${B}[1/5]${R} 工具鏈（覆寫＝更新到最新）"
for f in "${TOOLCHAIN[@]}"; do copy_file "$f"; done
copy_dir "scripts/hooks"

echo
echo "${B}[2/5]${R} skills（user-scope，不在此處裝）"
# skills 升格為跨專案 user-scope 唯一源(lumos-toolchain repo,symlink 進 ~/.claude/skills)。
# 不再複製進各 project,避免散落漂移。這裡只檢查使用者那邊裝了沒。
if [[ -e "$HOME/.claude/skills/lumos-project-notes" ]]; then
  echo "  ${G}✓${R} ~/.claude/skills/lumos-project-notes 已在(user-scope,所有專案共用)"
else
  echo "  ${Y}!${R} 未偵測到 user-scope lumos skills。請 clone lumos-toolchain repo 後跑一次 ./install.sh:"
  echo "      ${D}git clone <lumos-toolchain> ~/harness/lumos-toolchain && ~/harness/lumos-toolchain/install.sh${R}"
fi

# ── 圖譜骨架（已存在就跳過，絕不覆寫資料）──────────────────────────
echo
echo "${B}[3/5]${R} 圖譜骨架 docs/<slug>-knowledge（已存在 → 跳過保護資料）"
if [[ "$DO_SCAFFOLD" == 0 ]]; then
  echo "  ${D}--no-scaffold,跳過${R}"
elif [[ -z "$SLUG" ]]; then
  echo "  ${Y}!${R} 沒給 --slug,跳過 scaffold（工具鏈仍已裝；之後手動建 docs/<name>-knowledge）"
else
  KG="$TARGET/docs/$SLUG-knowledge"
  if [[ -d "$KG" ]]; then
    echo "  ${Y}skip${R} $KG 已存在 → 不動（保護既有筆記）"
  elif [[ "$DRY" == 1 ]]; then
    echo "  ${D}[dry]${R} 建 $KG/{Systems,Verification,Projects,Issues,Sessions,MOC} + MOC/index.md"
  else
    for sub in Systems Verification Projects Issues Sessions MOC; do mkdir -p "$KG/$sub"; done
    cat > "$KG/MOC/index.md" <<MOC
---
type: moc
status: doing
created: $(date +%F)
tags:
  - type/moc
---
# $SLUG 知識圖譜總索引

> 用 \`python3 scripts/lumos new system <名稱>\` 建第一個系統節點。
MOC
    echo "  ${G}✓${R} created $KG/(Systems/Verification/Projects/Issues/Sessions/MOC + MOC/index.md)"
  fi
fi

# ── CLAUDE.md 圖譜紀律(範本注入,idempotent)─────────────────────
echo
echo "${B}[4/5]${R} CLAUDE.md 圖譜先行紀律(有則更新到最新範本,無則注入)"
if [[ "$DRY" == 1 ]]; then
  echo "  ${D}[dry]${R} 注入/更新 $TARGET/CLAUDE.md 的 LUMOS:GRAPH-DISCIPLINE 區塊"
else
  KG_REL=$([[ -n "$SLUG" ]] && echo "docs/$SLUG-knowledge/" || echo "docs/<專案>-knowledge/")
  python3 - "$TARGET" "$KG_REL" <<'PYEOF'
import sys, re, pathlib
target, kg = sys.argv[1], sys.argv[2]
tpl = pathlib.Path(target) / "scripts/templates/graph-discipline.md"
if not tpl.exists():
    print("  ! 範本缺(scripts/templates/graph-discipline.md),跳過"); sys.exit(0)
body = tpl.read_text(encoding="utf-8").replace("{{KG}}", kg).strip()
START = "<!-- LUMOS:GRAPH-DISCIPLINE:START — 自動注入/更新,勿手改本區塊;改範本 scripts/templates/graph-discipline.md -->"
END = "<!-- LUMOS:GRAPH-DISCIPLINE:END -->"
block = START + "\n" + body + "\n" + END
cm = pathlib.Path(target) / "CLAUDE.md"
if not cm.exists():
    cm.write_text("# CLAUDE.md\n\n" + block + "\n", encoding="utf-8")
    print("  + 建 CLAUDE.md 並注入紀律區塊"); sys.exit(0)
t = cm.read_text(encoding="utf-8")
if "LUMOS:GRAPH-DISCIPLINE:START" in t:
    t = re.sub(r"<!-- LUMOS:GRAPH-DISCIPLINE:START.*?LUMOS:GRAPH-DISCIPLINE:END -->",
               lambda _m: block, t, flags=re.S)
    cm.write_text(t, encoding="utf-8"); print("  ~ 更新既有紀律區塊到最新範本")
else:
    lines = t.split("\n"); ins = 0
    for i, l in enumerate(lines):
        if l.startswith("# "): ins = i + 1; break
    lines.insert(ins, "\n" + block + "\n")
    cm.write_text("\n".join(lines), encoding="utf-8"); print("  + 注入紀律區塊(置於 H1 之後)")
PYEOF
fi

# ── 在目標 repo 跑 install-hooks ─────────────────────────────────
echo
echo "${B}[5/5]${R} 在目標 repo 安裝 hooks"
if [[ "$DO_HOOKS" == 0 ]]; then
  echo "  ${D}--no-hooks,跳過${R}"
elif [[ ! -d "$TARGET/.git" ]]; then
  echo "  ${Y}skip${R} 目標非 git repo,略過（git init 後在目標 repo 跑 scripts/install-hooks.sh --force）"
elif [[ "$DRY" == 1 ]]; then
  echo "  ${D}[dry]${R} cd $TARGET && scripts/install-hooks.sh --force"
else
  ( cd "$TARGET" && scripts/install-hooks.sh --force )
fi

echo
echo "${B}${G}完成${R}"
echo "  後續（在目標 repo ${TARGET} ）："
echo "    1. ${D}python3 scripts/lumos install${R}     # 選用:lumos 上 ~/.local/bin 全域可用"
echo "    2. ${D}python3 scripts/lumos doctor${R}      # 確認圖譜健康"
echo "    3. ${D}git add scripts docs && git commit${R}   # 工具鏈(lumos/hooks)+骨架進該 repo,團隊 git pull 即共享"
echo "    4. skills 是 user-scope(lumos-toolchain repo,每台機器一次):見上方 [2/5] 提示"
echo "  再跑一次本指令 = 工具鏈更新到最新;圖譜資料不會被動。"
