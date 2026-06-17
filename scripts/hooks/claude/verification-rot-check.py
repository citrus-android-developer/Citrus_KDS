#!/usr/bin/env python3
"""PostToolUse hook: 偵測 git commit 是否使既有 Verification 結論失效。

Triggered by: PostToolUse on Bash matching `git commit` (any variant).
Strategy:
  1. After commit ran, read last commit (HEAD~1..HEAD) diff
  2. Find candidate Verifications via obsidian search (stem of changed code files)
  3. For each candidate (max 5), ask Sonnet via `claude -p` to judge if diff
     invalidates the verification's conclusion
  4. If invalidates with confidence >= 0.7:
       (a) print stderr warning surface to Claude
       (b) append to docs/.rot-queue.jsonl for weekly digest review

Design choices:
  - Sonnet via Max subscription (no API key), see `claude -p --model sonnet`
  - Never blocks commit (exit 0 always)
  - Graceful degrade: obsidian unavailable / claude unavailable → silent
  - Cost guard: skip if diff < 10 lines, > 2000 lines, no code files, etc.

Reference: arxiv 2603.00489 (README rot detection, ICSE 2026)
  Best 30B model: Specificity 99%, Recall 52%, MRR 0.64 → use as high-precision
  signal (rare alarms, always taken seriously), not the only safety net.
"""
from __future__ import annotations
import hashlib
import json
import os
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path

# Reuse PreToolUse hook's git commit regex
GIT_COMMIT_RE = re.compile(r'(^|;|&&|\|\|)\s*git(\s+-C\s+\S+)?\s+commit\b')

CODE_EXTS = {
    ".cs", ".vue", ".js", ".ts", ".tsx", ".jsx", ".mjs",
    ".sql", ".py", ".kt", ".kts", ".java", ".swift", ".go", ".rs",
    ".c", ".cc", ".cpp", ".h", ".hpp",
}

# Tunable thresholds
MAX_CANDIDATES = 5
MIN_DIFF_LINES = 10           # 太小 (typo) 不檢查
MAX_DIFF_LINES = 2000         # 太大 (rename/move 全檔) 不檢查
MAX_DIFF_CHARS_PROMPT = 4000
MAX_VERIFICATION_CHARS_PROMPT = 1500
CLAUDE_TIMEOUT = 25           # 每次 claude -p 上限
CONFIDENCE_THRESHOLD = 0.7    # 低於此忽略 (對應 paper Spec 99% 設計)

# C (2026-05-25): cache LLM 判斷結果避免同 (commit, verification, 內容版本) 重算
# 不降 candidates / 不退 Haiku,純加 cache,保住 paper recall 52% 不再降
CACHE_FILENAME = ".rot-check-cache.json"   # 在 docs/ 下,gitignored
CACHE_MAX_ENTRIES = 500                     # rolling LRU


def find_graph_root(project_root: Path) -> Path | None:
    docs = project_root / "docs"
    if not docs.is_dir():
        return None
    for child in docs.iterdir():
        if child.is_dir() and child.name.endswith("-knowledge"):
            return child
    legacy = docs / "knowledge"
    return legacy if legacy.is_dir() else None


def get_changed_code_files(project_root: Path) -> tuple[list[str], int]:
    """Return (changed code files, total diff lines insert+delete)."""
    try:
        names_result = subprocess.run(
            ["git", "diff", "HEAD~1", "HEAD", "--name-only"],
            cwd=str(project_root),
            capture_output=True, text=True, timeout=5,
        )
        if names_result.returncode != 0:
            return [], 0
        all_files = [l.strip() for l in names_result.stdout.splitlines() if l.strip()]
        code_files = [f for f in all_files if Path(f).suffix.lower() in CODE_EXTS]

        stat_result = subprocess.run(
            ["git", "diff", "HEAD~1", "HEAD", "--shortstat"],
            cwd=str(project_root),
            capture_output=True, text=True, timeout=5,
        )
        diff_lines = 0
        if stat_result.returncode == 0:
            m = re.search(r'(\d+) insertions?\(\+\)', stat_result.stdout)
            if m: diff_lines += int(m.group(1))
            m = re.search(r'(\d+) deletions?\(\-\)', stat_result.stdout)
            if m: diff_lines += int(m.group(1))
        return code_files, diff_lines
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return [], 0


def get_diff_text(project_root: Path) -> str:
    try:
        result = subprocess.run(
            ["git", "diff", "HEAD~1", "HEAD"],
            cwd=str(project_root),
            capture_output=True, text=True, timeout=5,
        )
        return result.stdout if result.returncode == 0 else ""
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return ""


def get_commit_sha(project_root: Path) -> str:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=str(project_root),
            capture_output=True, text=True, timeout=3,
        )
        return result.stdout.strip()[:12] if result.returncode == 0 else ""
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return ""


def find_candidate_verifications(code_files: list[str], graph_root: Path) -> list[Path]:
    """For each changed code file's basename stem, search Verification/ via
    obsidian CLI. Returns absolute Path list."""
    vault_name = graph_root.name
    candidates: list[Path] = []
    seen_paths: set[str] = set()

    stems: list[str] = []
    seen_stems: set[str] = set()
    for f in code_files:
        stem = Path(f).stem
        if not stem or len(stem) <= 2 or stem in seen_stems:
            continue
        seen_stems.add(stem)
        stems.append(stem)
    stems = stems[:5]

    for stem in stems:
        try:
            result = subprocess.run(
                ["obsidian", f"vault={vault_name}", "search",
                 f"query={stem}", "limit=5"],
                capture_output=True, text=True, timeout=3,
            )
        except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
            return []  # obsidian 不可用,完全放棄 (graceful degrade)
        if result.returncode != 0:
            continue
        for line in result.stdout.splitlines():
            line = line.strip()
            if not line.endswith(".md"):
                continue
            # 只看活躍 Verification (排除 Archive)
            if not line.startswith("Verification/") or line.startswith("Verification/Archive/"):
                continue
            if line in seen_paths:
                continue
            seen_paths.add(line)
            candidates.append(graph_root / line)
            if len(candidates) >= MAX_CANDIDATES:
                return candidates
    return candidates


def call_claude_sonnet(prompt: str) -> dict | None:
    """Call `claude -p --model sonnet --output-format json`.
    Return parsed inner JSON dict, or None on any failure."""
    try:
        result = subprocess.run(
            ["claude", "-p", "--model", "sonnet", "--output-format", "json"],
            input=prompt,
            capture_output=True, text=True, timeout=CLAUDE_TIMEOUT,
        )
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return None
    if result.returncode != 0 or not result.stdout:
        return None
    try:
        wrapper = json.loads(result.stdout)
    except json.JSONDecodeError:
        return None
    if wrapper.get("is_error") or wrapper.get("subtype") != "success":
        return None
    inner_text = (wrapper.get("result") or "").strip()
    if not inner_text:
        return None
    # Strip possible markdown fence
    if inner_text.startswith("```"):
        lines = inner_text.split("\n")
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        inner_text = "\n".join(lines).strip()
    try:
        return json.loads(inner_text)
    except json.JSONDecodeError:
        return None


def _cache_key(commit_sha: str, verification_path: str, verification_text: str, diff_text: str) -> str:
    """C: cache key = (commit, verification_path, verification_content_hash, diff_content_hash)。
    內容 hash 確保 Verification 更新或同 commit 不同 diff 都會 cache miss。"""
    v_hash = hashlib.sha1(verification_text.encode("utf-8", errors="ignore")).hexdigest()[:12]
    d_hash = hashlib.sha1(diff_text.encode("utf-8", errors="ignore")).hexdigest()[:12]
    return f"{commit_sha[:12]}:{verification_path}:{v_hash}:{d_hash}"


def _read_cache(cache_path: Path) -> dict:
    if not cache_path.is_file():
        return {}
    try:
        return json.loads(cache_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}


def _write_cache(cache_path: Path, cache: dict) -> None:
    # Rolling LRU: 超過 CACHE_MAX_ENTRIES 刪最舊 (依插入順序;Python 3.7+ dict 有序)
    if len(cache) > CACHE_MAX_ENTRIES:
        # 砍最舊的 1/4
        excess = len(cache) - int(CACHE_MAX_ENTRIES * 0.75)
        for k in list(cache.keys())[:excess]:
            cache.pop(k, None)
    try:
        cache_path.parent.mkdir(parents=True, exist_ok=True)
        cache_path.write_text(json.dumps(cache, ensure_ascii=False), encoding="utf-8")
    except OSError:
        pass


def build_prompt(verification_path: Path, verification_text: str, diff_text: str) -> str:
    diff_excerpt = diff_text[:MAX_DIFF_CHARS_PROMPT]
    if len(diff_text) > MAX_DIFF_CHARS_PROMPT:
        diff_excerpt += f"\n[... diff truncated, total {len(diff_text)} chars ...]"
    verification_excerpt = verification_text[:MAX_VERIFICATION_CHARS_PROMPT]
    if len(verification_text) > MAX_VERIFICATION_CHARS_PROMPT:
        verification_excerpt += f"\n[... verification truncated ...]"
    return f"""你的任務:判斷 git diff 是否使指定的 Verification 結論失效。

定義「失效」(invalidates=true):
  diff 修改了 Verification 中「被驗證的符號 / SQL / 邏輯路徑 / API 契約」的實際行為。
不算失效 (invalidates=false):
  rename、純格式、註解、log 文字、import 重排、空白、JSDoc 補充。

[VERIFICATION] path={verification_path.name}
{verification_excerpt}

[DIFF]
{diff_excerpt}

只輸出 JSON,不要 markdown fence,schema:
{{
  "invalidates": true|false,
  "confidence": 0.0,
  "reason": "<= 80 字一句話說明",
  "affected_symbols": ["string"],
  "suggested_action": "mark_stale" | "rerun_test" | "no_action"
}}"""


def main() -> int:
    try:
        payload = json.loads(sys.stdin.read())
    except json.JSONDecodeError:
        return 0

    if payload.get("tool_name") != "Bash":
        return 0

    cmd = (payload.get("tool_input") or {}).get("command", "")
    if not cmd or not GIT_COMMIT_RE.search(cmd):
        return 0

    project_root_str = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd")
    if not project_root_str:
        return 0
    project_root = Path(project_root_str)

    graph_root = find_graph_root(project_root)
    if graph_root is None:
        return 0

    code_files, diff_lines = get_changed_code_files(project_root)
    if not code_files:
        return 0
    if diff_lines < MIN_DIFF_LINES:
        return 0
    if diff_lines > MAX_DIFF_LINES:
        print(
            f"[rot-check] diff 太大 ({diff_lines} 行),跳過自動檢查;"
            "請手動審視相關 Verifications",
            file=sys.stderr,
        )
        return 0

    candidates = find_candidate_verifications(code_files, graph_root)
    if not candidates:
        return 0

    diff_text = get_diff_text(project_root)
    if not diff_text:
        return 0

    # C: cache 提早讀 (commit_sha + verification_path + 內容 hash 為 key)
    commit_sha = get_commit_sha(project_root)
    cache_path = graph_root.parent / CACHE_FILENAME
    cache = _read_cache(cache_path)
    cache_dirty = False

    # 平行跑 claude -p (Sonnet 單次 ~15-20s,5 候選 sequential 會超過 hook timeout)
    def check_one(cand: Path) -> dict | None:
        nonlocal cache_dirty
        try:
            verification_text = cand.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            return None
        try:
            rel_path = str(cand.relative_to(project_root))
        except ValueError:
            rel_path = str(cand)

        # C: cache lookup
        cache_key = _cache_key(commit_sha, rel_path, verification_text, diff_text)
        cached = cache.get(cache_key)
        if cached is not None:
            # cache hit — 不重跑 LLM
            if not cached.get("invalidates") or float(cached.get("confidence") or 0) < CONFIDENCE_THRESHOLD:
                return None
            return {
                "verification": rel_path,
                "confidence": float(cached["confidence"]),
                "reason": str(cached.get("reason", ""))[:200],
                "affected_symbols": list(cached.get("affected_symbols") or [])[:5],
                "suggested_action": str(cached.get("suggested_action", "no_action")),
                "_cache_hit": True,
            }

        # cache miss — 真跑 LLM
        prompt = build_prompt(cand, verification_text, diff_text)
        parsed = call_claude_sonnet(prompt)
        if not parsed:
            return None
        try:
            invalidates = bool(parsed.get("invalidates"))
            confidence = float(parsed.get("confidence") or 0)
        except (TypeError, ValueError):
            return None

        # 寫 cache (即使 invalidates=false 也寫,下次同 key 命中省 LLM)
        cache[cache_key] = {
            "invalidates": invalidates,
            "confidence": confidence,
            "reason": parsed.get("reason", ""),
            "affected_symbols": parsed.get("affected_symbols") or [],
            "suggested_action": parsed.get("suggested_action", "no_action"),
            "ts": datetime.now().isoformat(timespec="seconds"),
        }
        cache_dirty = True

        if not invalidates or confidence < CONFIDENCE_THRESHOLD:
            return None
        return {
            "verification": rel_path,
            "confidence": confidence,
            "reason": str(parsed.get("reason", ""))[:200],
            "affected_symbols": list(parsed.get("affected_symbols") or [])[:5],
            "suggested_action": str(parsed.get("suggested_action", "no_action")),
        }

    findings = []
    with ThreadPoolExecutor(max_workers=min(len(candidates), MAX_CANDIDATES)) as ex:
        futures = [ex.submit(check_one, cand) for cand in candidates]
        for fut in as_completed(futures):
            result = fut.result()
            if result is not None:
                findings.append(result)

    # C: flush cache (有跑 LLM 才寫)
    if cache_dirty:
        _write_cache(cache_path, cache)

    if not findings:
        return 0

    # === stderr 警告 ===
    msg = [
        "⚠️  Verification rot check (Sonnet): 本次 commit 可能使以下驗證紀錄失效",
        "",
    ]
    for f in findings:
        msg.append(f"   • {f['verification']}  (confidence={f['confidence']:.2f})")
        msg.append(f"     原因: {f['reason']}")
        msg.append(f"     建議: {f['suggested_action']}")
        if f["affected_symbols"]:
            msg.append(f"     symbols: {', '.join(f['affected_symbols'])}")
        msg.append("")
    msg.append(f"已寫入 docs/.rot-queue.jsonl (commit {commit_sha}),"
               "之後跑 `scripts/rot-queue-digest.sh` 看總覽。")
    print("\n".join(msg), file=sys.stderr)

    # === 寫 .rot-queue.jsonl ===
    queue_path = graph_root.parent / ".rot-queue.jsonl"
    timestamp = datetime.now().isoformat(timespec="seconds")
    try:
        with queue_path.open("a", encoding="utf-8") as fh:
            for finding in findings:
                entry = {
                    "ts": timestamp,
                    "commit": commit_sha,
                    **finding,
                }
                fh.write(json.dumps(entry, ensure_ascii=False) + "\n")
    except OSError:
        pass

    return 0


if __name__ == "__main__":
    sys.exit(main())
