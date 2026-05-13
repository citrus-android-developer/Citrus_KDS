---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  FLOW:顯示PREPARED訂單→Collect(status=F)/Reprint→分別觸發setOrderStatus/列印
  KEY:兩個按鈕(Reprint+Collect),搜尋過濾(servedSearchState),Collect路徑缺動畫,Reprint受printStatus互斥保護
  DEP:[[訂單狀態流轉]][[POS-API端點]][[輪詢架構]][[ISSUE-Collect動畫缺失]][[ISSUE-Collect死碼]][[掃描槍Collect功能]]
---
# ServedPage

已完成待取餐頁。顯示 status=PREPARED `O` 的訂單，提供 **Collect**（取餐完成）和 **Reprint**（重印）兩個操作。

## 資料來源

- `state.servedFilterList` ← `state.servedList` + `servedSearchState` 搜尋字串組合（`CentralViewModel.kt:187-203`）
- `state.servedList` ← `repository.getOrders(type="served")` 由 [[輪詢架構]] 抓回

## 進入時機

- KDS 模式下選 Tab index `1`（`KdsScreen.kt` 對應 `currentPage="served"`）

## 畫面結構

`ServedPage.kt`：
- `LazyVerticalGrid` 4 欄
- 每筆訂單：`OrderItem` 卡片 + `ServedFeatureBtn`（Reprint 灰白邊框 + Collected 深灰按鈕）
- 卡片 `Box` 內疊 `AnimatedVisibility(visible = !isVisible)` 包 `OrderItemWithOK`（但 Collect 路徑不會觸發 — [[ISSUE-Collect動畫缺失]]）

## 兩個操作

### Collected 按鈕

`ServedPage.kt:136-138`：
```
event(CollectedOrder(orderNo))                ← status 預設 "F"
        │
        ▼
setOrderStatus(orderNo, "F")
        │
        ▼
POST /KDS/SetOrderStatus { OrderNo, Status="F", KDS_ID }
        │
        ▼
status == COLLECTED 分支:
  - 不打 OrdersNotify（內層條件 status==PREPARED && E 開頭，COLLECTED 不符）
  - mainList & recallList isVisible=false ← 對 Served 列表的訂單為 no-op (見 [[ISSUE-Collect死碼]])
  - servedList 未被更新 ← 動畫不會觸發 (見 [[ISSUE-Collect動畫缺失]])
        │
        ▼
animateBufferGap = true → 1 秒後輪詢補位 → servedList 重抓 → 卡片消失
```

### Reprint 按鈕

`ServedPage.kt:135`：
```
event(ReprintOrder(order))
        │
        ▼
handleEvent ReprintOrder（CentralViewModel.kt:452-466）:
  if (printStatus != Idle) return                 ← 互斥保護
  if (prefs.printMode == 0) setState { copy(printOrder = event.order) }
        │
        ▼
某個 LaunchedEffect 觀察 printOrder（由 MainActivity 處理印表機驅動）
```

> 列印實際送出在 `MainActivity` 觀察 `homeViewModel.currentState.printOrder`，呼叫 `runPrintReceiptSequence`（目前 MainActivity 中該段被註解掉，需確認當前列印路徑）。

## 搜尋功能

`TitleRow(isShowSearch = true)` 提供搜尋欄，綁定 `state.servedSearchState`。
輸入時透過 `snapshotFlow + combine` 即時過濾 `servedList`，產生 `servedFilterList`。

## 關聯

- 上游：[[KDS訂單管理]] / [[輪詢架構]]
- 對應 API：[[POS-API端點]] (`/KDS/SetOrderStatus`)
- 狀態定義：[[訂單狀態流轉]]
- 已知問題：[[ISSUE-Collect動畫缺失]] / [[ISSUE-Collect死碼]]
- 將觸發新功能：[[掃描槍Collect功能]]（會繞過按鈕直接派發 Collect）
