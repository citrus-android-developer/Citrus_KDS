---
type: system
status: done
created: 2026-05-13
updated: 2026-06-04
tags:
  - type/system
  - status/done
summary: |-
  FLOW:TabRow+HorizontalPager 5 Tab(Main/Served/Recall/SetStock/Setting)→onTabChange→updateCurrentPage→影響輪詢
  KEY:初始Tab依prefs決定(連線參數空→直接進Setting),5個LaunchedEffect觀察(errMsg/printStatus/mode/tab/language),Setting同時是Tab4也是獨立route
  DEP:[[MainPage]][[ServedPage]][[RecallPage]][[SetStockPage]][[設定頁]][[系統模式切換]][[輪詢架構]]
---
# KdsScreen 容器

KDS 模式下的 Tab 容器。`KdsScreen.kt` 用 `TabRow + HorizontalPager` 容納 5 個子頁。

## 5 個 Tab（HomeTabs enum，MainActivity.kt:42-76）

| Index | 名稱 | 圖示 | Composable | currentPage |
|-------|------|------|------------|-------------|
| 0 | Main | ic_main_* | [[MainPage]] | "main" |
| 1 | Served | ic_served_* | [[ServedPage]] | "served" |
| 2 | ReCall | ic_recall_* | [[RecallPage]] | "recall" |
| 3 | SetStock | ic_setstock_* | [[SetStockPage]] | "setStock" |
| 4 | Setting | ic_setting_* | [[設定頁]] (內嵌版) | "else" |

> 注意：Setting 既可從 **Tab 4 內嵌** 進入，也可從 OrderReady 的「設定」按鈕 **route navigate** 進入（`MainActivity.kt:189`）。兩種進入方式都顯示 `SettingPage`，但 onVerifyCancel 行為不同（Tab 內嵌會派發 `Event.onVerifyCancel`；route navigate 則為空）。

## 啟動時的預設 Tab

`KdsScreen.kt:79`：

```kotlin
initialPage = if (prefs.localIp.isEmpty() || (prefs.kdsId.isEmpty() && prefs.mode == 0)) 4 else 0
```

連線參數未填 → 直接進 Setting Tab（強迫先配置）。

## 五個 LaunchedEffect 觀察者

`KdsScreen.kt:86-133`：

| 觀察的 state | 動作 |
|--------------|------|
| `Unit`（一次性） | 派發 `startFetchKdsInfo` 啟動 [[輪詢架構]] |
| `modeState` | == 1 時 `navigateToOrderReady()` 切去 [[OrderReady模式]] |
| `selectedTabIndex.value` | 呼叫 `viewModel.updateCurrentPage(index)` 改 `currentPage` |
| `errMsg` | 非 null 時設 `errShowing=true` → 顯示 ErrorDialog（描述見 [[資料層錯誤處理]]） |
| `printStatus` | `PrintStatus.Error` 時顯示列印錯誤 Dialog |
| `languageState.state.text` | 動態更新 `Configuration.setLocale`（即時切換語系） |

## 錯誤 Dialog 條件

`KdsScreen.kt:242-248`：

```kotlin
if (!prefs.firstInstall && prefs.localIp.isNotBlank()
    && viewModel.currentState.currentPage != "else"
    && viewModel.currentState.errMsg != null) {
    ErrorDialog(...)
}
```

「首次安裝完成 + 已設 IP + 不在 Setting 頁 + 有錯誤訊息」才彈出 — 避免設定中 IP 還沒填好時被網路錯誤打擾。

## 底部版本欄

`KdsScreen.kt:207-238`：顯示版本號 + Citrus logo，點擊觸發 `askUpdate()` → 走 [[版本更新系統]] 流程。

## 關聯

- 子頁：[[MainPage]] / [[ServedPage]] / [[RecallPage]] / [[SetStockPage]] / [[設定頁]]
- 模式切換：[[系統模式切換]]
- 輪詢機制：[[輪詢架構]]
- 版本更新入口：[[版本更新系統]]



## 2026-06-04 修正：切 tab 殘影
- **症狀**：主頁↔已完成切換有殘影/閃爍。
- **根因**：`HorizontalPager { page -> ReadyForPage(selectedTabIndex, ...) }` —— 每頁都用**全域 selectedTabIndex** 渲染（非自己的 page），切 tab 動畫過半 currentPage 一變、兩個分頁位置同時抽換內容 → 殘影。
- **修法**：改成 `ReadyForPage(page, ...)`、`ReadyForPage(page:Int)` 用 `when(page)`，每頁畫自己的內容；tab onClick 維持 animateScrollToPage。
- 驗證：build 綠燈、已上機。


## 2026-06-04 語系套用移除
移除原本的 `context.resources.updateConfiguration(...)`（已棄用、不通知 Compose）。語系改由 [[MainActivity入口]] 的 `CompositionLocalProvider` 統一提供 LocalConfiguration/LocalContext。詳見 [[ISSUE-語系切換套用不一致]]。
