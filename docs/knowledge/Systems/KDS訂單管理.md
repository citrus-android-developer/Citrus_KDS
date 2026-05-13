---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  FLOW:KdsScreen四Tab(Main/Served/Recall/SetStock)→輪詢只抓currentPage→各Tab操作打SetOrderStatus
  KEY:三個list(mainList/servedList/recallList)分離,servedFilterList=servedList+search組合,Tab互斥輪詢
  DEP:[[訂單狀態流轉]][[POS-API端點]][[輪詢架構]][[ISSUE-Collect動畫缺失]][[ISSUE-Collect死碼]]
---
# KDS 訂單管理

廚房製作畫面。`KdsScreen.kt` 用 Tab 容納四個子頁：

| Tab | currentPage | 內容 | 主要操作 |
|-----|-------------|------|----------|
| 0 | `main` | [[MainPage]] 待製作 + 製作中訂單 | Finish (完成→PREPARED)、Progress (改製作中→PROGRESSING) |
| 1 | `served` | [[ServedPage]] 已完成待取餐 | Collected (取餐→COLLECTED)、Reprint |
| 2 | `recall` | [[RecallPage]] 召回（取過餐又被拉回） | Recall (重新置回 PREPARED) |
| 3 | `setStock` | [[SetStockPage]] 庫存 / 售罄管理 | SetSellStatus |

## 三個資料 list

| State 欄位 | 來源 | 渲染處 |
|-----------|------|--------|
| `mainList: List<Order>?` | `getOrders(type="main")` | MainPage |
| `servedList: List<Order>?` | `getOrders(type="served")` | ServedPage（透過 `servedFilterList`） |
| `recallList: List<Order>?` | `getOrders(type="recall")` | RecallPage（透過 `recallFilterList`） |

`servedList` 和 `recallList` 透過 `snapshotFlow + combine` 與搜尋字串組合產生 filter list（`CentralViewModel.kt:187-219`），供 UI 即時搜尋。

## 輪詢只抓當前 Tab

`fetchOrders` 只抓 `currentState.currentPage` 對應的 list（`CentralViewModel.kt:822-851`）。切 Tab 時 `updateCurrentPage` 改 `currentPage`，下次輪詢就拉新 list。詳見 [[輪詢架構]]。

## 訂單卡片

`OrderItem` 渲染單筆訂單。卡片上方有操作按鈕（各 Page 自行傳入）。
被操作後設 `Order.isVisible = false`，會觸發 `OrderItemWithOK` 覆蓋層 `AnimatedVisibility` 淡入（**但 Served 頁 Collect 沒走到這條** — 見 [[ISSUE-Collect動畫缺失]]）。

## 進入時機

冷啟動 + `prefs.mode == 0` → `MainActivity` startDestination 走 `"kds"` 路由（`MainActivity.kt:154`）。

## 輪詢啟動

```
KdsScreen.LaunchedEffect(Unit)
event(startFetchKdsInfo) → startFetchOrders()
  stopFetchOrderReady()                ← 互斥
  orderInfoJob = launch { fetchOrdersJob().collect() }
```

## 關聯

- 訂單狀態：[[訂單狀態流轉]]
- 後端 API：[[POS-API端點]]
- 輪詢機制：[[輪詢架構]]
- 已知問題：[[ISSUE-Collect動畫缺失]] / [[ISSUE-Collect死碼]]
