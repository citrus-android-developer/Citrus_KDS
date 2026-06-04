---
type: system
status: done
created: 2026-05-13
updated: 2026-06-03
tags:
  - type/system
  - status/done
summary: |-
  AUTH:內網信任+KDS_ID識別(請求帶硬編ApiKey header,見ISSUE)｜本地POS(prefs.localIp)+遠端Compass(prefs.serverBaseUrl,設定頁可改)
  KEY:6支本地+2支遠端,/controller/OrdersList路徑風格特殊,resultFlowData統一包裝,測試階段POS+Server同一台8099
  FLOW:訂單(抓單+改製作狀態setOrderStatus)只走POS｜售罄=App雙寫(POS成功再寫Server,Server失敗還原本地)｜推播走Server｜無POS主動syncServer機制,App當orchestrator
  DEP:[[訂單狀態流轉]][[KDS訂單管理]][[OrderReady模式]][[輪詢架構]][[Prefs偏好設定]][[ISSUE-測試用預設URL待移除]]
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


## 2026-06-03 更新：遠端 Server URL 改為可設定

- 遠端 Compass base url 從寫死的 `Constants.BASE_URL` 改為 **`prefs.serverBaseUrl`**（設定頁 Server URL 欄位可改，空白回退 `DEFAULT_SERVER_URL`）。
- `setSellStatusRemote` / `setOrdersNotify` 改用 `prefs.serverBaseUrl + path`。
- `AppModule` 的 Retrofit `.baseUrl()` 仍用 `Constants.BASE_URL`（純佔位，所有呼叫走 `@Url`，不影響實際請求）。
- **測試階段部署**：POS（localIp）與 Server（serverUrl）指向**同一台後端** `192.168.0.162:8099`（IIS）。`/KDS/OrdersList`、`/KDS/SetOrderStatus`、`/KDS/InventoryList`、`/KDS/SetSellStatus`、`/KDS/OrdersNotify` 全在 8099。測試用 → [[ISSUE-測試用預設URL待移除]]
- ⚠️ POS IP 若只填 `192.168.0.162`（沒 port）會打到 port 80 → 404；要帶 `:8099`。


## 資料流向：POS vs Server 角色（2026-06-03 釐清）

常見誤解：「抓單抓 POS、改狀態改 Server，所以 POS 要跟 Server 同步」。**實際不是這樣**。

| 動作 | 打哪 | 說明 |
|------|------|------|
| 抓訂單 `getOrders` | **POS** | 廚房單來源 |
| 變更訂單狀態 `setOrderStatus`（J/W/O/F，含[[自動接單功能]]派 W） | **POS** | ⚠️ 製作流程狀態只寫 POS，**不碰 Server** |
| 取/設庫存 `getStockInfo`/`setInventory` | **POS** | |
| 取餐牆 `getOrderReadyInfo` | **POS** | |
| 售罄狀態 `setSellStatus` | **POS** | 雙寫第一步（本地） |
| 售罄狀態 `setSellStatusRemote` | **Server** | 雙寫第二步（雲端） |
| 推播 `setOrdersNotifyRemote` | **Server** | E 開頭單 PREPARED 通知客戶（⚠️ 2026-06-03 呼叫點已暫時註解停用） |

### 重點

1. **訂單（抓單 + 改製作狀態）全程只跟 POS 來回**，Server 不參與。自動接單的 `setOrderStatus(W)` 也打 POS。
2. **只有兩件事打 Server（Compass 雲端）**：
   - **售罄狀態雙寫**：先 `setSellStatus`(POS) 成功 → 再 `setSellStatusRemote`(Server)；**Server 失敗會還原本地**並提示（對應「9/16 setSellStatus update Server 失敗還原 Local」，見 [[訂單狀態流轉]] 同模式）。
   - **推播 OrdersNotify**（⚠️ 2026-06-03 起呼叫點已暫時註解，目前不發推播；函式本體保留）。
3. **沒有「POS 主動 sync Server」機制**。同步角色是 **App 本身**（orchestrator）：訂單只寫 POS；只有售罄做 POS+Server 雙寫。雲端若需訂單資料是後端各自處理，不在本 App 範圍。

### 測試階段混淆點

目前測試 POS（localIp）與 Server（serverUrl）**都指同一台 `192.168.0.162:8099`**（同一個 Compass 後端），所以兩個角色看起來像同一處。架構上是**兩個角色**：本地 POS（門市內網，訂單真實來源）vs 雲端 Compass（global.citrus.tw，跨店/客戶面），正式環境為不同主機。見 [[ISSUE-測試用預設URL待移除]]。
