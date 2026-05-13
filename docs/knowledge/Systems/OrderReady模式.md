---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  FLOW:OrderReadyScreen→LaunchedEffect→startFetchOrderReadyInfo→3秒輪詢POST /controller/OrdersList→更新orderReadyList
  KEY:無body/無query,/controller/路徑非/KDS/,POS端決定分組,OrderReadyInfo(OrderName+OrderNo陣列)
  DEP:[[POS-API端點]][[系統模式切換]][[輪詢架構]]
---
# OrderReady 模式

取餐叫號顯示牆，給客戶看的全螢幕黑底顯示，分區塊顯示「分組標題 + 訂單號清單」。

## 進入時機

模式切換完成後，由 `KdsScreen` 自動 `navigate("orderReady")`（見 [[系統模式切換]]）。

## 畫面結構

`OrderReadyScreen.kt`：
- 全螢幕黑底 LazyColumn
- 每筆 `OrderReadyInfo` 一個區塊：
  - 左 0.2 寬：`OrderName`（區塊標題，如「製作中」/「已完成」）
  - 右 0.8 寬：`OrderNo` 清單橫向排列

## 資料載入

```
OrderReadyScreen.LaunchedEffect(Unit)
        │
        ▼
event(startFetchOrderReadyInfo)              CentralViewModel.kt:280
        │
        ▼
startFetchOrderReady()                       CentralViewModel.kt:510
  stopFetchOrders()                          ← 停掉 KDS 輪詢
  orderReadyJob = launch { fetchOrderReadyJob().collect() }
        │
        ▼
fetchOrderReadyJob (while + delay 3000ms)    CentralViewModel.kt:786
        │
        ▼
fetchOrderReady()                            CentralViewModel.kt:794
        │
        ▼
repository.getOrderReadyInfo()
        │
        ▼
POST http://{prefs.localIp}/controller/OrdersList
        │
        ▼
state.orderReadyList = result.data
```

## API 細節

**Endpoint**：`POS_GET_ORDER_READY_INFO = "/controller/OrdersList"`（`Constants.kt:30`）
**Method**：POST，**無 body / 無 query**（只帶 `@Url`）
**回傳**：`List<OrderReadyInfo>`

```kotlin
data class OrderReadyInfo(
    @Json(name = "OrderName") var orderName: String,
    @Json(name = "OrderNo")   var orderNo: List<String>
)
```

預期 JSON：
```json
[
  { "OrderName": "製作中", "OrderNo": ["A001","A002"] },
  { "OrderName": "已完成", "OrderNo": ["B010"] }
]
```

## 注意事項

- `/controller/OrdersList` 路徑風格與 KDS 用的 `/KDS/OrdersList` 不同 — **可能是 POS 端不同的 controller / 服務**
- POS 端會根據呼叫端 IP 或店家設定決定回什麼（client 不送任何 filter 參數）
- 切回 KDS 模式時必須 `stopFetchOrderReady()`，避免兩個輪詢同時跑

## 關聯

- 後端 API 整理：[[POS-API端點]]
- 模式切換流程：[[系統模式切換]]
- 輪詢機制：[[輪詢架構]]
