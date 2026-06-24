#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""test_lumos.py — lumos 行為鎖定測試(stdlib only,零依賴)

跑法: python3 scripts/test_lumos.py
退出碼: 0 全過 / 1 有失敗(CI 可用)

涵蓋 2026-06-13 Sonnet 對抗審計找到的 bug + 核心讀寫行為。每個 fixture 對應一條
[[2026-06-13_lumos審計與測試套件]] 列出的優先案例。
"""
import subprocess
import sys
import tempfile
import unicodedata
from pathlib import Path

GRAPHCTL = str(Path(__file__).resolve().parent / "lumos")
PASS, FAIL = 0, 0


def run(vault, *args, expect_rc=None):
    r = subprocess.run([sys.executable, GRAPHCTL, "--vault", str(vault), *args],
                       capture_output=True, text=True)
    if expect_rc is not None and r.returncode != expect_rc:
        raise AssertionError(f"rc={r.returncode} 預期 {expect_rc}\n{r.stdout}\n{r.stderr}")
    return r


def check(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  ✓ {name}")
    else:
        FAIL += 1
        print(f"  ✗ {name}  {detail}")


def mkvault():
    d = Path(tempfile.mkdtemp(prefix="gctl-test-"))
    for sub in ("Systems", "Verification", "Projects", "MOC"):
        (d / sub).mkdir(parents=True)
    (d / "MOC" / "idx.md").write_text("---\ntype: moc\n---\n# idx\n", encoding="utf-8")
    return d


def write(vault, rel, fm, body="# x\n"):
    p = vault / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(f"---\n{fm}\n---\n{body}", encoding="utf-8")
    return p


def read(p):
    return p.read_text(encoding="utf-8")


# ── BUG-1: append dedup 前綴衝突 — X 不該因 X_v2 存在被誤判 ──
def t_append_prefix_collision():
    v = mkvault()
    p = write(v, "Systems/S.md",
              "type: system\nstatus: done\nverified_by:\n  - \"[[Projects/A_v2]]\"")
    run(v, "append", "S", "verified_by", "[[A]]", expect_rc=0)
    txt = read(p)
    check("BUG-1 append 前綴衝突: [[A]] 真的被加入(非被 A_v2 誤判 no-op)",
          "[[A]]" in txt and "[[Projects/A_v2]]" in txt, txt)


# ── append 精確 dedup — 同 basename 再 append 應 no-op ──
def t_append_exact_dedup():
    v = mkvault()
    p = write(v, "Systems/S.md",
              "type: system\nstatus: done\nverified_by:\n  - \"[[V1]]\"")
    run(v, "append", "S", "verified_by", "[[V1]]", expect_rc=0)
    check("append 精確 dedup: 同 basename 不重複加",
          read(p).count("[[V1]]") == 1, read(p))


# ── BUG-2: Check 3 前綴 — System 連 V 但 verified_by 只有 V_v2,doctor 應報漏 ──
def t_check3_prefix_no_false_pass():
    # Check 3 看 Verification→System 方向:topic 連 S,但 S.verified_by 只有 topic_v2。
    # 精確比對下 topic != topic_v2 → 應報漏;舊子字串碼會誤判已同步。
    v = mkvault()
    write(v, "Systems/S.md",
          "type: system\nstatus: done\nverified_by:\n  - \"[[2026-01-01_topic_v2]]\"",
          body="# S\n")
    write(v, "Verification/2026-01-01_topic.md", "type: verification\nstatus: pass\ndate: 2026-01-01",
          body="# topic\n驗 [[S]]\n")
    write(v, "Verification/2026-01-01_topic_v2.md", "type: verification\nstatus: pass\ndate: 2026-01-02",
          body="# topic_v2\n驗 [[S]]\n")
    r = run(v, "doctor", "--ci")
    # 精確解析「漏:」行的 token,避免 topic 是 topic_v2 子字串的歧義
    missed_tokens = set()
    for line in r.stdout.splitlines():
        if "漏:" in line:
            missed_tokens |= {t.strip() for t in line.split("漏:", 1)[1].split("|")}
    check("BUG-2 Check3 前綴: topic 漏寫被報(非被 topic_v2 子字串誤判已同步)",
          r.returncode == 1 and "2026-01-01_topic" in missed_tokens,
          f"missed={missed_tokens}\n{r.stdout}")


# ── BUG-6: cmd_new 路徑逃脫 ──
def t_new_path_traversal():
    v = mkvault()
    r = run(v, "new", "system", "../../../tmp/injected")
    check("BUG-6 new 路徑逃脫被拒(exit 2)", r.returncode == 2, r.stderr)
    check("BUG-6 未在 vault 外建檔", not (v.parent.parent.parent / "tmp" / "injected.md").exists())


def t_new_teaches_tags():
    # new 在執行當下教標籤規則:stdout 含合約鏈提示,檔案骨架含完整符號行
    v = mkvault()
    r = run(v, "new", "system", "AcctSvc", expect_rc=0)
    check("new system: stdout 教 ★INVARIANT★ + [test:] 合約鏈",
          "★INVARIANT★" in r.stdout and "[test:" in r.stdout, r.stdout)
    check("new system: stdout 提示寫完跑 doctor", "lumos doctor" in r.stdout, r.stdout)
    txt = read(v / "Systems" / "AcctSvc.md")
    check("new system: summary 骨架含 FLOW/KEY/DEP/TEST 符號行",
          all(s in txt for s in ("FLOW:", "KEY:", "DEP:", "TEST:")), txt)
    r2 = run(v, "new", "issue", "BadState", expect_rc=0)
    t2 = read(v / "Issues" / "BadState.md")
    check("new issue: 骨架含 FLAG/DECISION/KEY", all(s in t2 for s in ("FLAG:", "DECISION:", "KEY:")), t2)
    # 骨架本身要過 doctor(空符號行不該觸發 lint)
    rd = run(v, "doctor", "--ci")
    check("new 骨架 doctor 不報問題", rd.returncode == 0, rd.stdout)


# ── BUG-7: fmt_scalar YAML 型別劫持 ──
def t_set_boolean_guard():
    v = mkvault()
    p = write(v, "Systems/S.md", "type: system\nstatus: doing")
    run(v, "set", "S", "status", "true", expect_rc=0)
    check("BUG-7 set status true → 引號保護(status: \"true\")",
          'status: "true"' in read(p), read(p))


# ── set 日期 bare 不加引號(污染指紋防護的反向:正常日期不該被引號) ──
def t_set_date_bare():
    v = mkvault()
    p = write(v, "Systems/S.md", "type: system\nstatus: done\nupdated: 2026-01-01")
    run(v, "set", "S", "updated", "2026-06-13", expect_rc=0)
    check("set 日期 bare(updated: 2026-06-13 無引號)",
          "updated: 2026-06-13" in read(p) and '"2026-06-13"' not in read(p), read(p))


# ── set 最小 diff:只動目標行 ──
def t_set_minimal_diff():
    v = mkvault()
    fm = "type: system\nstatus: doing\ncreated: 2026-01-01\nsummary: |-\n  FLOW:A→B\n  KEY:keep"
    p = write(v, "Systems/S.md", fm)
    run(v, "set", "S", "status", "done", expect_rc=0)
    txt = read(p)
    check("set 最小 diff: summary block 逐字保留",
          "FLOW:A→B" in txt and "KEY:keep" in txt and "status: done" in txt, txt)


# ── append 全新 list(key 不存在) ──
def t_append_new_list():
    v = mkvault()
    p = write(v, "Systems/S.md", "type: system\nstatus: done")
    run(v, "append", "S", "plan_refs", "[[某計劃]]", expect_rc=0)
    txt = read(p)
    check("append 全新 list 建立(plan_refs:\\n  - \"[[某計劃]]\")",
          "plan_refs:" in txt and '- "[[某計劃]]"' in txt, txt)


# ── BUG-5: list 後接 sub-mapping(decisions)時,append verified_by 不插進 decisions ──
def t_append_with_nested_decisions():
    v = mkvault()
    fm = ("type: system\nstatus: done\n"
          "verified_by:\n  - \"[[V1]]\"\n"
          "decisions:\n  - content: 決策一\n    decided: 2026-01-01\n    valid: true")
    p = write(v, "Systems/S.md", fm)
    run(v, "append", "S", "verified_by", "[[V2]]", expect_rc=0)
    txt = read(p)
    # V2 應緊接 V1 後、在 decisions 之前;decisions 結構完整
    vi, di = txt.index("[[V2]]"), txt.index("decisions:")
    check("BUG-5 append 不插進 decisions 區塊(V2 在 decisions 前)", vi < di, txt)
    check("BUG-5 decisions 結構保留", "content: 決策一" in txt and "valid: true" in txt, txt)


# ── archive 前綴安全 + 移檔:archive X 不該動 X_v2 ──
def t_archive_prefix_and_move():
    # X 老(歸檔)、X_v2 近期(不歸檔):archive X 只動 X 的連結,X_v2 路徑連結+檔案不動
    v = mkvault()
    write(v, "Verification/2020-01-01_X.md", "type: verification\nstatus: pass\ncreated: 2020-01-01")
    write(v, "Verification/2090-01-01_X_v2.md", "type: verification\nstatus: pass\ncreated: 2090-01-01")
    s = write(v, "Systems/S.md", "type: system\nstatus: done",
              body="連 [[Verification/2020-01-01_X]] 與 [[Verification/2090-01-01_X_v2]]\n")
    run(v, "archive", "--days", "30", "--apply", expect_rc=0)
    txt = read(s)
    check("archive X 移到 Archive/2020-01/",
          (v / "Verification/Archive/2020-01/2020-01-01_X.md").exists())
    check("archive 前綴安全: X 連結正規化成 basename",
          "[[2020-01-01_X]]" in txt and "[[Verification/2020-01-01_X]]" not in txt)
    check("archive 前綴安全: 未歸檔的 X_v2 路徑連結+檔案不動",
          "[[Verification/2090-01-01_X_v2]]" in txt
          and (v / "Verification/2090-01-01_X_v2.md").exists(), txt)


# ── doctor 乾淨基線 ──
def t_doctor_clean():
    v = mkvault()
    write(v, "Systems/S.md", "type: system\nstatus: done\nverified_by:\n  - \"[[V1]]\"",
          body="# S\n")
    write(v, "Verification/V1.md", "type: verification\nstatus: pass\ndate: 2026-01-01",
          body="# V1\n驗 [[S]]\n")
    r = run(v, "doctor", "--ci")
    check("doctor 乾淨 vault → exit 0", r.returncode == 0, r.stdout)


# ══ 第二輪審計回歸 ══

# ── NEW-A: 跨資料夾同 basename append 不該誤 dedup / 不該 rc=2 ──
def t_append_cross_folder_same_basename():
    v = mkvault()
    write(v, "Systems/X.md", "type: system\nstatus: done")  # 另一篇,同 basename X
    p = write(v, "Systems/S.md",
              "type: system\nstatus: done\nverified_by:\n  - \"[[Verification/X]]\"")
    r = run(v, "append", "S", "verified_by", "[[Systems/X]]")
    check("NEW-A 跨資料夾同名 append 成功(非 rc=2 自驗失敗)", r.returncode == 0, r.stderr)
    check("NEW-A [[Systems/X]] 真的被加(與 [[Verification/X]] 並存)",
          "[[Systems/X]]" in read(p) and "[[Verification/X]]" in read(p), read(p))


# ── append path 式 vs basename 式同篇 → 視為重複(dedup) ──
def t_append_path_vs_basename_dedup():
    v = mkvault()
    p = write(v, "Systems/S.md",
              "type: system\nstatus: done\nverified_by:\n  - \"[[Verification/X]]\"")
    # 注意: link_target 保留路徑,故 [[X]] 與 [[Verification/X]] target 不同字串 →
    # 會新增(可接受的冗餘,非錯誤)。本案僅鎖定「完全相同 target 不重複」。
    run(v, "append", "S", "verified_by", "[[Verification/X]]", expect_rc=0)
    check("完全相同 target 不重複加", read(p).count("[[Verification/X]]") == 1, read(p))


# ── NEW-B: Check 3 跨資料夾同 basename 不該假通過 ──
def t_check3_cross_folder_no_false_pass():
    v = mkvault()
    write(v, "Systems/S.md",
          "type: system\nstatus: done\nverified_by:\n  - \"[[Projects/MyV]]\"")
    write(v, "Projects/MyV.md", "type: project\nstatus: done")  # 不同篇,同 basename
    write(v, "Verification/MyV.md", "type: verification\nstatus: pass\ndate: 2026-01-01",
          body="# MyV\n驗 [[S]]\n")
    r = run(v, "doctor", "--ci")
    missed = set()
    for line in r.stdout.splitlines():
        if "漏:" in line:
            missed |= {t.strip() for t in line.split("漏:", 1)[1].split("|")}
    check("NEW-B Check3 跨資料夾: Verification/MyV 漏寫被報(非被 Projects/MyV 同 basename 誤判)",
          r.returncode == 1 and "MyV" in missed, f"missed={missed}\n{r.stdout}")


# ── Check 3 path 式 vs basename 式同篇 → 視為已同步(不誤報漏) ──
def t_check3_path_basename_equiv():
    v = mkvault()
    # verified_by 用 path 式,Verification 也在 Verification/ → 同篇,不該報漏
    write(v, "Systems/S.md",
          "type: system\nstatus: done\nverified_by:\n  - \"[[Verification/MyV]]\"")
    write(v, "Verification/MyV.md", "type: verification\nstatus: pass\ndate: 2026-01-01",
          body="# MyV\n驗 [[S]]\n")
    r = run(v, "doctor", "--ci")
    check("Check3 path 式 verified_by 視為已同步(不誤報漏)", r.returncode == 0, r.stdout)


# ── archive CRLF 檔跳過 rewrite(不靜默正規化)──
def t_archive_crlf_skip():
    v = mkvault()
    write(v, "Verification/2020-01-01_Z.md", "type: verification\nstatus: pass\ncreated: 2020-01-01")
    # CRLF 檔,body 含 path 式連結
    p = v / "Systems/S.md"
    p.write_bytes("---\r\ntype: system\r\nstatus: done\r\n---\r\n連 [[Verification/2020-01-01_Z]]\r\n"
                  .encode("utf-8"))
    run(v, "archive", "--days", "30", "--apply", expect_rc=0)
    # Z 仍移檔,但 CRLF 檔的連結未被 rewrite(仍 path 式 + 仍 CRLF)
    txt = p.read_bytes()
    check("archive CRLF 檔跳過 rewrite(連結保留 path 式)",
          b"[[Verification/2020-01-01_Z]]" in txt and b"\r\n" in txt)


# ── archive 活守衛護欄:綁定測試仍存在的 Verification 不按年齡歸檔 ──
def t_archive_live_guard_protected():
    # 需要 docs/ 父層(repo_root 偵測)+ 一個含 [Fact] 方法的 .cs(discover_test_methods)
    root = Path(tempfile.mkdtemp(prefix="gctl-repo-"))
    (root / "Tests").mkdir()
    (root / "Tests" / "GuardTests.cs").write_text(
        "public class GuardTests {\n  [Fact]\n  public void MyLiveGuard() { }\n}\n", encoding="utf-8")
    vault = root / "docs" / "kg"
    for sub in ("Systems", "Verification", "MOC"):
        (vault / sub).mkdir(parents=True)
    (vault / "MOC" / "idx.md").write_text("---\ntype: moc\n---\n# idx\n", encoding="utf-8")
    # ★INVARIANT★ 綁定存活測試
    (vault / "Systems" / "S.md").write_text(
        "---\ntype: system\nstatus: done\nsummary: |-\n"
        "  KEY:★INVARIANT★ 某載重宣稱 [test:MyLiveGuard]\n---\n# S\n", encoding="utf-8")
    # 老 Verification:提到存活測試 → 應保留
    (vault / "Verification" / "2020-01-01_guarded.md").write_text(
        "---\ntype: verification\nstatus: pass\ncreated: 2020-01-01\n"
        "valid_under:\n  - MyLiveGuard\n---\n# guarded\n", encoding="utf-8")
    # 老 Verification:沒提到任何存活測試 → 應照舊歸檔
    (vault / "Verification" / "2020-01-01_plain.md").write_text(
        "---\ntype: verification\nstatus: pass\ncreated: 2020-01-01\n---\n# plain\n", encoding="utf-8")
    r = run(vault, "archive", "--days", "30", "--apply", expect_rc=0)
    check("活守衛護欄: 綁定測試仍存在的 Verification 保留不歸檔",
          (vault / "Verification/2020-01-01_guarded.md").exists()
          and not (vault / "Verification/Archive/2020-01/2020-01-01_guarded.md").exists(), r.stdout)
    check("活守衛護欄: 未提及存活測試的 Verification 照舊歸檔",
          (vault / "Verification/Archive/2020-01/2020-01-01_plain.md").exists(), r.stdout)


# ── archive 守衛被刪(測試不在 code)→ 該 Verification 恢復可歸檔 ──
def t_archive_dead_guard_archivable():
    root = Path(tempfile.mkdtemp(prefix="gctl-repo-"))
    (root / "Tests").mkdir()  # 無任何 .cs 測試方法 → 綁定名不存在於 code
    vault = root / "docs" / "kg"
    for sub in ("Systems", "Verification", "MOC"):
        (vault / sub).mkdir(parents=True)
    (vault / "MOC" / "idx.md").write_text("---\ntype: moc\n---\n# idx\n", encoding="utf-8")
    (vault / "Systems" / "S.md").write_text(
        "---\ntype: system\nstatus: done\nsummary: |-\n"
        "  KEY:★INVARIANT★ 某載重宣稱 [test:GoneGuard]\n---\n# S\n", encoding="utf-8")
    (vault / "Verification" / "2020-01-01_g.md").write_text(
        "---\ntype: verification\nstatus: pass\ncreated: 2020-01-01\n"
        "valid_under:\n  - GoneGuard\n---\n# g\n", encoding="utf-8")
    run(vault, "archive", "--days", "30", "--apply", expect_rc=0)
    check("守衛已死(測試不在 code): Verification 恢復按年齡可歸檔",
          (vault / "Verification/Archive/2020-01/2020-01-01_g.md").exists())


# ── negative: append 到 block key 應拒 ──
def t_append_block_key_rejected():
    v = mkvault()
    write(v, "Systems/S.md", "type: system\nstatus: done\nsummary: |-\n  FLOW:A")
    # summary 不在 append 白名單 → 應 rc=2
    r = run(v, "append", "S", "summary", "x")
    check("negative: append 非白名單 key(summary)被拒", r.returncode == 2, r.stderr)


# ── negative: set 非法日期應拒 ──
def t_set_bad_date_rejected():
    v = mkvault()
    write(v, "Systems/S.md", "type: system\nstatus: done\nupdated: 2026-01-01")
    r = run(v, "set", "S", "updated", "not-a-date")
    check("negative: set updated 非法日期被拒", r.returncode == 2, r.stderr)


# ══ 第三輪審計回歸 ══

# ── export 逸出節點名中的 " (R3 latent bug) ──
def t_export_quote_escape():
    v = mkvault()
    write(v, 'Systems/A"B.md', "type: system\nstatus: done")
    rm = run(v, "export", "--format", "mermaid", "--folders", "Systems", expect_rc=0)
    check('export mermaid: " 逸出成 &quot;(不破語法)',
          '&quot;' in rm.stdout and 'A"B"]' not in rm.stdout, rm.stdout)
    rd = run(v, "export", "--format", "dot", "--folders", "Systems", expect_rc=0)
    check('export dot: " 逸出成 \\"(不破語法)',
          '\\"' in rd.stdout, rd.stdout)


# ── search 全文搜尋 ──
def t_search():
    v = mkvault()
    write(v, "Systems/A.md", "type: system\nstatus: done", body="# A\nServiceType 代碼說明\n")
    write(v, "Verification/B.md", "type: verification\nstatus: pass\ndate: 2026-01-01",
          body="# B\n無關內容\n")
    r = run(v, "search", "ServiceType", "--files-only", expect_rc=0)
    check("search 命中 A 不命中 B", "Systems/A.md" in r.stdout and "Verification/B.md" not in r.stdout, r.stdout)
    r2 = run(v, "search", "servicetype", "--files-only", expect_rc=0)
    check("search 大小寫不敏感", "Systems/A.md" in r2.stdout, r2.stdout)
    r3 = run(v, "search", "ServiceType", "--path", "Verification", "--files-only", expect_rc=0)
    check("search --path 限定資料夾(Systems 命中被排除)", "Systems/A.md" not in r3.stdout, r3.stdout)
    r4 = run(v, "search", "Service.*代碼", "--regex", "--files-only", expect_rc=0)
    check("search --regex", "Systems/A.md" in r4.stdout, r4.stdout)


# ── search 尊重標籤哲學: 排除 code block + 標記區域(option A) ──
def t_search_structure_aware():
    v = mkvault()
    write(v, "Systems/C.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ widget 不可改",
          body="# C\n正文提到 widget\n```\nwidget in code block\n```\n")
    # 預設排除 code block: 只命中 frontmatter ★INVARIANT★ + body 正文,不含 code 那行
    r = run(v, "search", "widget", expect_rc=0)
    check("search 排除 code block(預設)", "widget in code block" not in r.stdout, r.stdout)
    check("search 區域標記 ★INVARIANT★", "[★INVARIANT★]" in r.stdout, r.stdout)
    check("search 區域標記 body", "[body]" in r.stdout, r.stdout)
    # --code 才含 code block 那行
    rc = run(v, "search", "widget", "--code", expect_rc=0)
    check("search --code 含 code block 內容", "widget in code block" in rc.stdout, rc.stdout)


# ══ T3 巢狀決策手術 ══

def _vault_with_decisions():
    v = mkvault()
    write(v, "Systems/X.md",
          "type: system\nstatus: done\n"
          "decisions:\n"
          "  - content: 舊方案用樂觀鎖\n"
          "    alternatives_considered:\n"
          '      - "Redis:要基礎設施"\n'
          "    why_chosen: 不增依賴\n"
          "    decided: 2026-04-01\n"
          "    valid: true\n"
          "  - content: 第二條不動\n"
          "    decided: 2026-04-02\n"
          "    valid: true")
    return v, v / "Systems/X.md"


def t_decision_supersede():
    v, p = _vault_with_decisions()
    r = run(v, "decision-supersede", "X", "樂觀鎖", "--by", "改用 Redis", "--ended", "2026-06-13")
    check("decision-supersede rc=0", r.returncode == 0, r.stderr)
    txt = read(p)
    check("supersede: 第一條 valid:false + superseded_by",
          "valid: false" in txt and "superseded_by: 改用 Redis" in txt, txt)
    check("supersede: 巢狀 alternatives_considered 子清單未被動",
          '"Redis:要基礎設施"' in txt, txt)
    check("supersede: 第二條 valid:true 未被動",
          "第二條不動\n    decided: 2026-04-02\n    valid: true" in txt, txt)


def t_decision_supersede_notfound():
    v, p = _vault_with_decisions()
    before = read(p)
    r = run(v, "decision-supersede", "X", "不存在的決策", "--by", "Y")
    check("decision-supersede 找不到 → rc=2", r.returncode == 2, r.stderr)
    check("decision-supersede 找不到 → 原檔不動", read(p) == before)


def t_decision_add():
    v, p = _vault_with_decisions()
    r = run(v, "decision-add", "X", "新決策含冒號: 測試", "--decided", "2026-06-13", "--why", "超越")
    check("decision-add rc=0", r.returncode == 0, r.stderr)
    txt = read(p)
    check("decision-add: content 含冒號自動引號",
          '"新決策含冒號: 測試"' in txt, txt)
    check("decision-add: valid:true + why_chosen", "why_chosen: 超越" in txt and txt.count("valid: true") >= 2, txt)


def t_decision_add_no_existing():
    v = mkvault()
    p = write(v, "Systems/Y.md", "type: system\nstatus: done")
    run(v, "decision-add", "Y", "首條決策", "--decided", "2026-06-13", expect_rc=0)
    txt = read(p)
    check("decision-add 無 decisions 時建立", "decisions:" in txt and "首條決策" in txt, txt)


# ══ T3 第三輪審計回歸:複雜巢狀案例 ══

def _complex_decisions_vault():
    """複雜 fixture: block scalar content + 巢狀子清單 + 多條 + decisions 後接 verified_by。"""
    v = mkvault()
    write(v, "Systems/Z.md",
          "type: system\nstatus: done\n"
          "decisions:\n"
          "  - content: |-\n"
          "      多行決策第一行\n"
          "      第二行補充說明含冒號: 細節\n"
          "    context: 當時痛點\n"
          "    alternatives_considered:\n"
          '      - "Redis:要基礎設施"\n'
          '      - "悲觀鎖:卡連線池"\n'
          "    why_chosen: 不增依賴\n"
          "    decided: 2026-04-01\n"
          "    valid: true\n"
          "  - content: 第二條短決策\n"
          "    decided: 2026-04-02\n"
          "    valid: true\n"
          "verified_by:\n"
          '  - "[[V1]]"',
          body="# Z\n")
    write(v, "Verification/V1.md", "type: verification\nstatus: pass\ndate: 2026-01-01",
          body="# V1\n驗 [[Z]]\n")  # 讓 verified_by 解析得到,fixture 本身乾淨
    return v, v / "Systems/Z.md"


def t_complex_supersede_block_scalar():
    v, p = _complex_decisions_vault()
    before = read(p)
    r = run(v, "decision-supersede", "Z", "多行決策第一行", "--by", "新方案", "--ended", "2026-06-13")
    check("複雜:block scalar content supersede rc=0", r.returncode == 0, r.stderr)
    txt = read(p)
    check("複雜:block scalar 多行 content 逐字未動",
          "多行決策第一行" in txt and "第二行補充說明含冒號: 細節" in txt, txt)
    check("複雜:巢狀子清單未動", '"Redis:要基礎設施"' in txt and '"悲觀鎖:卡連線池"' in txt, txt)
    check("複雜:why_chosen/context 未動", "why_chosen: 不增依賴" in txt and "context: 當時痛點" in txt, txt)
    check("複雜:第二條決策 valid:true 未動",
          "第二條短決策\n    decided: 2026-04-02\n    valid: true" in txt, txt)
    check("複雜:decisions 後的 verified_by 未動", '- "[[V1]]"' in txt, txt)
    check("複雜:只插了 superseded_by + ended(無重複 valid)", txt.count("valid: false") == 1, txt)


def t_complex_supersede_repeat_rejected():
    v, p = _complex_decisions_vault()
    run(v, "decision-supersede", "Z", "多行決策第一行", "--by", "第一次", expect_rc=0)
    before = read(p)
    r = run(v, "decision-supersede", "Z", "多行決策第一行", "--by", "第二次")
    check("複雜:重複 supersede → rc=2", r.returncode == 2, r.stderr)
    check("複雜:重複 supersede 原檔不動(無重複 superseded_by)",
          read(p) == before and read(p).count("superseded_by") == 1, read(p))


def t_complex_add_then_parse():
    v, p = _complex_decisions_vault()
    run(v, "decision-add", "Z", "第三條新決策", "--decided", "2026-06-13", "--why", "超越", expect_rc=0)
    # decisions 指令應讀回全部 3 條(結構沒被 add 破壞)
    r = run(v, "decisions", "Z", expect_rc=0)
    check("複雜:add 後 decisions 讀回 3 條",
          "多行決策第一行" in r.stdout and "第二條短決策" in r.stdout and "第三條新決策" in r.stdout, r.stdout)
    # add 不該插進 verified_by 之後或子清單
    txt = read(p)
    zi, vi = txt.index("第三條新決策"), txt.index("verified_by:")
    check("複雜:新決策插在 decisions 內(verified_by 之前)", zi < vi, txt)


def t_complex_consecutive_ops():
    v, p = _complex_decisions_vault()
    run(v, "decision-supersede", "Z", "第二條短決策", "--by", "翻盤2", expect_rc=0)
    run(v, "decision-add", "Z", "連續新增", "--decided", "2026-06-13", expect_rc=0)
    r = run(v, "doctor", "--vault", str(v), "--ci") if False else run(v, "doctor", "--ci")
    check("複雜:連續 supersede+add 後 doctor 仍乾淨", r.returncode == 0, r.stdout)
    r2 = run(v, "decisions", "Z", expect_rc=0)
    check("複雜:連續操作後 3 條讀回(第二條已翻盤)",
          "翻盤2" in r2.stdout and "連續新增" in r2.stdout, r2.stdout)


def t_complex_add_bad_date():
    v, p = _complex_decisions_vault()
    before = read(p)
    r = run(v, "decision-add", "Z", "壞日期決策", "--decided", "not-a-date")
    check("複雜:decision-add 非日期 → rc=2 原檔不動", r.returncode == 2 and read(p) == before, r.stderr)


def t_export_html():
    import tempfile
    v = mkvault()
    write(v, "Systems/A.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ x\nverified_by:\n  - \"[[V1]]\"",
          body="# A\n連 [[V1]]\n含危險 </script> 字串\n")
    write(v, "Verification/V1.md", "type: verification\nstatus: stale\ndate: 2026-01-01", body="# V1\n驗 [[A]]\n")
    out = str(Path(tempfile.mkdtemp()) / "g.html")
    r = run(v, "export", "--format", "html", "--output", out, expect_rc=0)
    html = Path(out).read_text(encoding="utf-8")
    check("export html: 產出檔含 DATA + 3D 引擎(ForceGraph3D)", "const DATA" in html and "ForceGraph3D" in html, r.stdout)
    check("export html: 筆記內 </script> 被轉義成 <\\/script>", "<\\/script>" in html, "escape")
    check("export html: 結尾完整、單一 </html>(未被內文提早關閉)",
          html.rstrip().endswith("</html>") and html.count("</html>") == 1, "structure")


def t_invariant_test_binding():
    # Check T 牙齒:裸 ★INVARIANT★(無 [test:])→ doctor 擋(載重宣稱沒綁可執行證據)
    v = mkvault()
    write(v, "Systems/Naked.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 自動型只派 V",
          body="# Naked\n")
    r = run(v, "doctor", "--ci")
    check("Check T: 裸 ★INVARIANT★(無 test 綁定)被 doctor 擋",
          r.returncode == 1 and "裸 ★INVARIANT★" in r.stdout, r.stdout)
    # 綁了 [test:X] → 不再算裸合約(沙盒無 repo root,存在性檢查跳過)
    v2 = mkvault()
    write(v2, "Systems/Bound.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 自動型只派 V [test:SomeGuardTest]",
          body="# Bound\n")
    r2 = run(v2, "doctor", "--ci")
    check("Check T: 綁了 [test:] 的 ★INVARIANT★ 不算裸合約",
          "裸 ★INVARIANT★" not in r2.stdout, r2.stdout)


def t_invariant_audit_binding():
    # Check T 牙齒:綁了 [test:] 但無 [audit:] → doctor 報「未經獨立審計」(maker/checker 破口)
    v = mkvault()
    write(v, "Systems/Bound.md",
          "type: system\nstatus: done\nsummary: |-\n"
          "  KEY:★INVARIANT★ 點數不足必須擋 [test:SomeGuard]",
          body="# Bound\n")
    r = run(v, "doctor", "--ci")
    check("Check T: 綁測試但未經獨立審計 → doctor 擋(rc1)",
          r.returncode == 1 and "未經獨立審計" in r.stdout, r.stdout)
    # 加上 [audit:模型/日期] → 不再報未審
    v2 = mkvault()
    write(v2, "Systems/Aud.md",
          "type: system\nstatus: done\nsummary: |-\n"
          "  KEY:★INVARIANT★ 點數不足必須擋 [test:SomeGuard] [audit:sonnet/2026-06-18]",
          body="# Aud\n")
    r2 = run(v2, "doctor", "--ci")
    check("Check T: 有 [audit:] 留痕 → 不再報未審", "未經獨立審計" not in r2.stdout, r2.stdout)
    # 裸合約(連 [test:] 都沒)不應被未審項誤報(naked 先擋,audit 不雙重計)
    v3 = mkvault()
    write(v3, "Systems/Naked.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 沒綁測試的",
          body="# Naked\n")
    r3 = run(v3, "doctor", "--ci")
    check("Check T: 裸合約只報裸、不報未審(不雙重計)",
          "未經獨立審計" not in r3.stdout and "裸 ★INVARIANT★" in r3.stdout, r3.stdout)


def t_guard_audit():
    # guard audit:把 [audit:模型/日期] 留痕寫回 KEY 行,保留 [test:],重審覆蓋舊留痕
    v = mkvault()
    p = write(v, "Systems/S.md",
              "type: system\nstatus: done\nsummary: |-\n"
              "  KEY:★INVARIANT★ 點數不足必須擋 [test:SomeGuard]",
              body="# S\n")
    r = run(v, "guard", "audit", "Systems/S", "點數不足", "--date", "2026-06-18")
    txt = read(p)
    check("guard audit: [audit:] 寫回 KEY 行", "[audit:sonnet/2026-06-18]" in txt, r.stdout + r.stderr)
    check("guard audit: [test:] 綁定不受影響", "[test:SomeGuard]" in txt, txt)
    # 重審(換模型/日期)→ 覆蓋,不重複留痕
    run(v, "guard", "audit", "Systems/S", "點數不足", "--date", "2026-07-01", "--model", "opus")
    txt2 = read(p)
    check("guard audit: 重審覆蓋舊留痕(新日期生效)",
          "[audit:opus/2026-07-01]" in txt2 and "2026-06-18" not in txt2, txt2)
    check("guard audit: 不累積(只一個 audit 標記)", txt2.count("[audit:") == 1, txt2)
    # 找不到子字串 → rc2
    r3 = run(v, "guard", "audit", "Systems/S", "不存在的合約")
    check("guard audit: 子字串找不到 KEY 行 → rc2", r3.returncode == 2, r3.stdout + r3.stderr)


def t_lint():
    # 單檔快檢:乾淨節點過、各種寫入當下的錯被抓
    v = mkvault()
    # 乾淨 system(無合約)→ 0 問題
    write(v, "Systems/Clean.md",
          "type: system\nstatus: doing\nsummary: |-\n  FLOW:a→b\n  KEY:某關鍵", body="# Clean\n")
    r = run(v, "lint", "Systems/Clean")
    check("lint: 乾淨節點 rc0", r.returncode == 0 and "0 問題" in r.stdout, r.stdout)
    # 裸 ★INVARIANT★ → error rc1
    write(v, "Systems/Naked.md",
          "type: system\nstatus: doing\nsummary: |-\n  KEY:★INVARIANT★ 沒綁測試的", body="# N\n")
    r = run(v, "lint", "Systems/Naked")
    check("lint: 裸合約 → rc1 error", r.returncode == 1 and "裸 ★INVARIANT★" in r.stdout, r.stdout)
    # ★INVARIANT★ 沒當 KEY 前綴(放 FLOW 行)→ 格式 error
    write(v, "Systems/BadMark.md",
          "type: system\nstatus: doing\nsummary: |-\n  FLOW:★INVARIANT★ 放錯行", body="# B\n")
    r = run(v, "lint", "Systems/BadMark")
    check("lint: ★ 非 KEY 前綴 → rc1(格式錯,contracts 抓不到)",
          r.returncode == 1 and "必須是 KEY 行前綴" in r.stdout, r.stdout)
    # 綁測試但未審 → error
    write(v, "Systems/Unaud.md",
          "type: system\nstatus: doing\nsummary: |-\n  KEY:★INVARIANT★ 擋下 [test:G]", body="# U\n")
    r = run(v, "lint", "Systems/Unaud")
    check("lint: 綁測試未審 → rc1", r.returncode == 1 and "未獨立審計" in r.stdout, r.stdout)
    # 綁測試 + 已審 → 0 問題
    write(v, "Systems/Good.md",
          "type: system\nstatus: doing\nsummary: |-\n  KEY:★INVARIANT★ 擋下 [test:G] [audit:sonnet/2026-06-18]",
          body="# G\n")
    r = run(v, "lint", "Systems/Good")
    check("lint: 綁測試+已審 → rc0", r.returncode == 0, r.stdout)
    # system 缺 summary → error
    write(v, "Systems/NoSum.md", "type: system\nstatus: doing", body="# NS\n")
    r = run(v, "lint", "Systems/NoSum")
    check("lint: system 缺 summary → rc1", r.returncode == 1 and "summary" in r.stdout, r.stdout)
    # ghost trap(單字串多 wikilink)→ error(複用 frontmatter 指紋)
    write(v, "Systems/Ghost.md",
          "type: system\nstatus: doing\nrelated: \"[[A]], [[B]]\"\nsummary: |-\n  KEY:x", body="# Gh\n")
    r = run(v, "lint", "Systems/Ghost")
    check("lint: 單字串多 wikilink ghost trap → rc1", r.returncode == 1 and "ghost" in r.stdout.lower(), r.stdout)
    # symbol typo → warning(不阻擋 rc0)
    write(v, "Systems/Typo.md",
          "type: system\nstatus: doing\nsummary: |-\n  KYE:打錯的符號\n  KEY:正常", body="# T\n")
    r = run(v, "lint", "Systems/Typo")
    check("lint: 符號 typo → warning 不阻擋(rc0)",
          r.returncode == 0 and "非標準符號行" in r.stdout, r.stdout)
    # 找不到節點 → rc2
    r = run(v, "lint", "Systems/NoSuchNode")
    check("lint: 找不到節點 → rc2", r.returncode == 2, r.stdout + r.stderr)


def t_guard():
    """guard list/scaffold/bind — 對談驅動守衛 scaffold(2026-06-15)。
    需 repo_root + 真 .cs(discover_test_methods),故自建 docs/ 結構而非 mkvault。"""
    import shutil
    root = Path(tempfile.mkdtemp(prefix="gctl-guard-"))
    vault = root / "docs" / "demo-knowledge"
    for sub in ("Systems", "Verification", "Projects", "MOC"):
        (vault / sub).mkdir(parents=True)
    (vault / "MOC" / "idx.md").write_text("---\ntype: moc\n---\n# idx\n", encoding="utf-8")
    (vault / "Systems" / "Demo.md").write_text(
        "---\ntype: system\nstatus: done\nsummary: |-\n"
        "  KEY:★INVARIANT★ 已綁的合約 [test:RealGuardX]\n"
        "  KEY:★INVARIANT★ 還沒綁的合約\n"
        "---\n# Demo\n", encoding="utf-8")
    td = root / "Demo.IntegrationTests"
    td.mkdir()
    (td / "RealGuard.cs").write_text(
        "using Xunit;\npublic class RealGuard {\n  [Fact]\n  public void RealGuardX() { }\n}\n",
        encoding="utf-8")
    tpl = root / ".lumos" / "guard-templates"
    tpl.mkdir(parents=True)
    (tpl / "behavioral.tmpl").write_text(
        "// {{NODE}} | {{INVARIANT}} | {{CLAIM}} | {{PREFIX}}\n"
        "public class {{CLASS}} {\n  public void {{METHOD}}() "
        "{ Assert.Fail(\"unfilled\"); }\n}\n", encoding="utf-8")
    try:
        r = run(vault, "guard", "list")
        check("guard list: real/naked 分類", "真綁 1" in r.stdout and "裸 1" in r.stdout, r.stdout)
        r = run(vault, "guard", "list", "--unbound")
        check("guard list --unbound: 列裸不列 real",
              "還沒綁的合約" in r.stdout and "已綁的合約" not in r.stdout, r.stdout)
        outd = root / "out"
        outd.mkdir()
        r = run(vault, "guard", "scaffold", "--node", "Systems/Demo", "--invariant", "還沒綁",
                "--method", "NewGuardX", "--type", "behavioral", "--claim", "具體可驗斷言",
                "--out", str(outd))
        f = outd / "NewGuardXTests.cs"
        txt = f.read_text(encoding="utf-8") if f.exists() else ""
        check("guard scaffold: 產出檔", f.exists(), r.stdout + r.stderr)
        check("guard scaffold: 預設紅燈 Assert.Fail", "Assert.Fail" in txt, txt)
        check("guard scaffold: placeholder 全替換", "{{" not in txt, txt)
        r = run(vault, "guard", "scaffold", "--node", "Systems/Demo", "--invariant", "還沒綁",
                "--method", "1bad", "--type", "behavioral", "--claim", "x", "--out", str(outd))
        check("guard scaffold: 非法 method 擋(rc2)", r.returncode == 2, r.stdout + r.stderr)
        r = run(vault, "guard", "scaffold", "--node", "Systems/Demo",
                "--invariant", "RealGuardX", "--method", "Zz", "--type", "behavioral",
                "--claim", "x", "--out", str(outd))
        check("guard scaffold: --invariant 不誤命中 [test:] 名(rc2)",
              r.returncode == 2, r.stdout + r.stderr)
        r = run(vault, "guard", "bind", "Systems/Demo", "還沒綁", "NewGuardX")
        nt = (vault / "Systems" / "Demo.md").read_text(encoding="utf-8")
        check("guard bind: [test:] 寫回 KEY 行", "[test:NewGuardX]" in nt, r.stdout + r.stderr)
        check("guard bind: 已綁行不受影響", nt.count("[test:RealGuardX]") == 1, nt)
    finally:
        shutil.rmtree(root, ignore_errors=True)


def t_guard_trace():
    """guard trace — 合約→守衛測試→Verification 證據鏈(reverse lookup)。"""
    v = mkvault()
    write(v, "Systems/Mod.md",
          "type: system\nstatus: done\nsummary: |-\n"
          "  KEY:★INVARIANT★ 某合約 [test:MyGuardTest]",
          body="# Mod\n")
    write(v, "Verification/2026-01-02_g.md", "type: verification\nstatus: pass",
          body="# g\n本守衛 MyGuardTest 跑 lab PASS\n")
    r = run(v, "guard", "trace", "Systems/Mod")
    check("guard trace: 合約→測試→Verification 鏈",
          "MyGuardTest" in r.stdout and "2026-01-02_g" in r.stdout, r.stdout)
    write(v, "Systems/Lonely.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 沒人測 [test:NobodyTestsThis]",
          body="# Lonely\n")
    r = run(v, "guard", "trace", "Systems/Lonely")
    check("guard trace: 無 Verification 提到 → 明示",
          "無 Verification 提到" in r.stdout, r.stdout)
    # Finding 4: 只有裸合約的節點不可印「無合約」矛盾 footer
    write(v, "Systems/NakedOnly.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 裸合約A\n  KEY:★INVARIANT★ 裸合約B",
          body="# NakedOnly\n")
    r = run(v, "guard", "trace", "Systems/NakedOnly")
    check("guard trace: 裸合約節點不印『無合約』矛盾",
          "★INVARIANT★" in r.stdout and "無 ★INVARIANT★ 合約" not in r.stdout, r.stdout)
    # Finding 3: code block 內方法名不算證據
    write(v, "Systems/Cb.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 某 [test:OnlyInCodeBlock]",
          body="# Cb\n")
    write(v, "Verification/2026-01-03_cb.md", "type: verification\nstatus: pass",
          body="# cb\n```\nOnlyInCodeBlock 只出現在 code block\n```\n")
    r = run(v, "guard", "trace", "Systems/Cb")
    check("guard trace: code block 內方法名不算證據",
          "無 Verification 提到" in r.stdout, r.stdout)


def t_sync_verified_by():
    """sync-verified-by — 補 Check 3 漏寫(dry-run 預設 / --apply 寫 / 冪等)。"""
    v = mkvault()
    write(v, "Systems/Pay.md", "type: system\nstatus: done", body="# Pay\n")
    write(v, "Verification/2026-01-01_payv.md", "type: verification\nstatus: pass",
          body="# payv\n## 相關模組\n- [[Systems/Pay]]\n")
    r = run(v, "sync-verified-by")
    check("sync dry-run: 列出漏寫", "Systems/Pay.md" in r.stdout and "待補" in r.stdout, r.stdout)
    check("sync dry-run: 不寫入", "verified_by" not in read(v / "Systems" / "Pay.md"))
    r = run(v, "sync-verified-by", "--apply")
    check("sync --apply: 寫入 verified_by",
          "2026-01-01_payv" in read(v / "Systems" / "Pay.md"), r.stdout + r.stderr)
    r = run(v, "sync-verified-by")
    check("sync 冪等: 補完後無漏", "無漏寫" in r.stdout, r.stdout)


def t_guard_kotlin():
    """P5 語言可插拔:.lumos/config.json test_profile=kotlin-junit →
    discover 認 @Test fun(.kt)、scaffold 寫 .kt、rglob 偵測巢狀 src/test。"""
    import shutil
    root = Path(tempfile.mkdtemp(prefix="gctl-kt-"))
    vault = root / "docs" / "demo-knowledge"
    for sub in ("Systems", "Verification", "Projects", "MOC"):
        (vault / sub).mkdir(parents=True)
    (vault / "MOC" / "idx.md").write_text("---\ntype: moc\n---\n# idx\n", encoding="utf-8")
    (vault / "Systems" / "Login.md").write_text(
        "---\ntype: system\nstatus: done\nsummary: |-\n"
        "  KEY:★INVARIANT★ 登入鎖定 [test:LoginLocksAfterFiveFails]\n"
        "---\n# Login\n", encoding="utf-8")
    (root / ".lumos").mkdir(parents=True)
    (root / ".lumos" / "config.json").write_text(
        '{"test_profile": "kotlin-junit"}\n', encoding="utf-8")
    ktdir = root / "app" / "src" / "test" / "java" / "auth"
    ktdir.mkdir(parents=True)
    (ktdir / "LoginTest.kt").write_text(
        "package auth\nimport org.junit.Test\nclass LoginTest {\n"
        "  @Test\n  fun LoginLocksAfterFiveFails() { }\n}\n", encoding="utf-8")
    tpl = root / ".lumos" / "guard-templates"
    tpl.mkdir(parents=True)
    (tpl / "pure.tmpl").write_text(
        "// {{NODE}} | {{INVARIANT}} | {{CLAIM}}\nclass {{CLASS}} {\n"
        "  @Test fun {{METHOD}}() { fail(\"unfilled\") }\n}\n", encoding="utf-8")
    try:
        r = run(vault, "guard", "list")
        check("guard kotlin: @Test fun 認成真方法(real)", "真綁 1" in r.stdout, r.stdout)
        outd = root / "out"
        outd.mkdir()
        r = run(vault, "guard", "scaffold", "--node", "Systems/Login", "--invariant", "登入鎖定",
                "--method", "NewKtGuard", "--type", "pure", "--claim", "連五次失敗鎖定", "--out", str(outd))
        check("guard kotlin: scaffold 寫 .kt 副檔名",
              (outd / "NewKtGuardTests.kt").exists(), r.stdout + r.stderr)
        r = run(vault, "guard", "scaffold", "--node", "Systems/Login", "--invariant", "登入鎖定",
                "--method", "AutoDetectKt", "--type", "pure", "--claim", "x")
        check("guard kotlin: rglob 偵測巢狀 src/test",
              (root / "app" / "src" / "test" / "AutoDetectKtTests.kt").exists(), r.stdout + r.stderr)
    finally:
        shutil.rmtree(root, ignore_errors=True)


def t_guard_profile_robustness():
    """P5 審計修正:壞 config 不 crash(F1)、ReDoS regex 拒用不 hang(F2)、null profile(F8)。"""
    import shutil
    root = Path(tempfile.mkdtemp(prefix="gctl-rob-"))
    vault = root / "docs" / "demo-knowledge"
    for sub in ("Systems", "Verification", "Projects", "MOC"):
        (vault / sub).mkdir(parents=True)
    (vault / "MOC" / "idx.md").write_text("---\ntype: moc\n---\n# idx\n", encoding="utf-8")
    (vault / "Systems" / "Z.md").write_text(
        "---\ntype: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 某 [test:RealZ]\n---\n# Z\n",
        encoding="utf-8")
    (root / "Z.Tests").mkdir()
    (root / "Z.Tests" / "Z.cs").write_text(
        "using Xunit;\npublic class Z {\n  [Fact]\n  public void RealZ() { }\n}\n", encoding="utf-8")
    cfgdir = root / ".lumos"
    cfgdir.mkdir()

    def setcfg(s):
        (cfgdir / "config.json").write_text(s, encoding="utf-8")

    try:
        setcfg('{"test": "oops"}')   # F1: test 非 dict
        r = run(vault, "doctor", "--ci")
        check("F1: test 非 dict 不 crash", "Traceback" not in r.stderr, r.stderr)
        setcfg('{"test_profile": "kotlin-junit", "test": {"exts": ".kt"}}')  # F1: exts 字串
        r = run(vault, "guard", "list")
        check("F1: exts 字串不 crash", "Traceback" not in r.stderr, r.stderr)
        setcfg('{"test": {"method_regex": "(a+)+$"}}')   # F2: ReDoS(若 hang 整個測試會卡死)
        r = run(vault, "doctor", "--ci")
        check("F2: ReDoS regex 拒用不 hang", "Traceback" not in r.stderr, r.stderr)
        setcfg('{"test_profile": null}')   # F8: null → csharp 預設,RealZ real
        r = run(vault, "guard", "list")
        check("F8: test_profile null → csharp 預設(真綁 1)", "真綁 1" in r.stdout, r.stdout)
    finally:
        shutil.rmtree(root, ignore_errors=True)


def t_stale_candidate():
    """P2 stale --candidate(須配 --match):聚焦『改 X 該重驗哪幾篇』。
    含護欄(bare candidate / 空 match 報錯)、compose、block scalar 展開、Archive 標記。"""
    v = mkvault()
    write(v, "Verification/2026-01-01_a.md",
          "type: verification\nstatus: pass\nrevalidate_when:\n  - schema 變更\n  - 比率調整",
          body="# a\n")
    write(v, "Verification/2026-01-02_b.md", "type: verification\nstatus: pass", body="# b\n")
    write(v, "Verification/2026-01-03_c.md",
          "type: verification\nstatus: pass\nrevalidate_when:\n  - DispatchLog 改寫", body="# c\n")
    write(v, "Verification/2026-01-04_d.md",   # block scalar:DispatchLog 在第二行
          "type: verification\nstatus: pass\nrevalidate_when: |-\n  第一行條件\n  DispatchLog 第二行",
          body="# d\n")
    write(v, "Verification/Archive/2025-01/arch.md",
          "type: verification\nstatus: pass\nrevalidate_when:\n  - DispatchLog 舊", body="# arch\n")
    # 護欄:bare --candidate 無 --match → rc2
    r = run(v, "stale", "--candidate")
    check("stale: bare --candidate 須配 --match(rc2)", r.returncode == 2, r.stdout + r.stderr)
    # 護欄:空 --match → rc2
    r = run(v, "stale", "--match", "")
    check("stale: 空 --match → rc2", r.returncode == 2, r.stdout + r.stderr)
    # compose 聚焦
    r = run(v, "stale", "--candidate", "--match", "DispatchLog")
    check("compose: 命中 c", "2026-01-03_c" in r.stdout, r.stdout)
    check("compose: block scalar 第二行命中 d(未截斷)", "2026-01-04_d" in r.stdout, r.stdout)
    check("compose: 濾掉不含關鍵字的 a", "2026-01-01_a" not in r.stdout, r.stdout)
    check("compose: 排除 Archive", "Archive" not in r.stdout, r.stdout)
    # --match 路徑(非 candidate):含 Archive 且標 [archived]
    r = run(v, "stale", "--match", "DispatchLog")
    check("stale --match: Archive 命中標 [archived]", "[archived]" in r.stdout, r.stdout)
    check("stale --match: 含活躍 c", "2026-01-03_c" in r.stdout, r.stdout)


def t_archive_live_guard_wordboundary():
    """P3:活守衛護欄詞界比對 — 短/前綴 live 方法名不假性護住無關 Verification。"""
    import shutil
    root = Path(tempfile.mkdtemp(prefix="gctl-arch-"))
    vault = root / "docs" / "demo-knowledge"
    for sub in ("Systems", "Verification", "Projects", "MOC"):
        (vault / sub).mkdir(parents=True)
    (vault / "MOC" / "i.md").write_text("---\ntype: moc\n---\n# i\n", encoding="utf-8")
    (vault / "Systems" / "S.md").write_text(   # live guard 方法名 "Pay"(短)
        "---\ntype: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 付款 [test:Pay]\n---\n# S\n",
        encoding="utf-8")
    (root / "S.Tests").mkdir()
    (root / "S.Tests" / "S.cs").write_text(
        "using Xunit;\npublic class S { [Fact] public void Pay() {} }\n", encoding="utf-8")
    (vault / "Verification" / "2020-01-01_exact.md").write_text(   # 精確提 Pay → 護住
        "---\ntype: verification\nstatus: pass\ncreated: 2020-01-01\n---\n# e\n守衛 Pay 跑綠\n",
        encoding="utf-8")
    (vault / "Verification" / "2020-01-02_substr.md").write_text(  # 只提 Payment → 不該護
        "---\ntype: verification\nstatus: pass\ncreated: 2020-01-02\n---\n# s\n講 Payment 流程,與守衛無關\n",
        encoding="utf-8")
    try:
        r = subprocess.run([sys.executable, GRAPHCTL, "--vault", str(vault), "archive", "--days", "30"],
                           capture_output=True, text=True)
        check("archive 護欄: 精確提 Pay 的篇被護住(backs: Pay)",
              "2020-01-01_exact.md  (backs: Pay)" in r.stdout, r.stdout)
        check("archive 護欄: 只提 Payment(超字串)不被護住",
              "2020-01-02_substr.md  (backs" not in r.stdout, r.stdout)
        # CJK 緊貼方法名(無空格)仍須護住(re.ASCII 詞界;否則 Unicode \b 漏護)
        (vault / "Verification" / "2020-01-03_cjk.md").write_text(
            "---\ntype: verification\nstatus: pass\ncreated: 2020-01-03\n---\n# c\n守衛Pay跑綠無空格\n",
            encoding="utf-8")
        r = subprocess.run([sys.executable, GRAPHCTL, "--vault", str(vault), "archive", "--days", "30"],
                           capture_output=True, text=True)
        check("archive 護欄: CJK 緊貼方法名仍護住(re.ASCII)",
              "2020-01-03_cjk.md  (backs: Pay)" in r.stdout, r.stdout)
    finally:
        shutil.rmtree(root, ignore_errors=True)


def t_doctor_suggest():
    """P4 doctor --suggest:orphan Verification 推薦掛載 Systems(正文連結>plan_refs>feature/檔名)。"""
    v = mkvault()
    write(v, "Systems/Billing.md", "type: system\nstatus: done", body="# Billing\n")
    write(v, "Systems/Auth.md", "type: system\nstatus: done", body="# Auth\n")
    write(v, "Projects/X_計劃.md", "type: project\nstatus: doing", body="計劃連 [[Systems/Auth]]\n")
    write(v, "Verification/2026-01-01_a.md", "type: verification\nstatus: pass",
          body="# a\n驗 [[Systems/Billing]]\n")                              # 正文連向
    write(v, "Verification/2026-01-02_b.md",
          "type: verification\nstatus: pass\nplan_refs:\n  - \"[[X_計劃]]\"", body="# b\n")  # plan_refs
    write(v, "Verification/2026-01-03_c.md",
          "type: verification\nstatus: pass\nfeature: 修 Billing 的問題", body="# c\n")        # feature
    write(v, "Verification/2026-01-04_d.md", "type: verification\nstatus: pass", body="# d\n")  # 無線索
    r = run(v, "doctor", "--suggest")
    check("suggest: 正文連向 → 推薦 Billing",
          "2026-01-01_a" in r.stdout and "推薦 Systems/Billing.md" in r.stdout, r.stdout)
    check("suggest: plan_refs → 經計劃推薦 Auth",
          "經 plan_refs" in r.stdout and "Systems/Auth.md" in r.stdout, r.stdout)
    check("suggest: feature 提到 stem → 推薦", "feature 提到「Billing」" in r.stdout, r.stdout)
    check("suggest: 無線索 → 明示人工判斷", "人工判斷" in r.stdout, r.stdout)
    # 不帶 --suggest:Check 1 維持原本扁平清單(向後相容)
    r = run(v, "doctor")
    check("doctor(無 --suggest)不印推薦", "推薦 Systems" not in r.stdout, r.stdout)
    # Bug-1 前綴重疊抑制:feature 提「點數商城」不該也推薦子字串 Systems「點數」
    v2 = mkvault()
    write(v2, "Systems/點數.md", "type: system\nstatus: done", body="# 點\n")
    write(v2, "Systems/點數商城.md", "type: system\nstatus: done", body="# 商城\n")
    write(v2, "Verification/2026-02-01_e.md",
          "type: verification\nstatus: pass\nfeature: 點數商城兌換流程", body="# e\n")
    r = run(v2, "doctor", "--suggest")
    check("suggest: 前綴抑制 — 推薦點數商城", "推薦 Systems/點數商城.md" in r.stdout, r.stdout)
    check("suggest: 前綴抑制 — 不推薦子字串點數", "提到「點數」" not in r.stdout, r.stdout)
    # Bug-1 ASCII 詞界:feature 無 api 整詞時不推薦 api(避 pos_api 類假命中)
    v3 = mkvault()
    write(v3, "Systems/api.md", "type: system\nstatus: done", body="# api\n")
    write(v3, "Verification/2026-03-01_f.md",
          "type: verification\nstatus: pass\nfeature: pos_api_auth 流程修正", body="# f\n")
    r = run(v3, "doctor", "--suggest")
    check("suggest: ASCII 詞界 — api 不命中 pos_api_auth", "提到「api」" not in r.stdout, r.stdout)


def t_reversibility_lint():
    v = mkvault()
    write(v, "Systems/Mig.md",
          "type: system\nstatus: doing\nsummary: |-\n  KEY:★IRREVERSIBLE★ 跑 schema 遷移", body="# M\n")
    r = run(v, "lint", "Systems/Mig")
    check("lint: ★IRREVERSIBLE★ 缺回退 → rc1", r.returncode == 1 and "缺實質回退" in r.stdout, r.stdout)
    write(v, "Systems/Mig2.md",
          "type: system\nstatus: doing\n"
          "decisions:\n  - content: 用樂觀鎖\n    decided: 2026-06-19\n"
          "summary: |-\n  KEY:★IRREVERSIBLE★ 跑遷移 [rollback:decisions]", body="# M2\n")
    r = run(v, "lint", "Systems/Mig2")
    check("lint: [rollback:] 指到無實質 rollback → rc1", r.returncode == 1, r.stdout)
    write(v, "Systems/Mig3.md",
          "type: system\nstatus: doing\n"
          "decisions:\n  - content: 用樂觀鎖\n    decided: 2026-06-19\n    rollback: 跑 revert_v4.sql\n"
          "summary: |-\n  KEY:★IRREVERSIBLE★ 跑遷移 [rollback:decisions]", body="# M3\n")
    r = run(v, "lint", "Systems/Mig3")
    check("lint: irreversible 有實質回退 → rc0", r.returncode == 0, r.stdout)
    write(v, "Systems/Cp.md",
          "type: system\nstatus: doing\nsummary: |-\n  KEY:★CHECKPOINT★ 部署 lab2", body="# C\n")
    r = run(v, "lint", "Systems/Cp")
    check("lint: ★CHECKPOINT★ 缺回退 → warning rc0", r.returncode == 0 and "建議補回退" in r.stdout, r.stdout)
    write(v, "Issues/Bad.md",
          "type: issue\nstatus: open\nsummary: |-\n  KEY:★IRREVERSIBLE★ 標錯地方", body="# B\n")
    r = run(v, "lint", "Issues/Bad")
    check("lint: 可逆性標記在非 Systems → rc1", r.returncode == 1 and "只能在 Systems" in r.stdout, r.stdout)


def t_reversibility_doctor():
    v = mkvault()
    write(v, "Systems/Mig.md",
          "type: system\nstatus: doing\nsummary: |-\n  KEY:★IRREVERSIBLE★ 跑遷移", body="# M\n")
    r = run(v, "doctor", "--ci")
    check("doctor Check R: irreversible 缺回退 → rc1", r.returncode == 1 and "缺實質回退" in r.stdout, r.stdout)
    v2 = mkvault()
    write(v2, "Systems/Cp.md",
          "type: system\nstatus: doing\nsummary: |-\n  KEY:★CHECKPOINT★ 部署 lab2", body="# C\n")
    r2 = run(v2, "doctor", "--ci")
    check("doctor Check R: 只有 checkpoint 缺回退 → rc0(warn_soft 不計 issues)", r2.returncode == 0, r2.stdout)


def t_governance_log_write():
    import subprocess as sp
    root = Path(tempfile.mkdtemp(prefix="gctl-gov-"))
    vault = root / "docs" / "kg"
    for sub in ("Systems", "MOC"):
        (vault / sub).mkdir(parents=True)
    (vault / "MOC" / "i.md").write_text("---\ntype: moc\n---\n# i\n", encoding="utf-8")
    (vault / "Systems" / "Mig.md").write_text(
        "---\ntype: system\nstatus: doing\nsummary: |-\n  KEY:★IRREVERSIBLE★ 跑遷移\n---\n# M\n", encoding="utf-8")
    sp.run(["git", "init", "-q"], cwd=str(root))
    sp.run(["git", "add", "-A"], cwd=str(root))
    sp.run(["git", "-c", "user.email=t@t", "-c", "user.name=t", "commit", "-qm", "init"], cwd=str(root))
    try:
        run(vault, "doctor", "--ci")
        log = root / "docs" / ".governance-log.jsonl"
        check("gov-log: --ci 寫入 governance-log", log.exists() and "check-r" in log.read_text(encoding="utf-8"), "未寫")
        if log.exists():
            log.unlink()
        run(vault, "doctor")
        check("gov-log: 純 doctor 不寫", not log.exists(), "不該寫")
    finally:
        import shutil
        shutil.rmtree(root, ignore_errors=True)


def t_gov_query():
    root = Path(tempfile.mkdtemp(prefix="gctl-govq-"))
    vault = root / "docs" / "kg"
    (vault / "MOC").mkdir(parents=True)
    (vault / "MOC" / "i.md").write_text("---\ntype: moc\n---\n# i\n", encoding="utf-8")
    docs = root / "docs"
    (docs / ".bypass-log.jsonl").write_text(
        '{"ts":"2026-06-18T10:00:00","commit":"abc","subject":"skip graph"}\n', encoding="utf-8")
    (docs / ".rot-queue.jsonl").write_text(
        '{"ts":"2026-06-18T11:00:00","commit":"abc12","verification":"docs/kg/Verification/Foo.md","reason":"schema 變"}\n', encoding="utf-8")
    (docs / ".governance-log.jsonl").write_text(
        '{"ts":"2026-06-19T09:00:00","commit":"def","gate":"check-r","kind":"blocked","hard":true,"nodes":["OrderSvc"]}\n', encoding="utf-8")
    try:
        r = run(vault, "gov")
        check("gov: 三來源合併", "check-r" in r.stdout and "skip graph" in r.stdout and "schema 變" in r.stdout, r.stdout)
        r = run(vault, "gov", "OrderSvc")
        check("gov <node>: 命中 governance-log 事件", "check-r" in r.stdout, r.stdout)
        r = run(vault, "gov", "Foo")
        check("gov <node>: stem 命中 rot-queue", "schema 變" in r.stdout, r.stdout)
    finally:
        import shutil
        shutil.rmtree(root, ignore_errors=True)


def t_marker_doc_sync():
    import pathlib
    repo = pathlib.Path(__file__).resolve().parent.parent
    skill = repo / "skills" / "lumos-project-notes" / "SKILL.md"
    disc = repo / "scripts" / "templates" / "graph-discipline.md"
    if not skill.exists() or not disc.exists():
        check("drift: skills/template 不在(vendored)→ 跳過", True)
        return
    st, dt = skill.read_text(encoding="utf-8"), disc.read_text(encoding="utf-8")
    for m in ("★CHECKPOINT★", "★IRREVERSIBLE★", "[rollback:"):
        check(f"drift: {m} 在 SKILL.md", m in st, "SKILL 缺")
        check(f"drift: {m} 在 graph-discipline", m in dt, "disc 缺")


def t_canary():
    import json as _j
    import shutil
    root = Path(tempfile.mkdtemp(prefix="gctl-can-"))
    vault = root / "docs" / "kg"
    (vault / "MOC").mkdir(parents=True)
    (vault / "MOC" / "i.md").write_text("---\ntype: moc\n---\n# i\n", encoding="utf-8")
    try:
        r = run(vault, "canary", "record", "missed", "--auditor", "sonnet")
        check("canary: record missed rc0", r.returncode == 0, r.stdout + r.stderr)
        log = root / "docs" / ".canary-log.jsonl"
        rec = _j.loads(log.read_text(encoding="utf-8").strip())
        check("canary: 寫入含 token + missed",
              rec.get("kind") == "missed" and rec.get("token", "").startswith("CANARY-"), str(rec))
        r = run(vault, "gov")
        check("canary: gov 顯示 canary/missed", "canary/missed" in r.stdout, r.stdout)
        # 兩筆不同 token → gov 各一列(不被 dedup 折成一列)
        run(vault, "canary", "record", "caught", "--token", "CANARY-A")
        run(vault, "canary", "record", "caught", "--token", "CANARY-B")
        r = run(vault, "gov")
        check("canary: 不同 token 不被 dedup", r.stdout.count("canary/caught") == 2, r.stdout)
        # 非法 kind → rc2(argparse choices)
        r = run(vault, "canary", "record", "bogus")
        check("canary: 非法 kind → rc2", r.returncode == 2, r.stdout + r.stderr)
    finally:
        shutil.rmtree(root, ignore_errors=True)


def t_canary_loop_fields():
    import json as _j
    import shutil
    root = Path(tempfile.mkdtemp(prefix="gctl-clf-"))
    vault = root / "docs" / "kg"
    (vault / "MOC").mkdir(parents=True)
    (vault / "MOC" / "i.md").write_text("---\ntype: moc\n---\n# i\n", encoding="utf-8")
    try:
        r = run(vault, "canary", "record", "caught", "--loop", "L", "--severity", "major", "--token", "zz")
        check("canary --loop/--severity: rc0", r.returncode == 0, r.stdout + r.stderr)
        rec = _j.loads((root / "docs" / ".canary-log.jsonl").read_text(encoding="utf-8").strip())
        check("canary --loop/--severity: 寫入 loop+severity",
              rec.get("loop") == "L" and rec.get("severity") == "major", str(rec))
        r = run(vault, "gov")
        check("gov: canary detail 開頭含 loop=/sev=", "loop=L" in r.stdout and "sev=major" in r.stdout, r.stdout)
    finally:
        shutil.rmtree(root, ignore_errors=True)


def t_loop_status():
    import json as _j
    import shutil
    root = Path(tempfile.mkdtemp(prefix="gctl-loop-"))
    vault = root / "docs" / "kg"
    (vault / "MOC").mkdir(parents=True)
    (vault / "MOC" / "i.md").write_text("---\ntype: moc\n---\n# i\n", encoding="utf-8")
    log = root / "docs" / ".canary-log.jsonl"
    n = [0]

    def rec(loop, kind, sev=None):
        n[0] += 1
        d = {"ts": "2026-06-19T10:00:00", "kind": kind, "auditor": "sonnet",
             "token": f"t{n[0]}", "note": ""}
        if loop:
            d["loop"] = loop
        if sev:
            d["severity"] = sev
        with open(log, "a", encoding="utf-8") as f:
            f.write(_j.dumps(d) + "\n")

    try:
        r = run(vault, "loop", "status", "L")
        check("loop status: 無記錄 → exit 1", r.returncode == 1, r.stdout + r.stderr)
        rec("L", "caught", "clean"); rec("L", "caught", "minor")
        r = run(vault, "loop", "status", "L")
        check("loop status: 連2輪 caught+clean/minor → CONVERGED exit0",
              r.returncode == 0 and "CONVERGED" in r.stdout, r.stdout)
        rec("L", "caught", "major")
        r = run(vault, "loop", "status", "L")
        check("loop status: 最後一輪 major → 未收斂 exit1", r.returncode == 1, r.stdout)
        rec("L", "caught", "clean"); rec("L", "caught", "clean")
        r = run(vault, "loop", "status", "L")
        check("loop status: tail-K 滑動,髒輪滑出 → CONVERGED", r.returncode == 0, r.stdout)
        rec("M", "caught", "clean"); rec("M", "missed"); rec("M", "caught", "clean")
        r = run(vault, "loop", "status", "M")
        check("loop status: missed 在 tail-2 → 未收斂", r.returncode == 1, r.stdout)
        rec("N", "caught"); rec("N", "caught")
        r = run(vault, "loop", "status", "N")
        check("loop status: 缺 severity → 未收斂", r.returncode == 1, r.stdout)
    finally:
        shutil.rmtree(root, ignore_errors=True)


def t_check_k():
    # Check K: ★COMBO★ 鐵則只綁 1 個 [test:] → 軟提醒補組合(warn_soft,不擋)
    v = mkvault()
    write(v, "Systems/Thin.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 不可超賣 ★COMBO★ [test:OverbookHappy]",
          body="# Thin\n")
    r = run(v, "doctor")
    check("Check K: ★COMBO★ 綁 1 標記 → 提醒補組合", "happy-path" in r.stdout, r.stdout)

    # 綁 2 個 [test:] 標記 → 不提醒
    v2 = mkvault()
    write(v2, "Systems/Two.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 不可超賣 ★COMBO★ [test:Happy] [test:Combo]",
          body="# Two\n")
    check("Check K: ★COMBO★ 綁 2 標記 → 不提醒", "happy-path" not in run(v2, "doctor").stdout)

    # 無 ★COMBO★ → 不提醒
    v3 = mkvault()
    write(v3, "Systems/NoCombo.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 不可超賣 [test:Happy]",
          body="# NoCombo\n")
    check("Check K: 無 ★COMBO★ → 不提醒", "happy-path" not in run(v3, "doctor").stdout)

    # F1: [test:a,b] 單逗號標記算 1 個 → 仍提醒(免繞過)
    v4 = mkvault()
    write(v4, "Systems/Comma.md",
          "type: system\nstatus: done\nsummary: |-\n  KEY:★INVARIANT★ 不可超賣 ★COMBO★ [test:HappyA,HappyB]",
          body="# Comma\n")
    check("Check K F1: [test:a,b] 算 1 標記 → 仍提醒(免逗號繞過)", "happy-path" in run(v4, "doctor").stdout)


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("t_")]
    print(f"lumos 測試({len(tests)} 案例)")
    for t in tests:
        try:
            t()
        except Exception as e:
            global FAIL
            FAIL += 1
            print(f"  ✗ {t.__name__} EXCEPTION: {e}")
    print(f"\n{'─'*40}\n{PASS} passed, {FAIL} failed")
    return 1 if FAIL else 0


if __name__ == "__main__":
    sys.exit(main())
