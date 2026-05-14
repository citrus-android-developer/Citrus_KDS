---
type: issue
status: todo
priority: P2
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/issue
  - status/todo
  - priority/P2
summary: |-
  FLAG:UNCONFIRMED_BEHAVIOR
  KEY:RecallOrder派發status=O→走PREPARED分支→E開頭觸發OrdersNotify→客戶可能收到第二次推播｜需向產品確認是否預期
  DEP:[[RecallPage]][[訂單狀態流轉]][[POS-API端點]]
---
# ISSUE: Recall E 開頭單會重複觸發 OrdersNotify 推播

## 現象

[[RecallPage]] 按下 Recall 按鈕時，事件預設 `status="O"` (PREPARED)。`setOrderStatus` 成功處理走 PREPARED 分支：

```kotlin
// CentralViewModel.kt:676-678
if (status == PREPARED && orderNo.startsWith("E")) {
    setOrdersNotify(orderNo)   // ★ 又會打一次推播
}
```

所以一筆 E 開頭線上單若已 Finish 過、Collect 過、又被 Recall，會**觸發第二次 OrdersNotify 推播**給客戶。

## 影響

- **若預期**：員工把訂單從已取餐拉回（如客戶忘了拿、要重新取餐），重複推播提醒客戶 — 合理的 UX
- **若非預期**：客戶會收到第二次「您的餐已準備好」通知，可能造成困惑

## 需澄清

向產品方確認 Recall 後是否應該推播：

- [ ] 是預期行為 → 在 [[RecallPage]] 註記說明
- [ ] 非預期 → 把 PREPARED 分支拆細：
  - FinishOrder 路徑：打推播
  - RecallOrder 路徑：不打推播

## 修正方向（若非預期）

把 setOrdersNotify 觸發條件改成「**只在從 PROGRESSING/NEW 轉成 PREPARED 時觸發**」，需要 setOrderStatus 加一個 `isRecall: Boolean` 參數或拆事件。

## 狀態

- [ ] 待產品確認

## 關聯

- 觸發點：[[RecallPage]] / [[訂單狀態流轉]]
- 受影響 API：[[POS-API端點]] (`OrdersNotify` 遠端)
