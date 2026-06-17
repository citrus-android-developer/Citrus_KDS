#!/usr/bin/env python3
"""全域 Stop hook: 提醒「程式碼改了但圖譜沒同步」。

只在當前專案有 docs/*-knowledge/ 或 docs/knowledge/ 時作用,否則完全闭嘴。
軟提醒 (stderr surface 給 Claude),不 block turn 結束。

四層閘門:
  0  圖譜不存在               → exit 0
  1  這 turn 沒改任何檔        → exit 0
  2  改的都是非原始碼/在 docs/ → exit 0
  3  這 turn 已動過圖譜        → exit 0
  否則                         → 印 stderr 提醒
"""
from __future__ import annotations
import json
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path

# === 觸發提醒的原始碼副檔名 ===
CODE_EXTS = {
    ".cs",                                                    # C# / .NET
    ".vue", ".js", ".ts", ".tsx", ".jsx", ".mjs",             # 前端
    ".sql",                                                   # DB migration
    ".py",                                                    # Python
    ".kt", ".kts",                                            # Kotlin / Compose
    ".java",                                                  # Java
    ".swift",                                                 # Swift
    ".go",                                                    # Go
    ".rs",                                                    # Rust
    ".c", ".cc", ".cpp", ".h", ".hpp",                        # C/C++
}

# === 即使副檔名對也要排除的路徑/檔名 ===
EXCLUDE_PATH_CONTAINS = (
    "/docs/",            # 圖譜本身 + 一般文件 (.md 全排除靠這個 + 副檔名清單)
    "/node_modules/",
    "/bin/", "/obj/",
    "/.git/",
    "/dist/", "/build/",
    "/__pycache__/",
)
EXCLUDE_FILENAMES = {
    "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
}

EDIT_TOOLS = {"Edit", "Write", "MultiEdit"}

# === #2 收緊 ===
# obsidian CLI 子命令裡「真的會 mutate 圖譜」的清單。
# Read-only 子命令 (search/backlinks/files/orphans/...) 不算「動過圖譜」,
# 避免 `obsidian --help` / `cat ...lumos-project-notes...` 之類純查詢誤判靜音。
OBSIDIAN_WRITE_SUBCMDS = {
    "create", "append", "prepend", "delete",
    "move", "rename",
    "property:set", "property:remove",
    "daily:append", "daily:prepend",
    "base:create",
    "template:insert",
}

# === #6 補抓 Bash 檔案異動 ===
# 由 rm/mv/cp/git mv/git rm 製造的檔案變動。
# 不處理 find -delete、brace expansion、xargs rm 之類 corner case,只覆蓋常見手寫情境。
BASH_FILE_OPS_PATH_BEARING = {"rm", "mv", "cp", "git rm", "git mv"}


def find_graph_root(project_root: Path) -> Path | None:
    """找到此專案的圖譜目錄,沒有就回 None (代表沒用這套系統)。"""
    docs = project_root / "docs"
    if not docs.is_dir():
        return None
    # 新慣例: docs/{slug}-knowledge/
    for child in docs.iterdir():
        if child.is_dir() and child.name.endswith("-knowledge"):
            return child
    # 舊慣例: docs/knowledge/
    legacy = docs / "knowledge"
    return legacy if legacy.is_dir() else None


def _is_real_user_input(obj: dict) -> bool:
    """區分「真實 user 輸入」vs「tool_result(也被標 type=user)」。

    Claude Code transcript 把 tool 回應記成 type=user + content[0].type=tool_result,
    若 turn 切點誤切在 tool_result,會漏報這 turn 前面的改動(風險四)。
    """
    if obj.get("type") != "user":
        return False
    msg = obj.get("message", {})
    if not isinstance(msg, dict):
        return False
    content = msg.get("content", "")
    if isinstance(content, str):
        return True  # 純文字 user 輸入
    if isinstance(content, list) and content:
        first = content[0]
        if isinstance(first, dict):
            return first.get("type") != "tool_result"
    return False


def collect_turn_actions(transcript_path: Path):
    """從 transcript 尾部反向掃,到最近一個「真實 user 輸入」為止
    (排除 tool_result 之類也被標 type=user 的雜訊)。
    回傳 (file_paths, bash_commands)。
    """
    if not transcript_path.is_file():
        return [], []
    lines = transcript_path.read_text(encoding="utf-8", errors="ignore").splitlines()
    turn_lines = []
    for line in reversed(lines):
        if not line.strip():
            continue
        try:
            obj = json.loads(line)
        except json.JSONDecodeError:
            continue
        if _is_real_user_input(obj):
            break
        turn_lines.append(obj)
    turn_lines.reverse()

    file_paths: list[str] = []
    bash_commands: list[str] = []
    for obj in turn_lines:
        if obj.get("type") != "assistant":
            continue
        for item in obj.get("message", {}).get("content", []) or []:
            if not isinstance(item, dict) or item.get("type") != "tool_use":
                continue
            name = item.get("name", "")
            inp = item.get("input", {}) or {}
            if name in EDIT_TOOLS:
                fp = inp.get("file_path", "")
                if fp:
                    file_paths.append(fp)
            elif name == "Bash":
                cmd = inp.get("command", "")
                if cmd:
                    bash_commands.append(cmd)
    return file_paths, bash_commands


def is_code_file(path: str, project_root: Path) -> bool:
    p = Path(path)
    if p.suffix.lower() not in CODE_EXTS:
        return False
    # 必須在 project_root 之下;避免改 ~/.claude/、/tmp 等外部檔案被誤判
    try:
        p.resolve().relative_to(project_root.resolve())
    except (ValueError, OSError):
        return False
    norm = str(p).replace("\\", "/")
    if any(seg in norm for seg in EXCLUDE_PATH_CONTAINS):
        return False
    if p.name in EXCLUDE_FILENAMES:
        return False
    return True


def is_graph_file(path: str, graph_root: Path) -> bool:
    """檔案是否在圖譜資料夾底下 (任何 .md)。"""
    p = Path(path)
    if p.suffix.lower() != ".md":
        return False
    try:
        p.resolve().relative_to(graph_root.resolve())
        return True
    except (ValueError, OSError):
        return False


def _segment_command(cmd: str) -> list[str]:
    """切 shell chain (`&&` / `||` / `;` / `|`)。Quote-aware 不嚴格,但對常見 case 夠用。"""
    return [s.strip() for s in re.split(r'\s*(?:&&|\|\||;|\|)\s*', cmd) if s.strip()]


def _tokens_of(segment: str) -> list[str]:
    try:
        return shlex.split(segment)
    except ValueError:
        return []


def touched_graph_via_cli(bash_commands: list[str]) -> bool:
    """這 turn 是否真的「寫」過圖譜 (#2 收緊):
       只有 obsidian CLI 用了 mutate 子命令 (create/append/property:set 等) 才算。
       Read-only 子命令 / `obsidian --help` / 路徑裡含 obsidian 字串的非 obsidian command 都不算。
    """
    for cmd in bash_commands:
        for seg in _segment_command(cmd):
            tokens = _tokens_of(seg)
            if not tokens:
                continue
            # 跳過 leading env vars (`FOO=bar obsidian ...`)
            idx = 0
            while idx < len(tokens) and "=" in tokens[idx] and not tokens[idx].startswith("-"):
                idx += 1
            if idx >= len(tokens) or tokens[idx] != "obsidian":
                continue
            # 從 obsidian 後找第一個非 key=value 的 token,即為子命令
            for t in tokens[idx + 1:]:
                if t.startswith("-"):
                    continue
                if "=" in t and not t.startswith("="):
                    continue
                if t in OBSIDIAN_WRITE_SUBCMDS:
                    return True
                break  # 遇到第一個 positional 但非 write subcmd → 結束此 segment
    return False


def extract_bash_file_paths(bash_commands: list[str], project_root: Path) -> list[str]:
    """#6: 從 rm/mv/cp/git rm/git mv 命令裡撈被影響的檔案路徑。

    回傳「絕對路徑字串」list,讓後續 is_code_file 統一處理。
    相對路徑視為相對 project_root (Bash tool 的 cwd 通常就是 project_root)。
    """
    out: list[str] = []
    for cmd in bash_commands:
        for seg in _segment_command(cmd):
            tokens = _tokens_of(seg)
            if not tokens:
                continue
            # 跳過 leading env vars
            i = 0
            while i < len(tokens) and "=" in tokens[i] and not tokens[i].startswith("-"):
                i += 1
            if i >= len(tokens):
                continue
            head = tokens[i]
            args = tokens[i + 1:]
            # 處理 "git rm" / "git mv"
            if head == "git" and args:
                sub = args[0]
                if sub in ("rm", "mv"):
                    head = f"git {sub}"
                    args = args[1:]
                else:
                    continue
            if head not in BASH_FILE_OPS_PATH_BEARING:
                continue
            # 過濾掉 flag,剩下都是路徑候選
            paths = [a for a in args if not a.startswith("-")]
            if not paths:
                continue
            if head == "cp":
                # cp [opts] SRC... DST → DST 是新檔
                if len(paths) >= 2:
                    paths = [paths[-1]]
                else:
                    continue
            # 標準化成絕對路徑
            for p in paths:
                pp = Path(p)
                if not pp.is_absolute():
                    pp = project_root / pp
                out.append(str(pp))
    return out


def find_notes_mentioning(rel_paths: list[str], graph_root: Path) -> dict[str, list[str]]:
    """#5: 用 obsidian CLI search 反查每個改的檔案在哪幾篇圖譜筆記出現。

    搜尋以「檔名 stem」為 query (PointService.cs → 'PointService'),
    既捕捉檔名直接引用,也捕捉透過 class/symbol name 的提及。

    若 obsidian CLI 不可用 (app 沒開 / CLI 未安裝),回 {} 讓警告維持基本版。
    """
    vault_name = graph_root.name
    stems: list[str] = []
    seen: set[str] = set()
    for fp in rel_paths:
        stem = Path(fp).stem
        # 短 stem (<=2 字元) 跳過,搜出來會全是噪音
        if not stem or len(stem) <= 2 or stem in seen:
            continue
        seen.add(stem)
        stems.append(stem)
    stems = stems[:5]  # 控成本

    if not stems:
        return {}

    result: dict[str, list[str]] = {}
    for stem in stems:
        try:
            proc = subprocess.run(
                ["obsidian", f"vault={vault_name}", "search", f"query={stem}", "limit=5"],
                capture_output=True, text=True, timeout=2,
            )
        except (FileNotFoundError, subprocess.TimeoutExpired, OSError):
            return {}  # obsidian 全面不可用,放棄 enrichment
        if proc.returncode != 0:
            continue
        notes = [
            ln.strip() for ln in proc.stdout.splitlines()
            if ln.strip() and ln.strip().endswith(".md")
        ]
        if notes:
            result[stem] = notes[:3]
    return result


def emit_queue_patrol(project_root: Path) -> None:
    """B (2026-05-25): Stop hook 巡邏 .rot-queue.jsonl,
    堆積到一定量就 stderr 提醒 — 避免 L3 寫入後沒人消化變黑洞。

    機械式不靠記得;用 Stop hook 既有機制不引入 launchd 新失效面。

    閾值:>= 3 個 finding 才提醒 (避免 single 噪音)
    """
    queue_path = project_root / "docs" / ".rot-queue.jsonl"
    if not queue_path.is_file():
        return
    try:
        entries = []
        with queue_path.open(encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    entries.append(json.loads(line))
                except json.JSONDecodeError:
                    pass
    except OSError:
        return
    if len(entries) < 3:
        return
    verifs = {e.get("verification", "") for e in entries if isinstance(e, dict)}
    ts_values = [e.get("ts", "") for e in entries if isinstance(e, dict)]
    oldest = min((t for t in ts_values if t), default="?")
    print(
        f"📋 rot-queue 累積 {len(entries)} 筆 finding 涵蓋 {len(verifs)} 篇 Verification "
        f"(oldest: {oldest[:10]})。"
        f"\n   跑 `scripts/rot-queue-digest.sh` review 後 `--clear`,避免堆成黑洞。",
        file=sys.stderr,
    )


def main() -> int:
    try:
        payload = json.loads(sys.stdin.read())
    except json.JSONDecodeError:
        return 0  # 寧可漏報

    project_root_str = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd")
    if not project_root_str:
        return 0
    project_root = Path(project_root_str)

    # 閘門 0
    graph_root = find_graph_root(project_root)
    if graph_root is None:
        return 0

    # B 巡邏: queue 堆積就提醒 (在主邏輯前跑,獨立於 code-sync warning)
    emit_queue_patrol(project_root)

    # 閘門 1
    transcript_path_str = payload.get("transcript_path", "")
    if not transcript_path_str:
        return 0
    file_paths, bash_commands = collect_turn_actions(Path(transcript_path_str))
    # #6: 補上 Bash rm/mv/cp/git mv/git rm 影響的檔案
    file_paths = file_paths + extract_bash_file_paths(bash_commands, project_root)
    if not file_paths and not bash_commands:
        return 0

    # 閘門 2
    src_files = [f for f in file_paths if is_code_file(f, project_root)]
    if not src_files:
        return 0

    # 閘門 3
    graph_touched_via_edit = any(is_graph_file(f, graph_root) for f in file_paths)
    if graph_touched_via_edit or touched_graph_via_cli(bash_commands):
        return 0

    # ── 印提醒 ──
    project_root_resolved = project_root.resolve()
    rel: list[str] = []
    seen: set[str] = set()
    for f in src_files:
        try:
            r = str(Path(f).resolve().relative_to(project_root_resolved))
        except (ValueError, OSError):
            r = f
        if r not in seen:
            seen.add(r)
            rel.append(r)

    try:
        graph_rel = graph_root.resolve().relative_to(project_root_resolved)
    except (ValueError, OSError):
        graph_rel = graph_root

    msg = [
        f"⚠️  這個 turn 改了 {len(rel)} 個原始碼檔但沒看到對應的圖譜更新:",
        *[f"   • {r}" for r in rel],
        "",
        f"圖譜位置: {graph_rel}/",
    ]

    # #5: 反查改的檔案出現在哪幾篇筆記
    mentions = find_notes_mentioning(rel, graph_root)
    if mentions:
        msg += ["", "以下圖譜筆記提到這些檔名/symbol (可能需要更新):"]
        for stem, notes in mentions.items():
            msg.append(f"   • {stem} → {', '.join(notes)}")

    msg += [
        "",
        "依 lumos-project-notes skill,功能異動要同步:",
        "   - Systems/*.md (受影響的 system note)",
        "   - Verification/{date}_{topic}.md (驗證紀錄)",
        "   - decisions 區塊 (若有設計選擇)",
        "",
        "若這次只是 typo / refactor / 格式不改變行為,可忽略此提醒。",
    ]
    print("\n".join(msg), file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
