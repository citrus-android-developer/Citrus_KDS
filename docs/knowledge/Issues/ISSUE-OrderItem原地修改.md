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
  FLAG:CODE_SMELL,VIOLATES_IMMUTABILITY,RACE_RISK
  KEY:OrderItem每次重組原地修改Order.detail[i].middleDetail,M/G/S組裝邏輯,行為冪等所以沒爆,違反global coding style
  DEP:[[訂單卡片元件]][[資料模型]]
---
# ISSUE: OrderItem 渲染時原地修改 Order.detail

## 現象

`OrderItem.kt:68-79`：

```kotlin
val orderDetail = order.detail   // ← 沒有 copy，是 reference
for (i in 0 until orderDetail.size) {
    if (orderDetail[i].gType == "M" || orderDetail[i].gType == "G") {
        orderDetail[i].middleDetail = mutableListOf()      // ★ 直接改 var
        for (j in i+1 until orderDetail.size) {
            if (orderDetail[j].gType == "S") {
                orderDetail[i].middleDetail = orderDetail[i].middleDetail?.plus(orderDetail[j])
            } else { break }
        }
    }
}
```

每次 Composable 重組都會跑這段，原地修改 `Order.detail[i].middleDetail`。

## 為什麼能 work

`Order` 和 `Detail` 用 `var`（[[資料模型]]），所以是合法的。
且邏輯本身是**冪等**的：每次都從相同 M/G 找後續 S，組出相同結果。

## 為什麼有問題

1. **違反 immutability 原則**：使用者全域 Coding Style（`~/.claude/rules/common/coding-style.md`）明確禁止 mutation
2. **多執行緒風險**：若 Order 同時被輪詢更新 (ViewModel) 與 Composable 重組讀取 — 競態條件
3. **效能浪費**：每次重組都重算，可在 ViewModel 層算一次存進不同欄位

## 修正方向

選項 1：在 OrderItem 內用 `remember(order)` 計算：

```kotlin
val groupedDetail = remember(order.orderNo) {
    buildList {
        order.detail.forEachIndexed { i, d ->
            if (d.gType == "M" || d.gType == "G") {
                val subs = order.detail.drop(i+1).takeWhile { it.gType == "S" }
                add(d.copy(middleDetail = subs))
            } else if (d.gType != "S") add(d)
        }
    }
}
```

選項 2：在 ViewModel 接收 Order 後就算好（修改 Repository 或 ViewModel 層）。

選項 3：請後端直接回巢狀結構（最徹底）。

## 狀態

- [ ] 待優化（無立即危害，屬程式碼品質）

## 關聯

- 元件：[[訂單卡片元件]]
- 資料：[[資料模型]] (Order/Detail mutability)
