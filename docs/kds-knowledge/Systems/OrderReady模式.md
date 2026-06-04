---
type: system
status: done
created: 2026-05-13
updated: 2026-06-04
tags:
  - type/system
  - status/done
summary: |-
  FLOW:OrderReadyScreen→LaunchedEffect→startFetchOrderReadyInfo→3秒輪詢POST /controller/OrdersList→更新orderReadyList
  KEY:無body/無query,/controller/路徑非/KDS/,POS端決定分組,OrderReadyInfo(OrderName+OrderNo陣列)
  CAP:電視牆不可捲動,每組顯示數=TextMeasurer動態量測(行數×每行個數,用最寬字串保守不裁切),店少列高→顯示更多,store多→自動收回;take(capacity)
  ORIENT:方向由設定頁prefs.orderReadyOrientation(0橫/1直)鎖定,isPortrait切換fontMul(0.36/0.27)+fontFloor(13/10),直立字小
  SORT:後端Finishtime DESC(最新前面),前端維持序取最新N個;單號粗體(FontWeight.Bold)
  DEP:[[POS-API端點]][[系統模式切換]][[輪詢架構]]
  DECISION:[2026-06-04]每組顯示數改動態量測(取代寫死8/6)(valid)
decisions:
  - content: 取餐牆每組固定顯示上限：水平 8、直立 6（依方向切換），超出靠輪詢補位
    decided: 2026-06-03
    valid: false
    superseded_by: 改為依列高/寬度動態量測（TextMeasurer），店少顯示更多、店多自動收回
    ended: 2026-06-04
  - content: 取餐牆每組顯示數改為動態量測：capacity = 容得下行數 × 每行個數，用該組最寬字串保守估算保證不裁切；店少→列高→顯示更多，店多→自動收回（寫死 8/6 只是「組多」的特例）
    decided: 2026-06-04
    valid: true
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


## 取餐牆容量 / 直立支援 (2026-06-03)

電視牆**不可捲動**，每組固定顯示上限，超出由輪詢補位（被取餐 → 後端不再回傳 → 下次輪詢自動補第 9 個）。

### 依方向自動切換（OrderReadyScreen.kt）
`isPortrait = configuration.orientation == ORIENTATION_PORTRAIT` 決定三個參數：

| 方向 | maxPerGroup | 排法 | fontMul / fontFloor |
|------|-------------|------|---------------------|
| 水平 | **8** | 1 行 8 個 | 0.36f / 13f |
| 直立 | **6** | 3 個 × 2 行 | 0.27f / 10f（字較小才塞得下） |

- `orderNo.take(maxPerGroup)` 套用上限
- `fontSp = (rowHeight.value * fontMul).coerceIn(fontFloor, 32f).sp`
- 直立畫面較窄（寬度約水平一半）→ 一行只擠得下 2~3 個，故倍率調小讓一行 3 個、2 行 = 6

### 實測容量（16 組、含震動器字串「P01-0001 (88)」≈13字）
| 方向 | 一行幾個 | 乾淨容量/組 |
|------|---------|-----------|
| 水平 | 8 | 8 |
| 直立 | 3（縮字後） | 6 |

### 顯示排序
後端 `OrderReadyDAL` 為 `ORDER BY Finishtime DESC`（最新做好排前面），前端維持此序 → `take(N)` 取最新 N 個。新單會推到最前、補位行為靠輪詢。


## 方向改為設定控制 (2026-06-04)

取餐牆方向從原本 `SCREEN_ORIENTATION_SENSOR`（跟著裝置轉）改為**由設定頁決定**:
- `prefs.orderReadyOrientation`：0=橫向(LANDSCAPE) / 1=直向(PORTRAIT)
- 設定頁 `OrientationRadio`（Landscape/Portrait）→ `onOrderReadyOrientationChanged` → 寫 prefs（見 [[設定頁]]）
- `MainActivity` orderReady route `LaunchedEffect` 依 prefs 鎖定方向
- OrderReadyScreen 仍依 `configuration.orientation` 自動切換 maxPerGroup(8/6) 與字級 → 方向一變、容量字級隨之套用
- 已在實機驗證:選 Portrait→重啟/重入呈直立6個、選 Landscape→橫向8個



## 每組顯示數改動態量測 (2026-06-04，取代寫死 8/6)

電視牆不可捲動，原本寫死「水平8/直立6」其實只是「組多→列矮」的特例。改為**每組依列高/寬度動態量測**（[OrderReadyScreen.kt](../../app/src/main/java/com/citrus/citruskds/ui/presentation/OrderReadyScreen.kt) items 內 `capacity`）:

```
capacity = 每行個數 × 容得下行數
每行個數 = ⌊(可用寬 + chipGap) / (字寬 + chip左右padding + chipGap)⌋   // 可用寬 = 螢幕寬×0.8 − padding
行數     = ⌊(列內高 + chipVPad) / (字高 + chipVPad×3)⌋                  // 列內高 = 列高 − padding
```
- 用 `rememberTextMeasurer().measure(該組最寬字串)` 量真實字寬/字高（含 Bold + letterSpacing），**保守 → 保證不裁切**（FlowRow 實際排版只會更省）。
- **店少 → 列高 → 行數多 → 顯示更多**；店多 → 列矮 → 自動收回（16組直立≈6、4組橫向≈12，實機驗證）。
- BOM 2024.02（Foundation 1.6）無 FlowRow maxLines，故用 TextMeasurer 計算而非 maxLines。
- 字級仍依 isPortrait 切 fontMul(0.36/0.27)/fontFloor(13/10)；單號 **FontWeight.Bold**（量測同步帶 Bold）。



## 新單叮咚音效 (2026-06-04)
新單變紅時播一聲「叮咚」（門鈴 ding→dong）:
- 音效檔 `res/raw/dingdong.wav`（自製:E5→C5 兩鐘聲音 + 衰減，python 產生）
- `SoundPool`(USAGE_NOTIFICATION) 載入；`LaunchedEffect(redSet)` 偵測 **redSet 多出新成員**時 play 一次
- 與紅底同條件、同時機；**初次載入不播**（prevRed 初始化成當下 redSet），只在看著牆時有新單變紅才響
- 實機驗證:觸發新待取單→紅底+叮咚 ✅