---
type: system
status: done
created: 2026-05-13
updated: 2026-06-30
tags:
  - type/system
  - status/done
summary: |-
  AUTH:內網信任+KDS_ID識別(請求帶硬編ApiKey header,見ISSUE)｜本地POS(prefs.localIp)+遠端Compass(prefs.serverBaseUrl,設定頁可改)
  KEY:7支本地(含SetWastage損耗)+2支遠端,/controller/OrdersList路徑風格特殊,resultFlowData統一包裝,Server預設=BASE_URL雲端(localIp預設本地8099),遠端SetSellStatus為精簡contract(StoreNo/GKID/GID/Status,Gname/Size後端補)且Status僅Available/NotAvailable
  FLOW:訂單(抓單+改製作狀態setOrderStatus)只走POS｜售罄=App雙寫(POS full body+SoldOut成功→再寫Server精簡body+Status映射NotAvailable,Server失敗還原本地)｜推播走Server｜無POS主動syncServer機制,App當orchestrator
  ERR:後端例外回 ApiStatus=0+Error.Code=999｜2026-06-22起 Error.Message=真實 e.Message(原通用「伺服器忙碌中」),前端 resultFlowData 組「<功能名>: <真錯誤>」顯示(FlowExt.kt:36),ErrorDialog 可捲動+限高320dp不爆版→C2B 客戶截圖即可遠端判因(完整 e.ToString 仍寫後端 log)
  DEP:[[訂單狀態流轉]][[KDS訂單管理]][[OrderReady模式]][[輪詢架構]][[Prefs偏好設定]][[SetSellStatus-Remote串接]][[ISSUE-測試用預設URL待移除]]
verified_by:
  - "[[Verification/2026-06-05_損耗功能端到端]]"
  - "[[Verification/2026-06-08_soldout_remote精簡contract]]"
  - "[[Verification/2026-06-30_加料第二語言數量]]"
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
| 損耗/報廢 | `POS_SET_WASTAGE` | `/KDS/SetWastage` | POST | `SetWastageRequest{GKID,GID,Qty,Status,CreateUser}` | `Int` |
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



## 2026-06-04~05 端點與後端異動

### 新增 /KDS/SetWastage（損耗/報廢，本地）
- Req：GKID/GID/Qty/CreateUser/Status(W=報廢,S=損耗)；其餘後端填。
- DAL：`INSERT INTO Wastage SELECT ... FROM product.dbo.Goods` —— Gname=GName、sizedesc=Desc、GPrice=Price×Qty、CreateDate=GETDATE()、Flag='A'。

### SetSellStatus 改寫（修「伺服器忙碌中」）
- 原寫 `ProductBoutique`/`Store`（本地 POS DB 無此表→例外）。
- **本地 POS** 版改寫 `SoldOutItem`（與 GetInventoryList 讀取端一致）：Available→DELETE、其餘→upsert SOType。
- **後台同步**：前端 setSellStatus 成功後再打 `serverUrl + KDS/SetSellStatus`（雲端後台 global.citrus.tw/CompassKDS_UAT，維持 ProductBoutique 版）。
- ⚠️ serverUrl 預設誤設成本地 192.168.0.162:8099，應指雲端後台 UAT，詳見 [[ISSUE-設置庫存]]。

### 訂單明細擴充（OrdersList，加點+雙語用）
- Detail 新增 `ItemStatus`（per-item，判斷加點）。
- 加料/調味第二語言：原抓 OrdersItem.FlavorDesc2/AddGName2（多空），改 **JOIN 主檔**：調味 `product.dbo.Flavor.Gname2`(對 FlavorID)、加料 `product.dbo.Goods.GName2`(對 AddGID, GKID=10)；第一語言仍用 OrdersItem。
- ⚠️ **2026-06-30 修加料第二語言掉數量**：加料數量(`*N`)只存在第一語言 `AddGName`(如 `多牛奶*1`);第二語言 `AddGName2`=`Goods.GName2` 是純品名(`milk more`)→**英文顯示模式掉加料數量**(卡片+列印都走 `additionDisplay`)。修:後端 `AppendAddQty(AddGName, AddGName2)` 取第一語言結尾 `*數字` 補到 Addition2(單一加料;複合加料 AddGName2 本為 null→前端 fallback 第一語言已含數量)。後端單獨修、前端不用動,本機驗 `milk more*1`/`onion more*6`。`KitchenDisplayDAL.cs` AppendAddQty + 行122。
- ⚠️ **2026-06-24 修 row 乘出 bug**：上面調味 JOIN 原本只比 `F.FlavorID = I.FlavorID`（類別），但套餐附餐常帶調味**類別**(Sauce/SUGAR OPTIONS)卻沒選具體值(`FlavorDesc=''`)→ 對到該類別**所有選項**(Sauce 2/SUGAR 3)，GROUP BY 含 `F.Gname2` → **附餐在 KDS 重複顯示**(廚房多做)。修:JOIN 加比具體值 `AND F.[Desc] = I.[FlavorDesc]`(main+served/recall 兩分支)；沒選調味→不對應不乘、有選→精準翻。後端單獨修(前端忠實渲染、不用動)，本機驗 4號單 6→3筆。`KitchenDisplayDAL.cs:39,55`。

### SetStatus 來源狀態感知
- 加 FromStatus（逗號分隔），`AND ItemStatus IN @FromStatuses` 只搬對應品項（見 [[訂單狀態流轉]]、[[加點處理]]）。

> 以上後端皆**待部署到本地 POS 192.168.0.162**（雲端後台 SetSellStatus 維持 ProductBoutique，勿覆蓋）。



## 2026-06-05 更新
- **新增本地端點 `/KDS/SetWastage`**（損耗/報廢，POST `SetWastageRequest{GKID,GID,Qty,Status,CreateUser}`）：寫 `order.dbo.Wastage`，Gname/sizedesc/GPrice 由 `product.dbo.Goods` 帶（GPrice=Price×Qty）、Flag='A'、CreateDate=GETDATE()。已端到端驗證 → [[Verification/2026-06-05_損耗功能端到端]]。本地端點數 6→**7**。
- **Server URL 預設改 `BASE_URL`**：`DEFAULT_SERVER_URL` 不再是 `192.168.0.162:8099`，留空時遠端售罄/推播打雲端後台。詳見 [[設定頁]]。
- ⚠️ 售罄雙寫陷阱：若 Server URL 被填成本地 POS IP，遠端那一寫也打到本地 POS（同支 SetSellStatus）→ **看似成功但沒同步雲端**。