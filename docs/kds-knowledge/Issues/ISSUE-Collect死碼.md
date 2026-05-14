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
  FLAG:CODE_SMELL,NO_RUNTIME_IMPACT
  KEY:setOrderStatus成功處理中COLLECTED走的分支對mainList/recallList設isVisible=false但這些list根本不含該訂單→no-op死碼
  DEP:[[訂單狀態流轉]][[ISSUE-Collect動畫缺失]]
---
# ISSUE: Collect 流程的死碼

## 現象

`setOrderStatus` 成功處理中，COLLECTED 與 PREPARED 共用同一分支：

```kotlin
// CentralViewModel.kt:674-694
if (status == PREPARED || status == COLLECTED) {
    if (status == PREPARED && orderNo.startsWith("E")) {
        setOrdersNotify(orderNo)
    }
    setState {
        copy(mainList = currentState.mainList?.map { ... isVisible=false ... },
             recallList = currentState.recallList?.map { ... isVisible=false ... })
    }
}
```

對 **COLLECTED** 而言：
- 該訂單來自 Served 頁，**根本不在 mainList 或 recallList 裡**
- `map { if (orderNo 匹配) isVisible=false else 不變 }` 等同 no-op
- 即「對 mainList/recallList 設 isVisible=false」這段對 COLLECTED 是死碼

## 影響

- **無功能影響**（map 沒實際 effect）
- 程式碼閱讀容易誤導：以為 COLLECTED 也會處理 mainList
- 同時讓 [[ISSUE-Collect動畫缺失]] 顯得反直覺（看起來有處理，但其實對 Served 沒幫助）

## 推測來源

- 早期版本可能 PREPARED 與 COLLECTED 真的會共用 list
- 後來重構分頁但忘記拆條件

## 修正方向（待決定）

選項 1：把 COLLECTED 與 PREPARED 拆開
```kotlin
when (status) {
    PREPARED -> { ... 處理 mainList & recallList，若 E 開頭打 OrdersNotify ... }
    COLLECTED -> { ... 處理 servedList ... }
}
```
順便修掉 [[ISSUE-Collect動畫缺失]]

選項 2：先不動，避免引入回歸風險

## 狀態

- [ ] 待決策

## 關聯

- 相關 Issue：[[ISSUE-Collect動畫缺失]]
- 影響系統：[[訂單狀態流轉]] / [[KDS訂單管理]]
