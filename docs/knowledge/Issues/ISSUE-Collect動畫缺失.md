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
  FLAG:UX_INCONSISTENCY,NOT_DATA_BUG
  KEY:Served頁Collect不觸發OK覆蓋層,因為servedList未被設isVisible=false｜Main頁Finish正常
  DEP:[[KDS訂單管理]][[訂單狀態流轉]][[ISSUE-Collect死碼]][[掃描槍Collect功能]]
---
# ISSUE: Collect 動畫缺失（Served Page）

## 現象

Served 頁按 Collected 按鈕後，**沒有 OK 覆蓋層淡入動畫**，卡片靜止 ~1 秒後直接從畫面消失。

對照 Main 頁按 Finish：會看到 `OrderItemWithOK` 淡入動畫，~1 秒後卡片才被輪詢移除。

## 對照表

| 操作 | 來源頁 | status | 客戶端 `isVisible=false` 對哪個 list 設？ | 看得到動畫？ |
|------|--------|--------|------------------------------|------|
| Finish | Main | `O` (PREPARED) | mainList ✅ + recallList | ✅ |
| Collect | Served | `F` (COLLECTED) | mainList + recallList（但 **沒設 servedList**） | ❌ |

## 根因

`CentralViewModel.kt:674-705` 的成功處理：
```kotlin
if (status == PREPARED || status == COLLECTED) {
    setState {
        copy(mainList = ...isVisible=false,
             recallList = ...isVisible=false)
        // ← servedList 沒被處理
    }
}
```

但 `ServedPage.kt:140-149` 的 `AnimatedVisibility` 條件是讀 `dataList[index].isVisible`（資料來源是 `servedFilterList` ← `servedList`）。所以 servedList 的項目永遠保持 `isVisible=true`，AnimatedVisibility 永遠不會觸發 OK 覆蓋層。

## 影響

- 嚴格說不是 data bug — 最終資料仍會在 ~1 秒輪詢後正確消失
- 屬於 **UX 不一致**：員工不會得到「按下成功」的視覺確認
- 若加入 [[掃描槍Collect功能]]，缺乏視覺回饋會更明顯

## 修正方向（待決定）

選項 1：在 PREPARED/COLLECTED 分支同步更新 `servedList.isVisible=false`
選項 2：保持現狀，掃描槍功能改用音效/震動回饋取代視覺動畫

## 狀態

- [ ] 是否修正：與 [[掃描槍Collect功能]] 一起決定
- [ ] 若修正，要連帶處理 [[ISSUE-Collect死碼]]

## 關聯

- 影響系統：[[KDS訂單管理]] / [[訂單狀態流轉]]
- 相關 Issue：[[ISSUE-Collect死碼]]
- 受影響功能：[[掃描槍Collect功能]]
