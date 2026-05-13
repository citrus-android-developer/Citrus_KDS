---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  FLOW:顯示status=F訂單→按Recall→RecallOrder(status=O)→setOrderStatus→重置回Served
  KEY:Recall實際拉回的是已取餐單(F→O),不是Finish後的單;E開頭單會重複觸發OrdersNotify(需確認是否預期)
  DEP:[[訂單狀態流轉]][[POS-API端點]][[KDS訂單管理]][[輪詢架構]]
---
# RecallPage

召回頁。顯示**已取餐**（status=COLLECTED `F`）的訂單，提供「Recall 重新置回 Served」的操作。

> 名為 Recall 但實際上拉回的是已取餐訂單而非已 Finish 的訂單 — 員工誤觸 Collected 時的補救入口。

## 資料來源

- `state.recallFilterList` ← `state.recallList` + `recallSearchState` 搜尋字串組合（[[MVVM架構]] 的 snapshotFlow combine，`CentralViewModel.kt:205-219`）
- `state.recallList` ← `repository.getOrders(type="recall")` 由 [[輪詢架構]] 抓回

## 進入時機

- KDS 模式下選 Tab index `2`（`KdsScreen.kt` 對應 `currentPage="recall"`）
- 切到該 Tab 後 `updateCurrentPage(2)` 設 `currentPage="recall"`，下次輪詢拉 recall 列表

## 畫面結構

`RecallPage.kt`：
- `LazyVerticalGrid` 4 欄
- 每筆訂單：`OrderItem` 卡片 + `RecallFeatureBtn`（單一藍色 Recall 按鈕）
- 卡片 `Box` 內疊一層 `AnimatedVisibility(visible = !isVisible)` 包 `OrderItemWithOK`（成功動畫）

## 操作

按下 Recall 按鈕 → `event(RecallOrder(orderNo))`（`RecallPage.kt:138`）

```
Event.RecallOrder(orderNo, status="O")        ← CentralContract.kt:27
        │
        ▼
setOrderStatus(orderNo, "O")                  ← CentralViewModel.kt:382
        │
        ▼
POST /KDS/SetOrderStatus { OrderNo, Status="O", KDS_ID }
        │
        ▼
status == PREPARED 分支:
  - if orderNo.startsWith("E") → setOrdersNotify(orderNo)  ← E 開頭線上單會再推一次
  - mainList & recallList isVisible=false
        │
        ▼
animateBufferGap = true → 1 秒後輪詢補位
recallList 重抓 → 已 Recall 的訂單不再回傳 → 消失
servedList 在切去 Served Tab 時下次輪詢會看到該單回到 Served
```

> Recall 的成功處理路徑與 Finish 完全一樣（都是 status="O"），所以 OK 覆蓋層動畫**會正常顯示**（不像 Served Collect 那樣有缺失，見 [[ISSUE-Collect動畫缺失]]）。

## 副作用：E 開頭訂單推播

對線上單（orderNo 以 "E" 開頭），Recall 會**重複觸發 OrdersNotify 推播**。這可能是預期行為（讓客戶再收到一次「您的餐準備好了」通知），也可能是 bug — **需求需確認**。

## 關聯

- 上游：[[KDS訂單管理]] / [[輪詢架構]]
- 對應 API：[[POS-API端點]] (`/KDS/SetOrderStatus`)
- 狀態定義：[[訂單狀態流轉]]
