---
type: issue
status: done
created: 2026-06-17
updated: 2026-06-26
priority: P1
tags:
  - type/issue
  - status/done
  - priority/P1
summary: |-
  FLAG:KITCHEN_IMPACT,DISPLAY_GAP
  KEY:OrderItem附餐(GType S/R=isSideDish)只當套餐主項(G/M)的middleDetail渲染｜缺陷1:附餐不顯示數量(OrderItem.kt:187只印"- 名稱",Qty>1會少做)｜缺陷2:孤兒附餐隱形(主項不在同KDS/該單無G主項→filter{!isSideDish}濾掉,OrderItem.kt:130)｜缺陷3:附餐不顯示調味加料(middleDetail渲染OrderItem.kt:186-193只印qty+名稱,主項L133-136才有flavorDisplay/additionDisplay;列印EscPosReceiptBuilder攤平有印→又是card≠print不一致,同缺陷1對稱)
  DECISION:[2026-06-17]缺陷1已修(附餐帶qty,middleItemLine+SideDishLineTest,實機驗),缺陷2(孤兒隱形)經確認為設定問題已排除非bug｜[2026-06-26]新增缺陷3(附餐調味加料未顯示,實機03單附餐辣/多麵少麵卡片不出)→已修(middleItemLine加flavor/addition參數+OrderItem附餐呼叫帶入,SideDishLineTest 6測綠),1.1.17實機驗證通過(附餐調味加料顯示在卡片)→結案
related:
  - "[[Systems/訂單卡片元件]]"
---
# ISSUE：附餐顯示缺陷

`OrderItem` 卡片對附餐（`Detail.isSideDish` = GType `S`(M套餐附餐)/`R`(G套餐附餐)）的顯示問題（缺陷1/3 為 bug，缺陷2 為設定問題已排除）。附餐只會被收進套餐主項（G/M）的 `middleDetail` 巢狀渲染（`OrderItem.kt:81-92` 組裝、`:130` 頂層過濾、`:183-195` 巢狀渲染）。

## 缺陷 1：附餐數量不顯示（KITCHEN_IMPACT，先修）

`OrderItem.kt:187`：
```kotlin
for (element in middleList) {
    Text(text = "- " + element.displayName())   // 只有「- 名稱」，無 qty
}
```
- 主項那行有 `qty x 名稱`（`:175`），但附餐只印 `- 名稱`，不管 Qty。
- **實際資料就有附餐 Qty>1**：W01-00012 奶茶(S) Qty=2、E01-00010 咖啡(M)/蘑菇 Qty=2、W01-00004 奶茶/黑胡椒 Qty=2。
- **影響**：附餐做 2 份的單，KDS 看起來像 1 份 → 廚房少做。
- **跨路徑檢查（2026-06-17 補）**：列印路徑 `EscPosReceiptBuilder.kt:54-65` 是**攤平印** `order.detail`，附餐(非 combo main)走 `"${qty} x 名稱"` → **列印一直都有印數量、不受此缺陷影響**。亦即修正前是「卡片漏 qty / 列印有 qty」**兩路徑不一致**。取餐牆不印品項，不涉及。→ 本缺陷只需修卡片。

## 缺陷 2：孤兒附餐隱形 → 經確認為設定問題，已排除（非顯示 bug）

`OrderItem.kt:130`：`orderDetail.filter { !it.isSideDish }` 頂層把附餐全濾掉，只靠主項 middleDetail 帶出。
- 觀察：W01-00004 在 KDS 01 一個品項都不顯示——該單兩筆都是 GType=R，無 G 主項。
- **2026-06-17 結論（使用者確認）**：這是**設定問題已排除**，非前端顯示邏輯缺陷。亦即正常設定下不會出現「附餐無對應套餐主項」的拆站情形，`filter { !it.isSideDish }` 的前提（附餐必有同站主項）成立 → 不需改前端。

## 缺陷 3：附餐不顯示調味加料（KITCHEN_IMPACT，2026-06-26 發現，待修）

`OrderItem.kt:186-193` 巢狀渲染附餐只印 `middleItemLine(element.qty, element.displayName())`（= `- {qty} x {名稱}`），**沒接 `flavorDisplay`/`additionDisplay`**。而主項那段（`OrderItem.kt:133-136`）有印 `#調味 #加料`。
- **實機觀察（03 單 E012026062600003）**：附餐蘑菇義大利麵(S) 後端回 `Flavor=辣/spicy, Addition=多麵*2,少麵*1`，但卡片只顯示 `- 2 x 蘑菇義大利麵`，調味加料消失。一般品項(N)的調味加料正常顯示。
- **跨路徑**：列印 `EscPosReceiptBuilder.kt` 攤平印 `order.detail`，附餐也走 `#flavor #addition` → **列印有印、卡片沒印**，又是 card≠print 不一致（與缺陷1完全對稱的破口）。取餐牆不印品項，不涉及。
- **修法**：附餐渲染比照主項接上 `flavorDisplay/additionDisplay`，抽 helper + 單元測試（同缺陷1套路）。後端不用動。
- **已修（2026-06-26，code+test，待實機驗）**：`middleItemLine(qty,name,flavor="",addition="")` 加可選調味/加料參數（空則格式不變、不回歸）→ `OrderItem.kt:186-198` 呼叫時帶入 `element.flavorDisplay/additionDisplay(prefs.itemDisplayLan)`。`SideDishLineTest` 補 4 例（空不變 / 只調味 / 只加料 / 兩者），6 測全綠。

## 處置
- [x] **缺陷 1：已修（2026-06-17）**。格式採「一律 `- {qty} x 名稱`」（與主項一致）。抽純函式 `middleItemLine(qty,name)`（`Order.kt`）+ `OrderItem.kt:187` 套用 + import；測試 `SideDishLineTest`（qty=1/2，紅→綠）。實機 SM-X710 1.1.6 截圖確認套餐附餐顯示 `- 1 x 名稱`。
- [x] 缺陷 2：**設定問題，已排除**（2026-06-17 使用者確認），非顯示邏輯缺陷，不需改前端。
- [x] **缺陷 3：已修並實機驗證通過**（2026-06-26，1.1.17）。`middleItemLine` 加 flavor/addition 參數 + `OrderItem.kt` 附餐呼叫帶入 + `SideDishLineTest` 補 4 例（6 測綠）。實機確認套餐附餐的調味/加料顯示在卡片上 ✅ → 結案。

## 相關
- [[Systems/訂單卡片元件]]
