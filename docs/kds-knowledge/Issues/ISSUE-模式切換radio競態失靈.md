---
type: issue
status: done
priority: P1
created: 2026-06-04
---
# ISSUE: 模式切換 radio 競態失靈

## 現象
設定頁切換 KDS / OrderReady 的 radio **有時候失靈** —— 點了沒切過去（尤其 OrderReady → KDS：點 KDS 卻彈回 OrderReady）。

## 根因（競態）
- 舊版 ModeRadio onClick：先 `navigateTo("kds")`（一律導到 kds），再 `event(OnModeChanged(it))`。
- 由 `KdsScreen` 的 `LaunchedEffect(modeState){ if (modeState==1) navigateToOrderReady() }` 補路由。
- OrderReady→KDS 時，`OnModeChanged(0)` 事件是 **async** 處理；`navigate("kds")` 後 `KdsScreen` 先以**舊 modeState=1** 組合 → `LaunchedEffect` 立刻 `navigateToOrderReady()` **彈回 OrderReady**。
- 屬時序競態 → 「有時候」才中。

## 解法
依選擇**直接導到對應 route**，不再經 KdsScreen 彈回：
- `SettingPage` `navigateTo: ()->Unit` → `(Int)->Unit`
- ModeRadio：`event(OnModeChanged(it)); navigateTo(it)`
- `MainActivity` setting route：`navigateTo = { mode -> navigate(if(mode==1)"orderReady" else "kds"){ popUpTo("setting"){inclusive=true}; launchSingleTop=true } }`
- `KdsScreen`：**移除** `LaunchedEffect(modeState)` 彈回（modeState 不再用於導頁）

## 相關
- [[系統模式切換]]
- [[設定頁]]
- [[OrderReady模式]]

## 狀態
- [x] 分析原因（競態）
- [x] 實作修正
- [x] 實機驗證雙向切換（OrderReady→KDS、KDS→OrderReady 皆穩定）
