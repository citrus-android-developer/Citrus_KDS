---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  AUTH:無token,僅靠內網信任+KDS_ID識別｜本地POS(/KDS/*+/controller/OrdersList) | 遠端Compass(BASE_URL UAT)
  KEY:7支本地+2支遠端,/controller/OrdersList路徑風格特殊,resultFlowData統一包裝
  DEP:[[訂單狀態流轉]][[KDS訂單管理]][[OrderReady模式]][[輪詢架構]]
---
# POS-API 端點

KDS App 串接兩個後端服務：本地 POS（透過 `prefs.localIp`）與遠端 Compass（`Constants.BASE_URL`）。

## 端點清單

### 本地 POS（`http://{prefs.localIp}` + path）

| 用途 | Path 常數 | Path | Method | Body | Response |
|------|-----------|------|--------|------|----------|
| 取訂單（依 type） | `POS_GET_ORDER` | `/KDS/OrdersList` | POST | `OrderRequest{kdsId,type}` | `List<Order>` |
| 變更訂單狀態 | `POS_SET_ORDER_STATUS` | `/KDS/SetOrderStatus` | POST | `SetOrderStatusRequest{orderNo,status,kdsId}` | `Int` |
| 取庫存清單 | `POS_GET_STOCK_INFO` | `/KDS/InventoryList` | POST | — | `List<StockInfo>` |
| 設庫存 | `POS_SET_INVENTORY` | `/KDS/SetInventory` | POST | `SetInventoryRequest` | `Int?` |
| 售罄狀態（本地） | `POS_SET_SELL_STATUS` | `/KDS/SetSellStatus` | POST | `SetItemSellStatusRequest` | `Int` |
| **取餐顯示牆訂單** | `POS_GET_ORDER_READY_INFO` | `/controller/OrdersList` | POST | — | `List<OrderReadyInfo>` |

### 遠端 Compass（`Constants.BASE_URL` = `https://global.citrus.tw/CompassKDS_UAT/`）

| 用途 | Path 常數 | Path | Method | Body |
|------|-----------|------|--------|------|
| 售罄狀態（同步雲端） | `SERVER_SET_SELL_STATUS` | `KDS/SetSellStatus` | POST | `SetItemSellStatusRequest` |
| 已備餐推播 | `SERVER_SET_ORDERS_NOTIFY` | `KDS/OrdersNotify` | POST | `OrdersNotifyRequest{storeNo,orderNo}` |

> UAT 與正式之間切 `Constants.BASE_URL` 即可（3/19 commit 訊息有「調整uat環境url」）。

## 為什麼 `/controller/OrdersList` 路徑風格不同

本地 POS 大部分走 `/KDS/...`，唯獨 OrderReady 用 `/controller/OrdersList` — **疑似來自不同的後端模組或 controller**（如 ASP.NET MVC）。建議去 POS 端 repo 求證，路由可能是另一個服務。

## 共用 Wrapper

所有呼叫都經 `resultFlowData()` 封裝成 `Flow<Result<T, RootError>>`：
- `Result.Loading` / `Result.Success(data)` / `Result.Error(error)`
- `RootError`（含 `NetworkError`）

UI 層 `collect` 時用 `when (result)` 做分支。

## 認證

**無**任何 token / API key。完全靠內網信任。請求只送 `KDS_ID` 識別來源裝置。

## 來源檔案

- 介面：`commonData/ApiService.kt`
- 實作：`ui/domain/ApiRepositoryImpl.kt`
- 常數：`util/Constants.kt:21-30`

## 關聯

- 訂單狀態 API 細節：[[訂單狀態流轉]]
- 顯示牆 API：[[OrderReady模式]]
- 取訂單列表：[[KDS訂單管理]] + [[輪詢架構]]
