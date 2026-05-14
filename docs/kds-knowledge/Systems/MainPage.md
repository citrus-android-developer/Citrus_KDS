---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  FLOW:待製作→(prepareMode關:Finish)/(prepareMode開:Prepare→Preparing→Finish)→PREPARED→(同按鈕變)Collected
  KEY:MainFeatureBtn三態切換,本地orderStatus state混合資料層status,點卡片中央開AllItemDialog(品項多時用),搜尋欄被alpha=0隱藏
  DEP:[[訂單狀態流轉]][[KdsScreen容器]][[Prefs偏好設定]][[訂單卡片元件]][[POS-API端點]]
---
# MainPage

KDS Tab 0 — 待製作 / 製作中訂單頁。是廚房日常使用最頻繁的畫面。

## 資料來源

- `state.mainList: List<Order>?` ← `repository.getOrders(type="main")`，由 [[輪詢架構]] 每 3 秒拉一次
- **不**經過 filter，直接渲染（不像 Served/Recall 有 search filter list）
- 標題列 `TitleRow` 帶搜尋欄但 `isShowSearch=false`（隱藏 alpha=0），等同沒搜尋功能

## 畫面結構

`MainPage.kt`：
- `LazyVerticalGrid` 4 欄
- 每筆 `OrderItem` 卡片 + `MainFeatureBtn`（單一狀態切換按鈕）
- 卡片可點開全項彈窗 `AllItemDialog`（用於品項過多時，內含 Finish 按鈕）
- `AnimatedVisibility(visible = !isVisible)` 包 `OrderItemWithOK` 成功動畫

## 核心：MainFeatureBtn 狀態機

按鈕外觀與行為依當前 `orderStatus` + `prefs.isPrepareEnable`（準備模式）切換：

| 條件 | 文案 | 顏色 | 點擊行為 |
|------|------|------|---------|
| `orderStatus == PREPARED ("O")` | `R.string.prepared` | `ColorPrimary` 橘 | `collected()` → 派發 `CollectedOrder` (status="F") |
| `prepareMode 開` 且 `status != PROGRESSING` | `R.string.prepare` | `ColorBlue` 藍 | 觸發 `progressing()` 並就地 `orderStatus = PROGRESSING` |
| `prepareMode 開` 且 `status == PROGRESSING` | `R.string.preparing`（含 spinner） | `ColorPrimary` 橘 | `finish()` → 派發 `FinishOrder` (status="O") |
| `prepareMode 關`（任何非 PREPARED 狀態） | `R.string.prepare` | `ColorBlue` 藍 | `finish()` → 派發 `FinishOrder` (status="O") |

關鍵程式碼 `MainPage.kt:276-291`：

```kotlin
if (orderStatus == PREPARED) {
    collected()
    return@Button
}
if (orderStatus != PROGRESSING && prefs.isPrepareEnable) {
    orderStatus = PROGRESSING
    progressing()
} else {
    finish()
}
```

> 注意 `orderStatus` 是 `remember(status.uppercase())` 的本地 state — 重組時若資料 `status` 變動會重新計算。`progressing` 點擊時**只就地改本地 state**（顯示為「製作中」），實際資料更新仍透過事件派發到 ViewModel。

## 準備模式（PrepareMode）

由 [[設定頁]] `PrepareRadio` 控制 `prefs.isPrepareEnable`：

- **關閉**：按鈕只有兩態（待製作 → 已完成）
- **開啟**：三態流程（待製作 → 製作中 → 已完成）

製作中狀態額外顯示 "click to finish" 提示文字（`MainPage.kt:336-343`）。

## AllItemDialog（全項彈窗）

點擊卡片中央區（透過 `viewOrder` state，`MainPage.kt:88-90`）→ 開啟彈窗顯示所有 detail + Finish 按鈕。

> 通常品項多到卡片顯示不完時用，按 Finish 等同卡片按鈕的最終 finish 動作（不走 PROGRESSING 中繼態）。

## 關聯

- 上游容器：[[KdsScreen容器]]
- 設定來源：[[設定頁]] / [[Prefs偏好設定]]
- 狀態定義：[[訂單狀態流轉]]
- 卡片元件：[[訂單卡片元件]]
- 對應 API：[[POS-API端點]]
- 同層 Tab：[[ServedPage]] / [[RecallPage]] / [[SetStockPage]]
